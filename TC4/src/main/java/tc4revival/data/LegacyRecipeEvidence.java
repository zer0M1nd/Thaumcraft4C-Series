package tc4revival.data;

import tc4revival.aspect.recipe.RecipeAspectData;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tc4revival.aspect.recipe.RecipeAspectData.Ingredient;
import tc4revival.aspect.recipe.RecipeAspectData.Key;
import tc4revival.aspect.recipe.RecipeAspectData.Kind;
import tc4revival.aspect.recipe.RecipeAspectData.Recipe;

/** Converts observer evidence into the independently implemented calculation model. Used only for DataGen and verification. */
public  class LegacyRecipeEvidence {
    private LegacyRecipeEvidence() {}

    public static RecipeAspectData convert(JsonArray definitions, JsonArray registered, JsonArray crafting, JsonArray thaumcraft) {
        var items = new LinkedHashMap<String, RecipeAspectData.ItemProperties>();
        for (var entry : definitions) {
            var row = entry.getAsJsonObject();
            items.put(row.get("item").getAsString(), new RecipeAspectData.ItemProperties(row.get("damageable").getAsBoolean(),
                    row.get("has_subtypes").getAsBoolean(), row.has("container") ? row.get("container").getAsString() : null));
        }
        var registrations = new ArrayList<RecipeAspectData.Registration>();
        for (var entry : registered) {
            var row = entry.getAsJsonObject();
            registrations.add(new RecipeAspectData.Registration(key(row), quantities(row.getAsJsonObject("aspects"))));
        }
        var recipes = new ArrayList<Recipe>();
        for (JsonArray file : List.of(crafting, thaumcraft)) {
            for (var entry : file) {
                var row = entry.getAsJsonObject();
                if (!row.has("output") || !row.get("output").isJsonObject()) continue;
                String type = row.get("aspect_recipe_kind").getAsString();
                Kind kind = switch (type) {
                    case "crucible" -> Kind.CRUCIBLE;
                    case "infusion" -> Kind.INFUSION;
                    case "arcane_shaped", "arcane_shapeless", "arcane_other" -> Kind.ARCANE;
                    case "crafting_shaped", "crafting_shapeless", "crafting_ore" -> Kind.CRAFTING;
                    default -> null;
                };
                if (kind == null) continue;
                var output = row.getAsJsonObject("output");
                var ingredients = new ArrayList<Ingredient>();
                if (kind == Kind.CRUCIBLE) {
                    if (row.has("catalyst")) add(ingredients, row.get("catalyst"));
                } else {
                    if (kind == Kind.INFUSION && row.has("center")) add(ingredients, row.get("center"));
                    if (row.has("ingredients")) {
                        var input = row.getAsJsonArray("ingredients");
                        if (type.endsWith("_shaped")) {
                            int width = row.get("width").getAsInt(), height = row.get("height").getAsInt();
                            for (int x = 0; x < Math.min(width, 3); x++) for (int y = 0; y < Math.min(height, 3); y++) add(ingredients, input.get(x + y * width));
                        } else {
                            int limit = kind == Kind.INFUSION ? input.size() : Math.min(9, input.size());
                            for (int index = 0; index < limit; index++) add(ingredients, input.get(index));
                        }
                    }
                }
                recipes.add(new Recipe(kind, key(output), output.get("count").getAsInt(), ingredients,
                        row.has("aspects") ? quantities(row.getAsJsonObject("aspects")) : Map.of()));
            }
        }
        return new RecipeAspectData(items, registrations, recipes);
    }

    private static void add(List<Ingredient> destination, JsonElement entry) {
        if (entry == null || entry.isJsonNull()) return;
        if (entry.isJsonArray()) {
            var choices = new ArrayList<Key>();
            for (var choice : entry.getAsJsonArray()) if (choice.isJsonObject()) choices.add(key(choice.getAsJsonObject()));
            destination.add(new Ingredient(choices, true));
        } else if (entry.isJsonObject()) destination.add(Ingredient.direct(key(entry.getAsJsonObject())));
    }

    static Key key(JsonObject row) { return new Key(row.get("item").getAsString(), row.get("metadata").getAsInt()); }
    static Map<String, Integer> quantities(JsonObject object) {
        var result = new LinkedHashMap<String, Integer>();
        object.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsInt()));
        return result;
    }
}
