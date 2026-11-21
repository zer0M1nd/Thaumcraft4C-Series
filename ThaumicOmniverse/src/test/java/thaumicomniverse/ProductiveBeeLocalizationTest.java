package thaumicomniverse;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ProductiveBeeLocalizationTest {
    private static final Path RESOURCES = Path.of("src/main/resources");

    @ParameterizedTest
    @ValueSource(strings = {"zh_cn", "en_us"})
    void everyConfiguredBeeHasTheNameUsedByProductiveBees(String locale) throws Exception {
        var language = JsonParser.parseString(Files.readString(RESOURCES.resolve(
                "assets/thaumicomniverse/lang/" + locale + ".json"))).getAsJsonObject();
        var manifest = JsonParser.parseString(Files.readString(RESOURCES.resolve(
                "thaumicomniverse/renewables.json"))).getAsJsonArray();
        Set<String> expectedNames = new HashSet<>();
        Set<String> configuredNames = new HashSet<>();
        try (var files = Files.walk(RESOURCES.resolve("data/thaumicomniverse/productivebees"))) {
            for (var file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                // BeeReloadListener drops data-pack grouping directories from the bee ID.
                String name = file.getFileName().toString().replaceFirst("\\.json$", "");
                assertTrue(configuredNames.add(name), "Duplicate bee ID: " + name);
            }
        }
        assertFalse(configuredNames.isEmpty());
        for (var entry : manifest) {
            var row = entry.getAsJsonObject();
            String name = row.get("name").getAsString();
            expectedNames.add(name);
            // Productive Bees 12.6.0 removes the namespace, then appends _bee.
            // Entities, cages, combs, comb blocks, spawn eggs and JEI share this name.
            String key = "entity.productivebees." + name.replace("_bee", "") + "_bee";
            assertTrue(language.has(key), locale + " is missing " + key);
            String expected = row.get(locale.equals("zh_cn") ? "zh" : "en").getAsString()
                    + (locale.equals("zh_cn") ? "蜂" : " Bee");
            assertEquals(expected, language.get(key).getAsString(), key);
        }
        assertEquals(expectedNames, configuredNames, "Cover every optional bee definition");
    }
}
