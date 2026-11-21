package tc4revival.aspect.recipe;

import java.util.*;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tc4revival.aspect.*;
import tc4revival.aspect.item.*;
import tc4revival.aspect.inference.*;
import tc4revival.aspect.sync.InferenceSettings;

class InferenceLimitsTest {
    private final AspectCatalog catalog=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private Map<String,Integer> many(){return Map.of("aer",900,"terra",800,"aqua",700,"ignis",600,"ordo",500,"perditio",400,"praecantatio",300);}
    @Test void limitsPersistAcrossRecipeRoundsWithoutChangingAuthoredSeeds(){
        var limits=new InferenceLimits(12,3);
        var recipes=List.of(new InferenceEngine.Recipe("first",new InferenceEngine.Amount("B",1),List.of(List.of(new InferenceEngine.Amount("A",9)))),
            new InferenceEngine.Recipe("second",new InferenceEngine.Amount("C",1),List.of(List.of(new InferenceEngine.Amount("B",9)))));
        var result=InferenceEngine.infer(Map.of("A",many()),recipes,6,.9,v->limits.apply(v,catalog));
        assertEquals(many(),result.get("A"));
        for(var key:List.of("B","C")){assertEquals(3,result.get(key).size());assertTrue(result.get(key).values().stream().allMatch(n->n<=12));}
        var defaults=InferenceEngine.infer(Map.of("A",many()),recipes,6,.9);
        assertEquals(7,defaults.get("C").size());assertTrue(defaults.get("C").values().stream().allMatch(n->n<=256));
    }
    @Test void resolverUsesItsOwnSettingsAndPreservesExplicitValues(){
        var quantities=many().entrySet().stream().map(e->new ItemAspectRule.Quantity(e.getKey(),e.getValue())).toList();
        var rules=List.of(new ItemAspectRule(List.of("test:derived"),null,quantities,true),new ItemAspectRule(List.of("test:explicit"),null,quantities));
        var small=new ItemAspectResolver(rules,catalog,false,new InferenceLimits(8,2));
        var large=new ItemAspectResolver(rules,catalog,false,new InferenceLimits(128,7));
        assertEquals(2,small.resolve("test:derived",t->false).orElseThrow().size());
        assertEquals(7,large.resolve("test:derived",t->false).orElseThrow().size());
        assertEquals(900,small.resolve("test:explicit",t->false).orElseThrow().amount(catalog.require("aer")));
        assertEquals(7,small.resolve("test:explicit",t->false).orElseThrow().size());
    }
    @Test void nonDefaultSettingsRoundTripEvenWhenAdvancedInferenceIsDisabled(){
        var settings=new InferenceSettings(false,new JsonObject(),InferenceFiles.defaultKeywords(),new InferenceLimits(128,9));
        assertEquals(settings,InferenceSettings.decode(settings.encode().toString()));
        assertEquals(InferenceLimits.DEFAULT,InferenceSettings.decode("{\"advanced\":false}").limits());
        assertThrows(IllegalArgumentException.class,()->new InferenceLimits(0,6));
    }
    @Test void mergingGeneratedRulesCannotExceedTypeLimit(){
        var rules=List.of(new ItemAspectRule(List.of("test:merged"),null,List.of(new ItemAspectRule.Quantity("aer",100)),true),
                new ItemAspectRule(List.of("test:merged"),null,List.of(new ItemAspectRule.Quantity("terra",100)),true));
        var resolver=new ItemAspectResolver(rules,catalog,false,new InferenceLimits(16,1));
        var result=resolver.resolve("test:merged",t->false).orElseThrow();
        assertEquals(1,result.size());assertEquals(16,result.total());
    }
    @Test void ordinaryRecipeForksRetainCustomLimits(){
        var input=new RecipeAspectData.Key("test:input",0);var output=new RecipeAspectData.Key("test:output",0);
        var data=new RecipeAspectData(Map.of(),List.of(new RecipeAspectData.Registration(input,many())),List.of(
            new RecipeAspectData.Recipe(RecipeAspectData.Kind.CRAFTING,output,1,List.of(RecipeAspectData.Ingredient.direct(input)),Map.of())));
        var engine=new RecipeAspectEngine(data,catalog,Map.of(),(key,value)->value,new InferenceLimits(120,2));
        var value=engine.fork().object(output);assertEquals(2,value.size());assertTrue(value.view().values().stream().allMatch(n->n<=120));
    }
}
