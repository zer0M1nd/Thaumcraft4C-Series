package tbmodern;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BraceletDataTest {
    private final Path root=Path.of("src/main/resources");
    @Test void everyBraceletHasRecipeResearchAndExternalModel() throws Exception {
        var rows=JsonParser.parseString(Files.readString(root.resolve("tbmodern/bracelets.json"))).getAsJsonArray();
        var config=JsonParser.parseString(Files.readString(root.resolve("tbmodern/configured-data.json"))).getAsJsonObject();
        var modules=JsonParser.parseString(Files.readString(root.resolve("tbmodern/research-modules.json"))).getAsJsonObject();
        var ids=new HashSet<String>();int[] capacities={10,15,17,20,23,23,25,25,25,25,25,27,30};int i=0;
        for(var entry:rows) {
            var row=entry.getAsJsonObject();String id=row.get("id").getAsString();assertTrue(ids.add(id));
            assertEquals(capacities[i++],row.get("capacity").getAsInt());
            var recipe=JsonParser.parseString(Files.readString(root.resolve("data/thaumicbases/recipes/equipment/"+id+"braceletrec.json"))).getAsJsonObject();
            assertEquals("thaumicbases:bracelet_"+id,recipe.getAsJsonObject("result").get("item").getAsString());
            assertTrue(config.get("research_catalog/modern.json").toString().contains(recipe.get("research").getAsString()));
            assertTrue(modules.get("enableBracelets").toString().contains(recipe.get("research").getAsString()));
            assertTrue(Files.exists(root.resolve("assets/thaumicbases/models/item/bracelet_"+id+".json")));
        }
        assertEquals(13,ids.size());
    }
    @Test void originalModelAndAddonTexturesExistWithoutBeingBundled() throws Exception {
        var mappings=JsonParser.parseString(Files.readString(root.resolve("tbmodern/resource-map.json"))).getAsJsonObject().getAsJsonObject("mapping");
        try(var jar=new ZipFile("references/ThaumicBases-1.3.1710.4.jar")) {
            for(String resource:List.of("models/bracelet/bracelet.obj","textures/blocks/sugarcaneblock.png","textures/blocks/boneblock.png","textures/blocks/blazeblock.png","textures/blocks/voidblock.png")) {
                String path="assets/thaumicbases/"+resource;assertNotNull(jar.getEntry(mappings.get(path).getAsString()));assertFalse(Files.exists(root.resolve(path)));
            }
        }
    }
}
