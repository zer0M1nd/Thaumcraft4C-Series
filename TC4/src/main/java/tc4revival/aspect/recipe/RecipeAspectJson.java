package tc4revival.aspect.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import tc4revival.aspect.recipe.RecipeAspectData.Key;

public  class RecipeAspectJson {
    private RecipeAspectJson() {}

    public static JsonObject encode(RecipeAspectData data) {
        var root = new JsonObject();
        root.addProperty("format", 1);
        var items = new JsonArray();
        data.items().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            var row = new JsonObject();
            row.addProperty("item", entry.getKey());
            row.addProperty("damageable", entry.getValue().damageable());
            row.addProperty("subtypes", entry.getValue().hasSubtypes());
            if (entry.getValue().container() != null) row.addProperty("container", entry.getValue().container());
            items.add(row);
        });
        root.add("items", items);
        var registrations = new JsonArray();
        for (var registration : data.registrations()) {
            var row = key(registration.key());
            row.add("aspects", quantities(registration.aspects()));
            registrations.add(row);
        }
        root.add("registrations", registrations);
        var recipes = new JsonArray();
        for (var recipe : data.recipes()) {
            var row = new JsonObject();
            row.addProperty("kind", recipe.kind().name());
            row.add("output", key(recipe.output()));
            row.addProperty("count", recipe.outputCount());
            row.add("cost", quantities(recipe.cost()));
            var inputs = new JsonArray();
            for (var ingredient : recipe.ingredients()) {
                var input = new JsonObject();
                input.addProperty("choose_nonempty", ingredient.chooseFirstWithAspects());
                var alternatives = new JsonArray();
                ingredient.alternatives().forEach(candidate -> alternatives.add(key(candidate)));
                input.add("alternatives", alternatives);
                inputs.add(input);
            }
            row.add("ingredients", inputs);
            recipes.add(row);
        }
        root.add("recipes", recipes);
        return root;
    }

    public static RecipeAspectData decode(JsonElement json) {
        var root = json.getAsJsonObject();
        if (integer(root, "format") == 2) return RuntimeCraftingJson.decode(root);
        if (integer(root, "format") != 1) throw new IllegalArgumentException("Unsupported recipe aspect format");
        var items = new LinkedHashMap<String, RecipeAspectData.ItemProperties>();
        for (var entry : root.getAsJsonArray("items")) {
            var row = entry.getAsJsonObject();
            String id = string(row, "item");
            new Key(id, 0);
            var item = new RecipeAspectData.ItemProperties(bool(row, "damageable"), bool(row, "subtypes"), row.has("container") ? string(row, "container") : null);
            if (items.putIfAbsent(id, item) != null) throw new IllegalArgumentException("Duplicate semantic item: " + id);
        }
        var registrations = new ArrayList<RecipeAspectData.Registration>();
        for (var entry : root.getAsJsonArray("registrations")) {
            var row = entry.getAsJsonObject();
            registrations.add(new RecipeAspectData.Registration(key(row), quantities(row.getAsJsonArray("aspects"))));
        }
        var recipes = new ArrayList<RecipeAspectData.Recipe>();
        for (var entry : root.getAsJsonArray("recipes")) {
            var row = entry.getAsJsonObject();
            var inputs = new ArrayList<RecipeAspectData.Ingredient>();
            for (var input : row.getAsJsonArray("ingredients")) {
                var value = input.getAsJsonObject();
                var candidates = new ArrayList<Key>();
                for (var candidate : value.getAsJsonArray("alternatives")) candidates.add(key(candidate.getAsJsonObject()));
                inputs.add(new RecipeAspectData.Ingredient(candidates, bool(value, "choose_nonempty")));
            }
            recipes.add(new RecipeAspectData.Recipe(RecipeAspectData.Kind.valueOf(string(row, "kind")), key(row.getAsJsonObject("output")),
                    integer(row, "count"), inputs, quantities(row.getAsJsonArray("cost"))));
        }
        return new RecipeAspectData(items, registrations, recipes);
    }

    private static JsonObject key(Key key) {
        var row = new JsonObject();
        row.addProperty("item", key.item());
        row.addProperty("metadata", key.metadata());
        return row;
    }
    private static Key key(JsonObject row) { return new Key(string(row, "item"), integer(row, "metadata")); }
    private static JsonArray quantities(Map<String, Integer> quantities) {
        var rows = new JsonArray();
        quantities.forEach((aspect, amount) -> {
            var row = new JsonObject();
            row.addProperty("aspect", aspect);
            row.addProperty("amount", amount);
            rows.add(row);
        });
        return rows;
    }
    private static Map<String, Integer> quantities(JsonArray rows) {
        var values = new LinkedHashMap<String, Integer>();
        for (var entry : rows) {
            var row = entry.getAsJsonObject();
            String tag = string(row, "aspect");
            int amount = integer(row, "amount");
            if (values.putIfAbsent(tag, amount) != null) throw new IllegalArgumentException("Duplicate aspect: " + tag);
        }
        return values;
    }
    private static String string(JsonObject row, String name) {
        var value = row.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Expected string " + name);
        return value.getAsString();
    }
    private static int integer(JsonObject row, String name) {
        var value = row.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected integer " + name);
        return value.getAsBigDecimal().intValueExact();
    }
    private static boolean bool(JsonObject row, String name) {
        var value = row.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Expected boolean " + name);
        return value.getAsBoolean();
    }
}
