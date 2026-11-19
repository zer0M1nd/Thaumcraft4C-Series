package tc4revival.aspect.inference;

import com.google.gson.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InferenceIdentityCacheTest {
    private JsonObject fixture(){return JsonParser.parseString("""
        {"recipes":[
          {"id":"one","output":{"key":"item:test:result","count":2},"inputs":[[{"key":"item:test:ore","count":1},{"key":"item:missing:ore","count":1}],[{"key":"item:test:ore","count":2}]]},
          {"id":"two","output":{"key":"item:test:result","count":1},"inputs":[[{"key":"item:test:ore","count":1},{"key":"item:missing:ore","count":1}]]},
          {"id":"bad","output":{"key":"item:test:result","count":1},"inputs":[[{"key":"item:missing:ore","count":1}]]}
        ]}
        """).getAsJsonObject();}
    @Test void checksPositiveAndNegativeIdentitiesOnceAndPreservesRecipeOrderCountsAndSlots(){
        for(var input:List.of(fixture(),InferenceRecipeInputs.compact(fixture()))){
            var calls=new HashMap<String,Integer>();var recipes=new ArrayList<InferenceEngine.Recipe>();var candidates=new TreeMap<String,String>();
            AdvancedAspectInference.readJei(input,recipes,candidates,key->{calls.merge(key,1,Integer::sum);return !key.startsWith("item:missing:");});
            assertEquals(Map.of("item:test:result",1,"item:test:ore",1,"item:missing:ore",1),calls);
            assertEquals(List.of("jei:one","jei:two"),recipes.stream().map(InferenceEngine.Recipe::id).toList());
            assertEquals(2,recipes.get(0).output().count());assertEquals(2,recipes.get(0).inputs().size());
            assertEquals(2,recipes.get(0).inputs().get(1).get(0).count());
            assertEquals(Set.of("item:test:result","item:test:ore"),candidates.keySet());
        }
    }
    @Test void doesNotRetainNegativeLookupsAcrossReloadSnapshots(){
        var first=new ArrayList<InferenceEngine.Recipe>();AdvancedAspectInference.readJei(fixture(),first,new HashMap<>(),key->false);assertTrue(first.isEmpty());
        var next=new ArrayList<InferenceEngine.Recipe>();AdvancedAspectInference.readJei(fixture(),next,new HashMap<>(),key->true);assertEquals(3,next.size());
    }
}
