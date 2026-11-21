package thaumicomniverse;

import com.google.gson.JsonParser;
import java.nio.file.*;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AtmBeeRecipeContractTest {
    @Test void everyCentrifugeSupportsAtm9GtceuRecipeIds() throws Exception {
        var root = Path.of("src/main/resources/data");
        var names = new HashSet<String>();
        var manifest = JsonParser.parseString(Files.readString(Path.of("src/main/resources/thaumicomniverse/renewables.json"))).getAsJsonArray();
        try (var files = Files.walk(root)) {
            for (var file : files.filter(p -> p.toString().endsWith(".json") && p.toString().contains("recipes")).toList()) {
                var json = JsonParser.parseString(Files.readString(file));
                if (!json.isJsonObject() || !json.getAsJsonObject().has("type") ||
                        !json.getAsJsonObject().get("type").getAsString().equals("productivebees:centrifuge")) continue;
                var relative = root.relativize(file);
                var path = relative.subpath(2, relative.getNameCount()).toString().replace('\\', '/').replace(".json", "");
                String id = relative.getName(0) + ":" + path;
                // ATM9 apiary_recipes.js builds all three GTCEu variants this way.
                String gt = "kubejs:gtceu/comb_processor" + id.replace("productivebees:centrifuge", "");
                for (String suffix : new String[]{"", "_block", "_block_max"}) {
                    assertTrue((gt + suffix).matches("[a-z0-9_.-]+:[a-z0-9/._-]+"), id);
                    assertTrue(names.add(gt + suffix), "Duplicate " + gt + suffix);
                }
                assertTrue(path.startsWith("centrifuge/thaumicomniverse/"), "Keep addon ownership in the path");
                assertTrue(json.getAsJsonObject().getAsJsonArray("conditions").asList().stream().anyMatch(c ->
                        c.getAsJsonObject().has("modid") && c.getAsJsonObject().get("modid").getAsString().equals("productivebees")));
            }
        }
        assertEquals(manifest.size() * 3, names.size());
    }
}
