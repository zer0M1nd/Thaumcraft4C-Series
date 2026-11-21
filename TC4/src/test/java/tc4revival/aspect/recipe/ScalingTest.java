package tc4revival.aspect.recipe;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tc4revival.aspect.*;
import tc4revival.aspect.inference.InferenceEngine;
import tc4revival.aspect.sync.InferenceSettings;
import com.google.gson.JsonObject;

class ScalingTest {
    final InferenceLimits limits=InferenceLimits.DEFAULT;
    final AspectCatalog catalog=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    @Test void thresholdsRoundingMonotonicityAndCaps(){
        assertTrue(limits.scaling());assertEquals(10,limits.types());
        assertEquals(32,limits.compress(32));assertEquals(32,limits.expand(32));
        assertEquals(256,limits.compress(1048576));assertEquals(1048576,limits.expand(256));
        assertEquals(256,limits.compress(Double.MAX_VALUE));assertEquals(1048576,limits.expand(10000));
        assertEquals(47,limits.compress(64));assertEquals(34,limits.expand(33));
        int last=0;for(int x=0;x<=1048576;x+=31){int value=limits.compress(x);assertTrue(value>=last&&value<=256);last=value;}
        for(int x=0;x<=256;x++)assertEquals(x,limits.compress(limits.expand(x)));
        assertEquals(9,limits.compress(8.5));
    }
    @Test void advancedInferenceExpandsEachInputAndDividesBeforeCompression(){
        var recipe=new InferenceEngine.Recipe("r",new InferenceEngine.Amount("out",3),List.of(
            List.of(new InferenceEngine.Amount("a",2)),List.of(new InferenceEngine.Amount("b",4))));
        var result=InferenceEngine.infer(Map.of("a",Map.of("terra",80),"b",Map.of("terra",60)),List.of(recipe),2,.9,limits,v->limits.apply(v,catalog));
        assertEquals(limits.compress((limits.expand(80)*2+limits.expand(60)*4)*.9/3),result.get("out").get("terra"));
        assertEquals(80,result.get("a").get("terra"));
    }
    @Test void ordinaryRecipesUseSameScaleAndIgnoreAmountLimit(){
        var a=new RecipeAspectData.Key("test:a",0);var out=new RecipeAspectData.Key("test:out",0);
        var data=new RecipeAspectData(Map.of(),List.of(new RecipeAspectData.Registration(a,Map.of("terra",100))),List.of(
            new RecipeAspectData.Recipe(RecipeAspectData.Kind.CRAFTING,out,2,List.of(RecipeAspectData.Ingredient.direct(a),RecipeAspectData.Ingredient.direct(a)),Map.of())));
        var tiny=new InferenceLimits(1,10,true,32,256,1048576);
        var engine=new RecipeAspectEngine(data,catalog,Map.of(),(k,v)->v,tiny);
        assertEquals(limits.compress(limits.expand(100)*2*.75/2),engine.fork().object(out).amount(catalog.require("terra")));
        assertTrue(engine.object(out).amount(catalog.require("terra"))>1);
    }
    @Test void fractionalOutputRoundsToZeroInsteadOfInventingOne(){
        var recipe=new InferenceEngine.Recipe("tiny",new InferenceEngine.Amount("out",100),List.of(List.of(new InferenceEngine.Amount("a",1))));
        var result=InferenceEngine.infer(Map.of("a",Map.of("terra",1)),List.of(recipe),2,.9,limits,v->limits.apply(v,catalog));
        assertFalse(result.containsKey("out"));
    }
    @Test void customScaleRoundTripsInClientInputs(){
        var setting=new InferenceSettings(false,new JsonObject(),tc4revival.aspect.inference.InferenceFiles.defaultKeywords(),new InferenceLimits(3,12,true,16,512,2000000));
        assertEquals(setting,InferenceSettings.decode(setting.encode().toString()));
        assertThrows(IllegalArgumentException.class,()->new InferenceLimits(64,10,true,32,16,1048576));
    }
}
