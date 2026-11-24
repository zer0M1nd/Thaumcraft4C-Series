package thaumicomniverse.research;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CertificateBookTest {
    @Test void taggedCertificateInputsKeepTheirTierAndAConcreteBookIcon() throws Exception {
        var root = Path.of("src/main/resources/data/thaumicomniverse");
        var book = JsonParser.parseString(Files.readString(root.resolve("research_page_recipes/atm.json")))
                .getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
        var components = book.getAsJsonObject("omniverse_certificate").getAsJsonArray("components");
        var recipe = JsonParser.parseString(Files.readString(root.resolve("recipes/atm/thaumaturge_certificate.json")))
                .getAsJsonObject().getAsJsonArray("components");
        var expected = List.of("void_3x", "thaumium_4x", "alchemical_brass_4x");
        for (int i = 0; i < components.size(); i++) {
            var input = components.get(i).getAsJsonObject();
            assertEquals(recipe.get(i).getAsJsonObject().get("ingredient"), input.get("ingredient"));
            var display = CollaborationResearch.certificateDisplay(input);
            if (i < expected.size()) {
                assertEquals("ore", display.get("kind").getAsString());
                assertEquals("#forge:storage_blocks/" + expected.get(i), display.getAsJsonArray("names").get(0).getAsString());
                display = display.getAsJsonArray("elements").get(0).getAsJsonObject();
                assertEquals(input.get("display_item"), display.getAsJsonObject("stack").get("item"));
            }
            assertEquals("stack", display.get("kind").getAsString());
            assertTrue(display.getAsJsonObject("stack").has("item"));
        }
    }
}
