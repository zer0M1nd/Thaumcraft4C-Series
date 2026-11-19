package tc4revival.research;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
class SilverResearchResourcesTest {
    @Test void silverPagesLoadWithTheShippedCatalogAndResolveModernRecipes() throws Exception {
        var root=new JsonObject();
        for(String[] group:new String[][]{{"categories","research_catalog","categories"},{"pages","research_pages","entries"},{"recipes","research_page_recipes","entries"}}){
            var rows=new JsonArray();
            for(String name:new String[]{"legacy","silver"}){
                var stream=getClass().getResourceAsStream("/data/thaumcraft/"+group[1]+"/"+name+".json");
                if(stream==null){assertEquals("research_page_recipes",group[1]);continue;}
                try(var reader=new java.io.InputStreamReader(stream,StandardCharsets.UTF_8)){rows.addAll(JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray(group[2]));}
            }root.add(group[0],rows);
        }
        var snapshot=ResearchDefinitions.decode(root,tc4revival.aspect.AspectDefinitions.bundled());
        assertEquals(java.util.List.of("CAP_gold","INFUSION"),snapshot.catalog().require("CAP_silver").parents());
        var pages=snapshot.pages().entries().get("CAP_silver");assertEquals(3,pages.size());
        assertEquals("modern",((ResearchPage.Reference)pages.get(1).recipe()).legacyClass());
    }
}
