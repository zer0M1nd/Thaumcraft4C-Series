package thaumcraft.api.research;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchDataContributorsTest {
    @Test void siblingContributorsComposeBeforeAdditionsWithoutChangingRawFiles(){
        ResearchDataContributors.register("test:futures",0,ResearchDataContributors.Phase.BEFORE_ADDITIONS,json->{
            if(json.has("testBundle"))json.getAsJsonArray("pages").add("future");return json;
        });
        ResearchDataContributors.register("test:collaboration",1,ResearchDataContributors.Phase.BEFORE_ADDITIONS,json->{
            if(json.has("testBundle"))json.getAsJsonArray("pages").add("collaboration");return json;
        });
        var original=JsonParser.parseString("{\"testBundle\":true,\"pages\":[]}").getAsJsonObject();
        var result=ResearchDataContributors.apply(ResearchDataContributors.Phase.BEFORE_ADDITIONS,original);
        assertEquals("[\"collaboration\",\"future\"]",result.get("pages").toString());
        assertEquals(0,original.getAsJsonArray("pages").size());
        assertEquals(original,ResearchDataContributors.apply(ResearchDataContributors.Phase.AFTER_ADDITIONS,original));
    }
}
