package tc4revival.aspect.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import static org.junit.jupiter.api.Assertions.*;

class RecipeAspectGoldenTest {
    private final AspectCatalog catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());

    @Test void bothEntryPointsMatchAll854NonPotionLegacyObservations() {
        var source = LegacyRecipeFixture.data();
        var data = RecipeAspectJson.decode(com.google.gson.JsonParser.parseString(RecipeAspectJson.encode(source).toString()));
        assertEquals(source, data);
        var differences = new ArrayList<String>();
        var template=new RecipeAspectEngine(data,catalog,Map.of(),(key,value)->value,new InferenceLimits(64,6));
        int count = 0;
        for (var observation : LegacyRecipeFixture.read("resolved-vanilla-aspects.json")) {
            var row = observation.getAsJsonObject();
            var key = LegacyRecipeFixture.key(row);
            if (key.item().equals("minecraft:potion")) continue;
            var object = template.fork().object(key);
            var canonical = template.fork().generate(key);
            if ((object != null) != row.get("has_aspect_list").getAsBoolean()
                    || !new ArrayList<>(values(object).entrySet()).equals(new ArrayList<>(LegacyRecipeFixture.quantities(row.getAsJsonObject("aspects")).entrySet()))) differences.add(key + " object=" + values(object) + " expected=" + row.get("aspects"));
            if ((canonical != null) != row.get("has_canonical_list").getAsBoolean()
                    || !new ArrayList<>(values(canonical).entrySet()).equals(new ArrayList<>(LegacyRecipeFixture.quantities(row.getAsJsonObject("canonical_aspects")).entrySet()))) differences.add(key + " canonical=" + values(canonical) + " expected=" + row.get("canonical_aspects"));
            count++;
        }
        assertEquals(854, count);
        assertTrue(differences.isEmpty(), () -> String.join("\n", differences));
    }

    @Test void resourceCodecRejectsFractionalCountsAndUnsupportedFormats() {
        var json = RecipeAspectJson.encode(LegacyRecipeFixture.data());
        json.getAsJsonArray("recipes").get(0).getAsJsonObject().addProperty("count", 1.5);
        assertThrows(ArithmeticException.class, () -> RecipeAspectJson.decode(json));
        json.addProperty("format", 2);
        assertThrows(IllegalArgumentException.class, () -> RecipeAspectJson.decode(json));
    }

    private Map<String, Integer> values(AspectAmounts amounts) {
        var result = new LinkedHashMap<String, Integer>();
        if (amounts != null) amounts.view().forEach((aspect, quantity) -> result.put(aspect.tag(), quantity));
        return result;
    }
}
