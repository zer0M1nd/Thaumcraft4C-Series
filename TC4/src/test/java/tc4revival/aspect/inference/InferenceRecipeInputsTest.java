package tc4revival.aspect.inference;
import com.google.gson.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tc4revival.aspect.sync.*;
import tc4revival.aspect.recipe.InferenceLimits;
class InferenceRecipeInputsTest {
    private JsonObject fixture(){return JsonParser.parseString("{\"recipes\":[{\"id\":\"test:r\",\"output\":{\"key\":\"item:test:out#{}\",\"count\":2},\"inputs\":[[{\"key\":\"fluid:test:f\",\"count\":0.25}],[{\"key\":\"fluid:test:f\",\"count\":0.25}]]}]}").getAsJsonObject();}
    @Test void dictionaryIsLosslessIncludingNbtCountsRepeatedSlotsAndOrdering(){
        var raw=fixture();var compact=InferenceRecipeInputs.compact(raw);
        assertEquals(InferenceRecipeInputs.recipes(raw),InferenceRecipeInputs.recipes(compact));
        assertEquals(2,compact.getAsJsonArray("amounts").size());assertEquals(1,compact.getAsJsonArray("slots").size());
        raw.getAsJsonArray("recipes").add(new JsonObject());assertEquals(1,InferenceRecipeInputs.recipes(InferenceRecipeInputs.compact(raw)).size());
    }
    @Test void authoritativeRecipeInputsSurviveFramingAndDoNotBecomeAspectTables(){
        var raw=fixture();var settings=new InferenceSettings(true,new JsonObject(),InferenceFiles.defaultKeywords(),InferenceLimits.DEFAULT,InferenceRecipeInputs.compact(raw));
        var payload=Map.of(AspectInputDelta.SETTINGS,settings.encode().toString());
        var receiver=new AspectDataTransfer.Receiver();Map<String,String> result=null;var frames=AspectDataTransfer.encode(payload);
        for(int i=0;i<frames.size();i++)result=receiver.accept(i,frames.size(),frames.get(i)).orElse(result);
        var received=InferenceSettings.decode(result.get(AspectInputDelta.SETTINGS));
        assertEquals(InferenceRecipeInputs.recipes(raw),InferenceRecipeInputs.recipes(received.recipes()));
        assertTrue(AspectInputDelta.apply(Map.of(),result).isEmpty());
        raw.getAsJsonArray("recipes").remove(0);
        assertEquals(1,InferenceRecipeInputs.recipes(settings.recipes()).size(),"Later disk/export data must not mutate the active snapshot");
    }
}
