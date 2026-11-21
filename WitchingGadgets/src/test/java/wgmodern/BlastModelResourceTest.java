package wgmodern;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BlastModelResourceTest {
    @Test void everyFurnaceStateResolvesFaceTextureVariables() throws Exception {
        var root = Path.of("src/generated/resources/assets/witchinggadgets");
        var states = JsonParser.parseString(Files.readString(root.resolve("blockstates/infernal_blast_furnace.json")))
                .getAsJsonObject().getAsJsonObject("variants");
        assertEquals(81, states.size());
        for (var variant : states.entrySet()) {
            var modelId = variant.getValue().getAsJsonObject().get("model").getAsString();
            var model = JsonParser.parseString(Files.readString(root.resolve("models/" + modelId.split(":", 2)[1] + ".json"))).getAsJsonObject();
            var textures = model.getAsJsonObject("textures");
            for (var element : model.getAsJsonArray("elements")) {
                for (var face : element.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                    var reference = face.getValue().getAsJsonObject().get("texture").getAsString();
                    assertTrue(reference.startsWith("#"), variant.getKey() + ": face must reference a texture variable");
                    assertTrue(textures.has(reference.substring(1)), variant.getKey() + ": unresolved " + reference);
                    assertTrue(textures.get(reference.substring(1)).getAsString().startsWith("witchinggadgets:blocks/blastfurnace"));
                }
            }
        }
    }
}
