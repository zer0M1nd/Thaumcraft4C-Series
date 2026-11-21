package tc4revival.aspect.sync;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AspectInputDeltaTest {
    @Test void bundledAndGeneratedTablesNeverTravel(){
        var bundled=Map.of("item_aspects/thaumcraft:explicit","{\"entries\":[]}");
        var effective=new TreeMap<>(bundled);
        effective.put("crafting_aspect_inputs/thaumcraft:runtime","{\"large\":true}");
        for(String type:List.of("item","block","entity"))effective.put(type+"_aspects/thaumcraft:advanced_inference","{\"large\":true}");
        assertEquals(Map.of(),AspectInputDelta.between(bundled,effective));
    }
    @Test void customOverridesAdditionsAndRemovalsRoundTripAndDoNotSurviveNextReload(){
        var bundled=Map.of("item_aspects/thaumcraft:explicit","{}","block_aspects/thaumcraft:blocks","{}");
        var effective=Map.of("item_aspects/thaumcraft:explicit","{\"custom\":1}","item_aspects/custom:extra","{}");
        var delta=AspectInputDelta.between(bundled,effective);
        assertEquals("",delta.get("block_aspects/thaumcraft:blocks"));
        var frames=AspectDataTransfer.encode(delta);var receiver=new AspectDataTransfer.Receiver();Map<String,String> received=null;
        for(int i=0;i<frames.size();i++)received=receiver.accept(i,frames.size(),frames.get(i)).orElse(received);
        assertEquals(effective,AspectInputDelta.apply(bundled,received));
        assertEquals(bundled,AspectInputDelta.apply(bundled,Map.of()));
    }
    @Test void noVersionReconciliationAndNoGeneratedCorrectionAllowed(){
        var serverBundled=Map.of("item_aspects/thaumcraft:explicit","{}");
        var clientBundled=Map.of("item_aspects/thaumcraft:explicit","{\"differentVersion\":true}");
        assertEquals(clientBundled,AspectInputDelta.apply(clientBundled,AspectInputDelta.between(serverBundled,serverBundled)));
        assertThrows(IllegalArgumentException.class,()->AspectInputDelta.apply(clientBundled,Map.of("item_aspects/thaumcraft:advanced_inference","{}")));
    }
    @Test void settingsTravelInSameEnvelopeWithoutAResultManifest(){
        var payload=Map.of(AspectInputDelta.SETTINGS,"{\"advanced\":true}");
        var frames=AspectDataTransfer.encode(payload);
        assertEquals(payload,AspectDataTransfer.decode(frames.get(0)));
        assertEquals(Map.of(),AspectInputDelta.apply(Map.of(),payload));
    }
}
