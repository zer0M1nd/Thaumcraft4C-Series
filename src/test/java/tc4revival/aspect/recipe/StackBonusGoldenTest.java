package tc4revival.aspect.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.aspect.item.ItemAspectJson;
import tc4revival.aspect.item.ItemAspectResolver;
import tc4revival.aspect.item.LegacyEnchantmentBonuses;
import tc4revival.aspect.item.StackAspectBonuses;
import static org.junit.jupiter.api.Assertions.*;

class StackBonusGoldenTest {
    private final AspectCatalog catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private final StackAspectBonuses bonuses = new StackAspectBonuses(catalog);
    private final ItemAspectResolver equipment = equipment();

    @Test void all924PlainResultsPreserveLegacyQuantitiesWithoutTruncation() {
        int count = 0;
        for (var value : LegacyRecipeFixture.read("bonus-vanilla-aspects.json")) {
            var row = value.getAsJsonObject();
            String item = row.get("item").getAsString();
            var gear = equipment.resolve(item, tag -> true).orElseGet(AspectAmounts::new);
            assertEquals(entries(amounts(row.getAsJsonObject("equipment_only"))), entries(gear), item);
            // Equipment minima are materialized into base definitions in the new format.
            var result = bonuses.apply(amounts(row.getAsJsonObject("base")).merge(gear), null, null, List.of());
            assertEquals(entries(amounts(row.getAsJsonObject("base")).merge(gear)), entries(result), item + ":" + row.get("metadata"));
            count++;
        }
        assertEquals(924, count);
        assertEquals(47, equipment.itemCount());
    }

    @Test void all64LegacyEnchantedCasesMatchIncludingBooksDuplicatesSignedAndUnknownIds() {
        int count = 0;
        for (var value : LegacyRecipeFixture.read("enchanted-aspects.json")) {
            var row = value.getAsJsonObject();
            String item = row.get("item").getAsString();
            var enchantments = new ArrayList<StackAspectBonuses.EnchantmentContribution>();
            for (var entry : row.getAsJsonArray("enchantments")) {
                int oldId = entry.getAsJsonArray().get(0).getAsInt();
                short level = entry.getAsJsonArray().get(1).getAsShort();
                String modernId = modernEnchantment(oldId);
                enchantments.add(new StackAspectBonuses.EnchantmentContribution(modernId == null ? null : LegacyEnchantmentBonuses.aspect(modernId), level));
            }
            var base = amounts(row.getAsJsonObject("base")).merge(equipment.resolve(item, tag -> true).orElseGet(AspectAmounts::new));
            var result = bonuses.apply(base, null, null, enchantments);
            var expected=amounts(row.getAsJsonObject("final_aspects"));
            // Recorded 1.7 output dropped low-weight types above six; those retained quantities remain identical.
            expected.view().forEach((aspect,amount)->assertEquals(amount.intValue(),result.amount(aspect),item+":"+aspect.tag()));
            if(result.size()<=6)assertEquals(entries(expected),entries(result),item+":"+row.get("enchantments"));
            count++;
        }
        assertEquals(64, count);
        assertEquals("instrumentum", LegacyEnchantmentBonuses.aspect("minecraft:mending"));
        assertEquals("telum", LegacyEnchantmentBonuses.aspect("minecraft:sweeping"));
    }

    @Test void containerFilteringPrecedesBaseAdditionAndDoesNotClampCombinedAmounts() {
        var stored = new AspectAmounts().add(catalog.require("aer"), 100).add(catalog.require("terra"), 0).add(catalog.require("ignis"), -2);
        var base = new AspectAmounts().add(catalog.require("aer"), 64).add(catalog.require("terra"), 0);
        var result = bonuses.apply(base, stored, null, List.of());
        assertEquals(164, result.amount(catalog.require("aer")));
        assertTrue(result.contains(catalog.require("terra")));
        assertFalse(result.contains(catalog.require("ignis")));
        assertEquals(100, stored.amount(catalog.require("aer")));
    }

    @Test void allEntriesAndTheirOrderSurviveWithoutMutatingTheInput() {
        var first = new AspectAmounts().add(catalog.require("aer"), 10).add(catalog.require("vitreus"), 9);
        for (String tag : List.of("terra", "ignis", "aqua", "ordo", "perditio")) first.add(catalog.require(tag), 20);
        var result = bonuses.cull(first);
        assertTrue(result.contains(catalog.require("aer")));
        assertEquals(entries(first),entries(result));
        assertNotSame(first,result);
        assertEquals(7, first.size());
    }

    @Test void storedAspectsAndEveryFabricAspectRemainAvailable() {
        var base = new AspectAmounts().add(catalog.require("bestia"), 3).add(catalog.require("pannus"), 6);
        for (String tag : List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio")) base.add(catalog.require(tag), 1);
        assertTrue(bonuses.apply(base, null, null, List.of()).contains(catalog.require("aer")));
        var stored = new AspectAmounts().add(catalog.require("aer"), 100).add(catalog.require("ignis"), 0).add(catalog.require("aqua"), -2);
        var result = bonuses.apply(base, stored, null, List.of());
        assertEquals(101, result.amount(catalog.require("aer")));
        assertEquals(List.of("aer", "bestia", "pannus", "terra", "ignis", "aqua", "ordo", "perditio"), result.aspects().stream().map(tc4revival.aspect.Aspect::tag).toList());
        assertEquals(8, base.size());
    }

    @Test void valuesAboveTheOldSentinelCannotHangCulling() {
        var source = new AspectAmounts();
        for (String tag : List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio", "vitreus")) source.add(catalog.require(tag), Integer.MAX_VALUE);
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> assertEquals(7, bonuses.cull(source).size()));
        assertEquals(7, source.size());
    }

    private AspectAmounts amounts(JsonObject values) {
        var result = new AspectAmounts();
        values.entrySet().forEach(entry -> result.add(catalog.require(entry.getKey()), entry.getValue().getAsInt()));
        return result;
    }
    private List<Map.Entry<String, Integer>> entries(AspectAmounts values) {
        var result = new LinkedHashMap<String, Integer>();
        values.view().forEach((aspect, quantity) -> result.put(aspect.tag(), quantity));
        return new ArrayList<>(result.entrySet());
    }
    private ItemAspectResolver equipment() {
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/aspect-migration/legacy-equipment.json"), StandardCharsets.UTF_8)) {
            return new ItemAspectResolver(ItemAspectJson.decode(JsonParser.parseReader(reader)), catalog);
        } catch (Exception failure) { throw new IllegalStateException(failure); }
    }
    private static String modernEnchantment(int oldId) {
        return switch (oldId) {
            case 0 -> "minecraft:protection"; case 1 -> "minecraft:fire_protection";
            case 2 -> "minecraft:feather_falling"; case 3 -> "minecraft:blast_protection";
            case 4 -> "minecraft:projectile_protection"; case 5 -> "minecraft:respiration";
            case 6 -> "minecraft:aqua_affinity"; case 7 -> "minecraft:thorns";
            case 16 -> "minecraft:sharpness"; case 17 -> "minecraft:smite";
            case 18 -> "minecraft:bane_of_arthropods"; case 19 -> "minecraft:knockback";
            case 20 -> "minecraft:fire_aspect"; case 21 -> "minecraft:looting";
            case 32 -> "minecraft:efficiency"; case 33 -> "minecraft:silk_touch";
            case 34 -> "minecraft:unbreaking"; case 35 -> "minecraft:fortune";
            case 48 -> "minecraft:power"; case 49 -> "minecraft:punch";
            case 50 -> "minecraft:flame"; case 51 -> "minecraft:infinity";
            case 61 -> "minecraft:luck_of_the_sea"; case 62 -> "minecraft:lure";
            case 150 -> "thaumcraft:haste"; case 151 -> "thaumcraft:repair";
            default -> null;
        };
    }
}
