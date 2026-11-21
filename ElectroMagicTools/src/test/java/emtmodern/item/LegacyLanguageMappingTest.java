package emtmodern.item;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

public class LegacyLanguageMappingTest {
    @Test void directTooltipAndResearchLookupsSurviveTheResourceBridge() throws Exception {
        var definition = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/data/emt/thaumcraft/original_resources.json"))).getAsJsonObject();
        for (String lang : new String[]{"en_us", "zh_cn"}) {
            var aliases = definition.getAsJsonObject("languages").getAsJsonObject("assets/emt/lang/" + lang + ".json");
            for (String key : new String[]{"tooltip.EMT.visDiscount", "tooltip.EMT.normalConditions",
                    "tooltip.EMT.euPerTick", "tooltip.EMT.specialEffect", "tooltip.EMT.output.double.half",
                    "emt.text.Electric Magic Tools"}) {
                assertTrue(aliases.has(key), lang + ": " + key);
                assertEquals(key, aliases.get(key).getAsString());
            }
            assertEquals("item.EMT.drill.thaumium.name", aliases.get("item.emt.thaumium_drill").getAsString());
        }
    }
}
