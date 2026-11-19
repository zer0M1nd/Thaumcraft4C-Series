package gadomancymodern.automation;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

public class LegacyLanguageMappingTest {
    @Test void packagerOptionsKeepTheirOriginalTranslationKeys() throws Exception {
        var definition = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/data/gadomancy/thaumcraft/original_resources.json"))).getAsJsonObject();
        for (String lang : new String[]{"en_us", "zh_cn"}) {
            var aliases = definition.getAsJsonObject("languages").getAsJsonObject("assets/gadomancy/lang/" + lang + ".json");
            for (String suffix : new String[]{"useEssentia", "autoStart", "disguisePackage"}) {
                String key = "gadomancy.info.ArcanePackager." + suffix;
                assertTrue(aliases.has(key), lang + ": " + key);
                assertEquals(key, aliases.get(key).getAsString());
            }
            assertEquals("tile.BlockStoneMachine.4.name", aliases.get("block.gadomancy.arcane_packager").getAsString());
        }
    }
}
