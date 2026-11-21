package tc4revival.aspect.recipe;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.aspect.item.ItemAspectResolver;
import tc4revival.aspect.item.ItemBaseAspectSession;
import tc4revival.aspect.item.LegacyItemIdentity;
import tc4revival.aspect.item.LegacyItemMappings;
import tc4revival.data.LegacyItemDefinitions;
import static org.junit.jupiter.api.Assertions.*;

class ItemIdentityGoldenTest {
    private final AspectCatalog catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private final RecipeAspectData data = LegacyRecipeFixture.data();
    private final LegacyItemMappings.Definition mapping = mapping();
    private final LegacyItemIdentity identity = new LegacyItemIdentity(mapping.items(), mapping.groups(), data);

    @Test void hashesMatchAll924ActualLegacyInitialAndResolvedObservations() {
        int count = 0;
        for (var value : LegacyRecipeFixture.read("scan-vanilla-aspects.json")) {
            var row = value.getAsJsonObject();
            var key = LegacyRecipeFixture.key(row);
            assertEquals(row.get("scan_metadata").getAsInt(), identity.scanKey(key).metadata(), key.toString());
            assertEquals(row.get("initial_hash").getAsInt(), identity.scanHash(key), key.toString());
            assertEquals(row.get("resolved_hash").getAsInt(), identity.scanHash(key), key.toString());
            count++;
        }
        assertEquals(924, count);
    }

    @Test void modernMappingsMatchLegacyBaseQuantitiesAndOrder() {
        var observations = new HashMap<RecipeAspectData.Key, com.google.gson.JsonObject>();
        for (var value : LegacyRecipeFixture.read("scan-vanilla-aspects.json")) observations.put(LegacyRecipeFixture.key(value.getAsJsonObject()), value.getAsJsonObject());
        int checked = 0;
        var differences = new ArrayList<String>();
        for (var alias : identity.items().entrySet()) {
            if (!alias.getValue().item().startsWith("minecraft:")) continue;
            var row = observations.get(alias.getValue());
            assertNotNull(row, alias.toString());
            var actual = session((id, tag) -> true).base(alias.getKey()).orElse(null);
            if ((actual != null) != row.get("has_aspect_list").getAsBoolean()
                    || !new ArrayList<>(values(actual).entrySet()).equals(new ArrayList<>(LegacyRecipeFixture.quantities(row.getAsJsonObject("aspects")).entrySet()))) {
                differences.add(alias + " actual=" + values(actual) + " expected=" + row.get("aspects"));
            }
            checked++;
        }
        assertEquals(468, checked);
        assertTrue(differences.isEmpty(), () -> String.join("\n", differences));
    }

    @Test void scanGroupsKeepRawRecipeKeysAndIronUsesTheLegacyNuggetIdentity() {
        var normal = identity.recipeKey("minecraft:anvil");
        var damaged = identity.recipeKey("minecraft:damaged_anvil");
        assertNotEquals(normal, damaged);
        assertEquals(normal, identity.scanKey(damaged));
        assertEquals(identity.scanHash(normal), identity.scanHash(damaged));
        assertEquals(new RecipeAspectData.Key("Thaumcraft:ItemNugget", 0), identity.recipeKey("minecraft:iron_nugget"));
        for (String id : List.of("minecraft:dragon_head", "minecraft:cherry_log", "minecraft:black_dye", "minecraft:granite", "minecraft:netherite_ingot")) {
            assertNull(identity.recipeKey(id));
            assertTrue(session((item, tag) -> true).base(id).isEmpty());
        }
    }

    @Test void directTagRemovalDoesNotResurrectCacheAndChangesDerivedToolIngredients() {
        var normal = session((id, tag) -> true);
        assertEquals(Map.of("metallum", 1), values(normal.base("minecraft:iron_nugget").orElseThrow()));
        assertTrue(values(normal.canonical("minecraft:paper").orElseThrow()).isEmpty());
        assertEquals(Map.of("cognitio", 1), values(normal.base("minecraft:paper").orElseThrow()));
        assertEquals(Map.of("vitreus", 9, "lucrum", 9, "arbor", 1), values(normal.base("minecraft:diamond_pickaxe").orElseThrow()));
        var removed = session((id, tag) -> !tag.equals("forge:nuggets/iron") && !tag.equals("forge:gems/diamond"));
        assertTrue(removed.base("minecraft:iron_nugget").isEmpty());
        assertTrue(removed.canonical("minecraft:iron_nugget").orElseThrow().isEmpty());
        assertEquals(Map.of("arbor", 1), values(removed.base("minecraft:diamond_pickaxe").orElseThrow()));
        assertEquals(values(normal.base("minecraft:diamond_pickaxe").orElseThrow()), values(session((id, tag) -> true).base("minecraft:diamond_pickaxe").orElseThrow()));
    }

    private ItemBaseAspectSession session(BiPredicate<String, String> tags) {
        return new ItemBaseAspectSession(identity, data, catalog, new ItemAspectResolver(LegacyItemDefinitions.rules(), catalog), tags, (key,value)->value, null, new InferenceLimits(64,6));
    }
    private static LegacyItemMappings.Definition mapping() {
        try (var reader = new InputStreamReader(ItemIdentityGoldenTest.class.getResourceAsStream("/data/thaumcraft/legacy_item_mappings/vanilla.json"), StandardCharsets.UTF_8)) {
            return LegacyItemMappings.decode(JsonParser.parseReader(reader));
        } catch (Exception failure) { throw new IllegalStateException(failure); }
    }
    private static Map<String, Integer> values(AspectAmounts amounts) {
        var values = new LinkedHashMap<String, Integer>();
        if (amounts != null) amounts.view().forEach((aspect, quantity) -> values.put(aspect.tag(), quantity));
        return values;
    }
}
