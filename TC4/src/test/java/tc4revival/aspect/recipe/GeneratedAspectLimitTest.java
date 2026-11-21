package tc4revival.aspect.recipe;

import com.google.gson.JsonParser;
import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import tc4revival.aspect.item.*;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedAspectLimitTest {
    private final AspectCatalog catalog=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private Map<String,Integer> seven(){var a=new LinkedHashMap<String,Integer>();for(var tag:List.of("aer","terra","ignis","aqua","ordo","perditio","praecantatio"))a.put(tag,8);return a;}
    @Test void onlyDerivedListsAreCulledIncludingCachedAndChainedResults(){
        var input=new RecipeAspectData.Key("test:explicit",0);var output=new RecipeAspectData.Key("test:crafted",0);
        var engine=new RecipeAspectEngine(new RecipeAspectData(Map.of(),List.of(new RecipeAspectData.Registration(input,seven())),List.of(
            new RecipeAspectData.Recipe(RecipeAspectData.Kind.CRAFTING,output,1,List.of(RecipeAspectData.Ingredient.direct(input)),Map.of()))),catalog,Map.of(),(k,v)->v,new InferenceLimits(64,6));
        assertEquals(7,engine.object(input).size());assertEquals(6,engine.object(output).size());assertEquals(6,engine.object(output).size());
        assertEquals(7,engine.object(input).size());assertEquals(6,engine.fork().object(output).size());
    }
    @Test void legacyWeightsPreferCompoundOverEquallySizedPrimalAndDoNotMutateInput(){
        var source=new AspectAmounts();seven().forEach((a,n)->source.add(catalog.require(a),n));
        var result=GeneratedAspectLimit.apply(source,catalog,new InferenceLimits(64,6));
        assertEquals(6,result.size());assertEquals(8,result.amount(catalog.require("praecantatio")));assertEquals(7,source.size());
        var large=new AspectAmounts();seven().forEach((a,n)->large.add(catalog.require(a),1000000));assertEquals(6,GeneratedAspectLimit.apply(large,catalog,new InferenceLimits(64,6)).size());
    }
    @Test void jsonGeneratedProvenanceSurvivesEncodingAndAllowsExplicitExceptions(){
        var quantities=seven().entrySet().stream().map(e->new ItemAspectRule.Quantity(e.getKey(),e.getValue())).toList();
        var generated=new ItemAspectRule(List.of("test:derived"),null,quantities,true);
        var direct=new ItemAspectRule(List.of("test:explicit"),null,quantities);
        var decoded=ItemAspectJson.decode(ItemAspectJson.encode(List.of(generated,direct)));
        assertEquals(6,decoded.get(0).amounts(catalog,new InferenceLimits(64,6)).size());assertEquals(7,decoded.get(1).amounts(catalog,new InferenceLimits(64,6)).size());
        var root=ItemAspectJson.encode(List.of(direct));root.addProperty("generated",true);
        assertEquals(6,ItemAspectJson.decode(root).get(0).amounts(catalog,new InferenceLimits(64,6)).size());
        root.getAsJsonArray("entries").get(0).getAsJsonObject().addProperty("generated",false);
        assertEquals(7,ItemAspectJson.decode(root).get(0).amounts(catalog,new InferenceLimits(64,6)).size());
    }
}
