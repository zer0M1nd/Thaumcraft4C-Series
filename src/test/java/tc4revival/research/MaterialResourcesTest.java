package tc4revival.research;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import tc4revival.content.MaterialType;
import static org.junit.jupiter.api.Assertions.*;

class MaterialResourcesTest {
    @Test void materialModelsUseExpectedArtworkAndBothOriginalNames() throws Exception {
        // The supplied 0920_01 ingots deliberately replace the original pixels.
        var replacements = java.util.Map.of(
                MaterialType.THAUMIUM, "8d81d74d1320e41fb518ca494aa51222c539d7f3ed44d86623b2c5c2a56bbbc8",
                MaterialType.VOID, "9077211aeee0ba49bbbc441c8bc6e1b0cb708427cc5429126a02bc001d44e4fe");
        for (var type : MaterialType.values()) {
            String texture = "assets/thaumcraft/textures/item/" + type.texture() + ".png";
            try (var stream = tc4revival.resources.ExternalTestResources.open("/" + texture)) {
                assertNotNull(stream, texture);
                if (replacements.containsKey(type)) {
                    assertEquals(replacements.get(type), tc4revival.resources.OriginalResources.sha256(stream.readAllBytes()), texture);
                    assertFalse(tc4revival.resources.OriginalResources.mappings().containsKey(texture), "Replacement must not be supplied by the old JAR");
                } else assertArrayEquals(Files.readAllBytes(Path.of("references/resources/assets/thaumcraft/textures/items/" + type.texture() + ".png")), stream.readAllBytes());
            }
            var metadata = Path.of("references/resources/assets/thaumcraft/textures/items/" + type.texture() + ".png.mcmeta");
            if (Files.exists(metadata)) try (var reader = new InputStreamReader(tc4revival.resources.ExternalTestResources.open("/" + texture + ".mcmeta"), StandardCharsets.UTF_8)) {
                assertEquals(JsonParser.parseString(Files.readString(metadata)), JsonParser.parseReader(reader), "Preserve original animation metadata");
            }
            try (var reader = new InputStreamReader(tc4revival.resources.ExternalTestResources.open("/assets/thaumcraft/models/item/" + type.path() + ".json"), StandardCharsets.UTF_8)) {
                var model = JsonParser.parseReader(reader).getAsJsonObject();
                assertEquals("minecraft:item/generated", model.get("parent").getAsString());
                assertEquals("thaumcraft:item/" + type.texture(), model.getAsJsonObject("textures").get("layer0").getAsString());
            }
            for (String locale : java.util.List.of("en_US", "zh_CN")) {
                String prefix = "item.ItemResource." + type.metadata() + ".name=";
                var expected = Files.readAllLines(Path.of("references/resources/assets/thaumcraft/lang/" + locale + ".lang"), StandardCharsets.UTF_8)
                        .stream().filter(line -> line.startsWith(prefix)).findFirst().map(line -> line.substring(prefix.length()));
                try (var reader = new InputStreamReader(tc4revival.resources.ExternalTestResources.open("/assets/thaumcraft/lang/" + locale.toLowerCase(java.util.Locale.ROOT) + ".json"), StandardCharsets.UTF_8)) {
                    var language = JsonParser.parseReader(reader).getAsJsonObject();
                    if (expected.isPresent()) assertEquals(expected.get(), language.get("item.thaumcraft." + type.path()).getAsString());
                    else {
                        assertEquals(MaterialType.GOLD_COIN, type); assertEquals("zh_CN", locale);
                        assertEquals("金币", language.get("item.thaumcraft." + type.path()).getAsString(), "0911_2 visual report explicitly requires the restored Chinese coin name");
                    }
                }
            }
        }
    }
    @Test void materialTagsAndDisplayIdentitiesUseTheOldMetadataAssignments() throws Exception {
        var items = ResearchDisplayItems.bundled();
        for (var type : MaterialType.values()) {
            var identity = new ResearchDisplayItems.Identity("Thaumcraft:ItemResource", type.metadata(), java.util.Optional.empty());
            if (items.entries().containsKey(identity)) assertEquals("thaumcraft:" + type.path(), items.entries().get(identity).variants().get(0).item());
            for (String tag : type.tags()) {
                String path = "/data/" + tag.replace(':', '/') + ".json";
                path = path.replace("/data/forge/", "/data/forge/tags/items/");
                try (var reader = new InputStreamReader(tc4revival.resources.ExternalTestResources.open(path), StandardCharsets.UTF_8)) {
                    var values = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("values");
                    assertTrue(values.asList().stream().anyMatch(value -> value.getAsString().equals("thaumcraft:" + type.path())), tag);
                }
            }
        }
        assertEquals(java.util.List.of("forge:quicksilver"), MaterialType.QUICKSILVER.tags());
        assertEquals(java.util.List.of("forge:nuggets/gold"), MaterialType.GOLD_COIN.tags());
        assertFalse(MaterialType.DEFUNCT_BRAIN.creativeVisible());
    }
}
