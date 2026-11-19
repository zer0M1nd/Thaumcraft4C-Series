package thaumicomniverse;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import thaumicomniverse.research.CollaborationResearch;
import static org.junit.jupiter.api.Assertions.*;

class CollaborationLogicTest {
    @Test void ownResearchKeepsMetadataAndOnlyUnavailableIntegrationsAreFiltered() throws Exception {
        var input=JsonParser.parseString("""
            [{"key":"THAUMICOMNIVERSE","research":[
              {"key":"ALCHEMICALBRASS","category":"THAUMICOMNIVERSE","innerAspects":["aer"],"negativeAspects":["ignis"]},
              {"key":"OPTIONAL","omniverse_requires":["missing"]}]}]
            """).getAsJsonArray();
        String before=input.toString();
        var output=CollaborationResearch.filter("research_catalog",input,id->false);
        assertEquals(before,input.toString());assertEquals(1,output.size());
        var research=output.get(0).getAsJsonObject().getAsJsonArray("research");assertEquals(1,research.size());
        assertEquals("aer",research.get(0).getAsJsonObject().getAsJsonArray("innerAspects").get(0).getAsString());
        var owned=JsonParser.parseString(java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/data/thaumicomniverse/research_catalog/modern_alchemy.json"))).getAsJsonObject().getAsJsonArray("categories");
        var entries=owned.get(0).getAsJsonObject().getAsJsonArray("research");assertEquals(1,entries.size());
        for(var r:entries) {
            assertEquals("THAUMICOMNIVERSE",r.getAsJsonObject().get("category").getAsString());
            assertTrue(r.getAsJsonObject().getAsJsonObject("icon_item").get("item").getAsString().startsWith("thaumicomniverse:"));
        }
        var absent=CollaborationResearch.filter("research_catalog",owned,id->false).get(0).getAsJsonObject().getAsJsonArray("research").get(0).getAsJsonObject();
        assertEquals("[\"TUBEFILTER\",\"thaumicomniverse:GUIDE_OMNIVERSE\"]",absent.getAsJsonArray("parents").toString());
        var present=CollaborationResearch.filter("research_catalog",owned,id->id.equals("thaumicfutures")).get(0).getAsJsonObject().getAsJsonArray("research").get(0).getAsJsonObject();
        assertEquals("[\"TUBEFILTER\",\"ALCHEMICALBRASS\",\"thaumicomniverse:GUIDE_OMNIVERSE\"]",present.getAsJsonArray("parents").toString());
    }
    @Test void pagesRequireAllTheirTargetsAndIgnoreSourceMod() {
        var rows = JsonParser.parseString("""
            [{"key":"BEES","omniverse_requires":["forestry","mobees"]},{"key":"BASE"}]
            """).getAsJsonArray();
        assertEquals(1, CollaborationResearch.filter("research_pages", rows, id -> id.equals("forestry")).size());
        assertEquals(2, CollaborationResearch.filter("research_pages", rows, id -> !id.equals("magicbees")).size());
    }
}
