package tc4revival.aspect.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.aspect.item.PotionAspectJson;
import tc4revival.aspect.item.PotionAspectRules;
import tc4revival.aspect.item.PotionAspectData;
import static org.junit.jupiter.api.Assertions.*;

class PotionAspectGoldenTest {
    private final AspectCatalog catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private final RecipeAspectData data = LegacyRecipeFixture.data();
    private final PotionAspectRules potions = new PotionAspectRules(PotionAspectJson.definitions(LegacyRecipeFixture.read("potion-aspects.json")), catalog);
    private RecipeAspectEngine engine() { return new RecipeAspectEngine(data, catalog, Map.of(), potions::decorate); }

    @Test void bothEntriesMatchAll577OldPotionQueriesIncludingNullAndOrder() {
        int count = 0;
        for (var value : LegacyRecipeFixture.read("potion-aspects.json")) {
            var row = value.getAsJsonObject();
            var key = new RecipeAspectData.Key("minecraft:potion", row.get("metadata").getAsInt());
            var object = engine().object(key);
            var canonical = engine().generate(key);
            assertEquals(row.get("has_aspect_list").getAsBoolean(), object != null, key.toString());
            assertEquals(new ArrayList<>(LegacyRecipeFixture.quantities(row.getAsJsonObject("aspects")).entrySet()), entries(object), key.toString());
            assertEquals(row.get("has_canonical_list").getAsBoolean(), canonical != null, key.toString());
            assertEquals(new ArrayList<>(LegacyRecipeFixture.quantities(row.getAsJsonObject("canonical_aspects")).entrySet()), entries(canonical), key.toString());
            count++;
        }
        assertEquals(577, count);
    }

    @Test void emptyPotionSplashDoesNotGainEntropyAndGeneratedCacheRetainsTheOldFirstCallDifference() {
        var water = engine().object(new RecipeAspectData.Key("minecraft:potion", 0));
        assertEquals(1, water.amount(catalog.require("vitreus")));
        var splash = engine().object(new RecipeAspectData.Key("minecraft:potion", 16384));
        assertEquals(0, splash.amount(catalog.require("perditio")));
        assertEquals(0, splash.amount(catalog.require("vitreus")));
        var session = engine();
        var key = new RecipeAspectData.Key("minecraft:potion", 8226);
        assertNull(session.generate(key));
        assertEquals(6, session.generate(key).amount(catalog.require("motus")));
    }

    @Test void generatedMappingsKeep64StandardVariantsAndOnlyAcceptMatchingPreservedMetadata() throws Exception {
        PotionAspectData mapped;
        try (var reader = new java.io.InputStreamReader(getClass().getResourceAsStream("/data/thaumcraft/potion_aspects/legacy.json"), java.nio.charset.StandardCharsets.UTF_8)) {
            mapped = PotionAspectJson.decode(com.google.gson.JsonParser.parseReader(reader));
        }
        assertEquals(mapped, PotionAspectJson.decode(PotionAspectJson.encode(mapped)));
        assertEquals(64, mapped.defaults().size());
        assertEquals(504, mapped.represented().size());
        var speed = new PotionAspectData.Variant("minecraft:potion", "minecraft:strong_swiftness");
        assertEquals(8226, mapped.metadata(speed, null));
        assertEquals(16306, mapped.metadata(speed, 16306));
        assertEquals(8226, mapped.metadata(speed, 0));
        assertEquals(0, mapped.metadata(new PotionAspectData.Variant("minecraft:potion", "minecraft:water"), 16306));
        assertNull(mapped.metadata(new PotionAspectData.Variant("minecraft:lingering_potion", "minecraft:water"), 0));
        assertNull(mapped.metadata(new PotionAspectData.Variant("minecraft:potion", "minecraft:leaping"), null));
        assertNull(mapped.metadata(new PotionAspectData.Variant("minecraft:potion", "minecraft:luck"), null));
        assertEquals(0, engine().object(new RecipeAspectData.Key("minecraft:potion", 8226)).amount(catalog.require("vitreus")));
        assertEquals(1, engine().object(new RecipeAspectData.Key("minecraft:potion", 16306)).amount(catalog.require("vitreus")));
    }

    private java.util.List<Map.Entry<String, Integer>> entries(AspectAmounts amounts) {
        var result = new LinkedHashMap<String, Integer>();
        if (amounts != null) amounts.view().forEach((aspect, amount) -> result.put(aspect.tag(), amount));
        return new ArrayList<>(result.entrySet());
    }
}
