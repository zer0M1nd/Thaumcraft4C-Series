package arcane_engineering;

import com.google.gson.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tc4revival.research.*;

class ResourceContractTest {
    private JsonObject data(String path) throws Exception {
        return JsonParser.parseString(Files.readString(Path.of("src/main/resources/"+path))).getAsJsonObject();
    }
    @Test void allPagesAndRecipeDisplaysUseTheRealCoreDecoder() throws Exception {
        var pages=ResearchPages.decode(data("data/arcane_engineering/research_pages/main.json").getAsJsonArray("entries"));
        var recipes=ResearchPageRecipes.decode(data("data/arcane_engineering/research_page_recipes/main.json").getAsJsonArray("entries"));
        recipes.validate(pages);
        assertEquals(11,pages.entries().size());
        assertEquals(14,recipes.entries().size());
    }
    @Test void modernTexturePathsAreValidAndAllOldAssetsExist() throws Exception {
        var source=data("data/arcane_engineering/thaumcraft/original_resources.json");
        try(var jar=new java.util.zip.ZipFile("../../../References/Collaboration/jars/arcane_engineering-0.2.7.jar")) {
            for(var entry:source.getAsJsonObject("mapping").entrySet()) {
                assertTrue(entry.getKey().matches("[a-z0-9_./-]+"),entry.getKey());
                assertNotNull(jar.getEntry(entry.getValue().getAsString()),entry.getValue().getAsString());
            }
        }
    }
    @Test void renderedDrillTexturesAreOnTheBlockAtlas() throws Exception {
        var sprites=data("assets/minecraft/atlases/blocks.json").getAsJsonArray("sources");
        assertEquals(2,sprites.size());
        assertEquals("arcane_engineering:item/drill_void",sprites.get(1).getAsJsonObject().get("resource").getAsString());
    }
}
