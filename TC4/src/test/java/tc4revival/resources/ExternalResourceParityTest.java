package tc4revival.resources;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Local migration comparison: the original JAR and pre-migration text never become test resources. */
class ExternalResourceParityTest {
    @TempDir Path game;

    @Test void convertedLanguagesPreserveUnchangedTranslationsAndAuthoredOverrides() throws Exception {
        Path original = Path.of("references/Thaumcraft-1.7.10-4.2.3.5.jar");
        Path baseline = Path.of(".tooling/external-resources-before");
        assumeTrue(Files.isRegularFile(original) && Files.isDirectory(baseline), "Private migration references required");
        var mapping = OriginalResources.mappings();
        var source = OriginalResources.read(original, mapping);
        assertTrue(source.reference());
        var output = ExternalResourceCache.convert(source, mapping);
        for (String locale : new String[] {"en_us", "zh_cn"}) {
            var actual = JsonParser.parseString(new String(output.get("assets/thaumcraft/lang/" + locale + ".json"), StandardCharsets.UTF_8)).getAsJsonObject();
            var authored = JsonParser.parseString(Files.readString(Path.of("src/generated/resources/assets/thaumcraft/lang/" + locale + ".json"))).getAsJsonObject();
            authored.entrySet().forEach(entry -> actual.add(entry.getKey(), entry.getValue()));
            var expected = JsonParser.parseString(Files.readString(baseline.resolve(locale + ".json")));
            for (var entry : expected.getAsJsonObject().entrySet()) {
                if (entry.getKey().startsWith("block.thaumcraft.cultist_banner")) continue;
                // Explicit corrections to two erroneous original Chinese rod names.
                if (locale.equals("zh_cn") && java.util.Set.of("item.Wand.ice.rod","item.Wand.bone.rod").contains(entry.getKey())) continue;
                if (locale.equals("zh_cn") && entry.getKey().equals("tc.research_page.COREHARVEST.1")) {
                    assertEquals(entry.getValue().getAsString().replace("自动冲洗播种","自动重新播种"),actual.get(entry.getKey()).getAsString());
                } else assertEquals(entry.getValue(), actual.get(entry.getKey()), locale + " " + entry.getKey());
            }
            authored.entrySet().forEach(entry -> assertEquals(entry.getValue(), actual.get(entry.getKey())));
        }
        Path cache = ExternalResourceCache.prepare(game, source, mapping);
        Path damaged = cache.resolve("assets/thaumcraft/lang/en_us.json");
        Files.writeString(damaged, "incomplete");
        assertEquals(cache, ExternalResourceCache.prepare(game, source, mapping));
        assertArrayEquals(output.get("assets/thaumcraft/lang/en_us.json"), Files.readAllBytes(damaged));
        for (var entry : mapping.entrySet()) assertArrayEquals(source.resources().get(entry.getValue()),
                output.get(entry.getKey()), entry.getKey());
    }
}
