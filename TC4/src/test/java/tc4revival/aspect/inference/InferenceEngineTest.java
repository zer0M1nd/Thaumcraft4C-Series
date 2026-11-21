package tc4revival.aspect.inference;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tc4revival.aspect.inference.InferenceEngine.*;

class InferenceEngineTest {
    private Map<String,Map<String,Integer>> linear(Map<String,Map<String,Integer>> seeds,List<Recipe> recipes,int rounds,double decay) {
        return InferenceEngine.infer(seeds,recipes,rounds,decay,new tc4revival.aspect.recipe.InferenceLimits(64,6),v->v);
    }
    private Recipe recipe(String output,int count,String... inputs){return new Recipe(output,new Amount(output,count),Arrays.stream(inputs).map(k->List.of(new Amount(k,1))).toList());}
    @Test void chainsRetryBeforeFallbackAndPreserveSmallAmounts(){
        var result=linear(Map.of("A",Map.of("terra",3)),List.of(recipe("C",1,"A","B"),recipe("B",1,"A")),6,.9);
        assertEquals(Map.of("terra",3),result.get("B"));assertEquals(Map.of("terra",6),result.get("C"));
    }
    @Test void incompleteInputsAndUnseededCyclesNeverInventAspects(){
        var result=linear(Map.of("A",Map.of("terra",3)),List.of(recipe("C",1,"A","unknown"),recipe("X",1,"Y"),recipe("Y",1,"X")),6,.9);
        assertEquals(Set.of("A"),result.keySet());
    }
    @Test void countsAndAlternativeCostsAreRespected(){
        var recipe=new Recipe("test",new Amount("out",2),List.of(List.of(new Amount("costly",1),new Amount("cheap",3))));
        var result=linear(Map.of("cheap",Map.of("terra",2),"costly",Map.of("terra",20)),List.of(recipe),6,.9);
        assertEquals(Map.of("terra",3),result.get("out"));
    }
    @Test void explicitAndCacheSeedsWinOverRecipes(){
        var result=linear(Map.of("A",Map.of("terra",9),"B",Map.of("aer",7)),List.of(recipe("B",1,"A")),6,.9);
        assertEquals(Map.of("aer",7),result.get("B"));
    }
    @Test void boundedRoundsDoNotDependOnRecipeOrder(){
        var recipes=new ArrayList<Recipe>();for(int i=1;i<9;i++)recipes.add(recipe("A"+i,1,"A"+(i-1)));
        var result=linear(Map.of("A0",Map.of("terra",1)),recipes,6,.9);Collections.reverse(recipes);
        assertEquals(result,linear(Map.of("A0",Map.of("terra",1)),recipes,6,.9));assertTrue(result.containsKey("A6"));assertFalse(result.containsKey("A7"));
    }
    @Test void fluidQuantitiesUseBucketFractionsAndRejectInvalidScales(){
        var recipe=new Recipe("fluid",new Amount("ingot",1),List.of(List.of(new Amount("fluid",.25))));
        assertEquals(2,linear(Map.of("fluid",Map.of("metallum",8)),List.of(recipe),6,.9).get("ingot").get("metallum"));
        assertThrows(IllegalArgumentException.class,()->new Amount("x",Double.NaN));
    }

    @Test void laterRoundAlternativesUseNewCostsAndKeepFirstEqualCostChoice(){
        var target=new Recipe("target",new Amount("out",1),List.of(
                List.of(new Amount("expensive",1),new Amount("cheap",1),new Amount("tie",1)),
                List.of(new Amount("gate",1))));
        var seeds=Map.of("source",Map.of("aqua",4),"expensive",Map.of("terra",20),"tie",Map.of("ignis",3));
        var recipes=List.of(target,recipe("cheap",1,"source"),recipe("gate",1,"source"));
        var first=linear(seeds,recipes,1,.75);
        assertFalse(first.containsKey("out"));
        var second=linear(seeds,recipes,2,.75);
        assertEquals(Map.of("aqua",5),second.get("out"));
        assertEquals(List.of("cheap","expensive","gate","out","source","tie"),new ArrayList<>(second.keySet()));
        assertThrows(UnsupportedOperationException.class,()->second.put("extra",Map.of()));
    }

    @Test void anEmptyLimitedEntryIsNotAnAvailableIngredient(){
        var result=InferenceEngine.infer(Map.of("source",Map.of("terra",1)),
                List.of(recipe("empty",1,"source"),recipe("out",1,"empty")),3,.9,
                new tc4revival.aspect.recipe.InferenceLimits(64,6),v->Map.of());
        assertTrue(result.containsKey("empty"));
        assertTrue(result.get("empty").isEmpty());
        assertFalse(result.containsKey("out"));
    }
}
