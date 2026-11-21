package tc4revival.aspect.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.aspect.item.LegacyItemIdentity;
import tc4revival.aspect.item.StackAspectBonuses;
import static org.junit.jupiter.api.Assertions.*;

class FoundationItemAspectTest {
    @Test void sevenShardsAndThaumometerPreserveOldBaseAndHashesWithoutCulling() {
        var catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
        var data = LegacyRecipeFixture.data();
        var identity = new LegacyItemIdentity(Map.of(), Map.of(), data);
        int checked = 0;
        for (var value : LegacyRecipeFixture.read("foundation-items.json")) {
            var row = value.getAsJsonObject();
            var key = LegacyRecipeFixture.key(row);
            var base = new RecipeAspectEngine(data, catalog).object(key);
            var canonical = new RecipeAspectEngine(data, catalog).generate(key);
            assertEquals(row.get("has_aspect_list").getAsBoolean(), base != null, key.toString());
            assertEquals(row.get("has_canonical_list").getAsBoolean(), canonical != null, key.toString());
            assertEquals(new ArrayList<>(LegacyRecipeFixture.quantities(row.getAsJsonObject("aspects")).entrySet()), entries(base), key.toString());
            assertEquals(new ArrayList<>(LegacyRecipeFixture.quantities(row.getAsJsonObject("canonical_aspects")).entrySet()), entries(canonical), key.toString());
            var complete = new StackAspectBonuses(catalog).apply(base, null, null, List.of());
            assertEquals(new ArrayList<>(LegacyRecipeFixture.quantities(row.getAsJsonObject("aspects")).entrySet()), entries(complete), key.toString());
            assertEquals(row.get("initial_hash").getAsInt(), identity.scanHash(key));
            assertEquals(row.get("resolved_hash").getAsInt(), identity.scanHash(key));
            checked++;
        }
        assertEquals(8, checked);
    }
    private static List<Map.Entry<String, Integer>> entries(AspectAmounts amounts) {
        var result = new LinkedHashMap<String, Integer>();
        if (amounts != null) amounts.view().forEach((aspect, quantity) -> result.put(aspect.tag(), quantity));
        return new ArrayList<>(result.entrySet());
    }
}
