package tc4revival.aspect.recipe;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.aspect.item.ItemAspectResolver;
import tc4revival.aspect.item.ItemBaseAspectSession;
import tc4revival.aspect.item.LegacyItemIdentity;
import tc4revival.aspect.item.LegacyItemMappings;
import tc4revival.aspect.item.StackAspectBonuses;
import tc4revival.aspect.recipe.RecipeAspectData.Key;
import tc4revival.data.LegacyItemDefinitions;
import static org.junit.jupiter.api.Assertions.*;

class SpecialStackIdentityTest {
    private final LegacyItemMappings.Definition mapping = mapping();
    private final RecipeAspectData data = LegacyRecipeFixture.data();
    private final LegacyItemIdentity identity = new LegacyItemIdentity(mapping.items(), mapping.groups(), data);
    private final AspectCatalog catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());

    @Test void all70OldSpecialStacksKeepTheirMetadataHashAndOrderedFinalAspects() {
        int checked = 0;
        for (var value : LegacyRecipeFixture.read("special-item-stacks.json")) {
            var row = value.getAsJsonObject();
            String oldId = row.get("item").getAsString();
            int requested = row.get("requested_metadata").getAsInt();
            var oldKey = LegacyRecipeFixture.key(row);
            String modern = oldId.equals("minecraft:filled_map") ? oldId : mapping.spawnEggs().entrySet().stream()
                    .filter(entry -> entry.getValue().equals(oldKey)).findFirst().orElseThrow().getKey();
            var actualKey = identity.stackKey(modern, requested, mapping.spawnEggs());
            assertEquals(oldKey, actualKey);
            assertEquals(row.get("hash").getAsInt(), identity.scanHash(actualKey), row.toString());
            var base = new ItemBaseAspectSession(identity, data, catalog, new ItemAspectResolver(LegacyItemDefinitions.rules(), catalog), (id, tag) -> true).base(modern, actualKey);
            assertEquals(row.get("has_aspect_list").getAsBoolean(), base.isPresent());
            int level = row.get("sharpness").getAsInt();
            var amounts = new StackAspectBonuses(catalog).apply(base.orElse(null), null, null,
                    level == 0 ? List.of() : List.of(new StackAspectBonuses.EnchantmentContribution("telum", (short) level)));
            var actual = new LinkedHashMap<String, Integer>();
            amounts.view().forEach((aspect, quantity) -> actual.put(aspect.tag(), quantity));
            assertEquals(new ArrayList<>(LegacyRecipeFixture.quantities(row.getAsJsonObject("final_aspects")).entrySet()), new ArrayList<>(actual.entrySet()), row.toString());
            checked++;
        }
        assertEquals(70, checked);
    }

    @Test void only24OldEggsAreMappedAndMapNumbersRemainDistinct() {
        assertEquals(24, mapping.spawnEggs().size());
        assertEquals(469, mapping.items().size());
        assertEquals(new Key("minecraft:spawn_egg", 57), identity.stackKey("minecraft:zombified_piglin_spawn_egg", null, mapping.spawnEggs()));
        assertEquals(new Key("minecraft:spawn_egg", 62), identity.stackKey("minecraft:magma_cube_spawn_egg", null, mapping.spawnEggs()));
        for (String id : List.of("piglin_spawn_egg", "allay_spawn_egg", "rabbit_spawn_egg", "donkey_spawn_egg", "wither_skeleton_spawn_egg")) {
            assertNull(identity.stackKey("minecraft:" + id, null, mapping.spawnEggs()), id);
        }
        assertEquals(new Key("minecraft:filled_map", 0), identity.stackKey("minecraft:filled_map", null, mapping.spawnEggs()));
        assertNotEquals(identity.scanHash(new Key("minecraft:filled_map", 0)), identity.scanHash(new Key("minecraft:filled_map", 42)));
        assertEquals(new Key("minecraft:iron_ingot", 0), identity.stackKey("minecraft:iron_ingot", 42, mapping.spawnEggs()));
    }

    @Test void eggCodecRejectsDuplicateIdsInvalidMetadataAndOrdinaryAliases() {
        var json = raw();
        var eggs = json.getAsJsonObject().getAsJsonArray("spawn_eggs");
        eggs.add(eggs.get(0).deepCopy());
        assertThrows(IllegalArgumentException.class, () -> LegacyItemMappings.decode(json));
        var invalid = raw();
        invalid.getAsJsonObject().getAsJsonArray("spawn_eggs").get(0).getAsJsonObject().addProperty("metadata", -1);
        assertThrows(IllegalArgumentException.class, () -> LegacyItemMappings.decode(invalid));
        var wrongItem = raw();
        wrongItem.getAsJsonObject().getAsJsonArray("spawn_eggs").get(0).getAsJsonObject().addProperty("item", "minecraft:iron_ingot");
        assertThrows(IllegalArgumentException.class, () -> LegacyItemMappings.decode(wrongItem));
    }
    private static LegacyItemMappings.Definition mapping() { return LegacyItemMappings.decode(raw()); }
    private static com.google.gson.JsonElement raw() {
        try (var reader = new InputStreamReader(SpecialStackIdentityTest.class.getResourceAsStream("/data/thaumcraft/legacy_item_mappings/vanilla.json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch (Exception failure) { throw new IllegalStateException(failure); }
    }
}
