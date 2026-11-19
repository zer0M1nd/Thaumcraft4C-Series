package tc4revival.aspect.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tc4revival.aspect.recipe.RecipeAspectData.Ingredient;
import tc4revival.aspect.recipe.RecipeAspectData.Key;
import tc4revival.aspect.recipe.RecipeAspectData.Kind;
import tc4revival.aspect.recipe.RecipeAspectData.Recipe;

/** Converts observer evidence into the independently implemented calculation model. Test-only. */
 class LegacyRecipeFixture {
    private LegacyRecipeFixture() {}

    static JsonArray read(String name) {
        try (var reader = new InputStreamReader(Objects.requireNonNull(LegacyRecipeFixture.class.getResourceAsStream("/" + name)), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonArray();
        } catch (Exception error) { throw new IllegalStateException("Cannot read baseline " + name, error); }
    }

    static RecipeAspectData data() {
        return tc4revival.data.LegacyRecipeEvidence.convert(read("item-definitions.json"), read("registered-aspects.json"),
                read("crafting-recipes.json"), read("thaumcraft-recipes.json"));
    }

    static Key key(JsonObject row) { return new Key(row.get("item").getAsString(), row.get("metadata").getAsInt()); }
    static Map<String, Integer> quantities(JsonObject object) {
        var result = new LinkedHashMap<String, Integer>();
        object.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsInt()));
        return result;
    }
}
