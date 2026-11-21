package tc4revival.aspect.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public  class EntityAspectJson {
    private EntityAspectJson() {}

    public static JsonObject encode(List<EntityAspectRule> rules) {
        var root = new JsonObject();
        root.addProperty("format", 1);
        var rows = new JsonArray();
        for (var rule : rules) {
            var row = new JsonObject();
            row.addProperty("entity", rule.entity());
            row.addProperty("legacy_name", rule.legacyName());
            row.addProperty("variant", rule.variant().name().toLowerCase(Locale.ROOT));
            row.addProperty("legacy_qualifier", rule.qualifier());
            if(rule.golemMaterial()!=null)row.addProperty("golem_material",rule.golemMaterial());
            var aspects = new JsonArray();
            for (var quantity : rule.aspects()) {
                var value = new JsonObject();
                value.addProperty("aspect", quantity.aspect());
                value.addProperty("amount", quantity.amount());
                aspects.add(value);
            }
            row.add("aspects", aspects);
            rows.add(row);
        }
        root.add("entries", rows);
        return root;
    }

    public static List<EntityAspectRule> decode(JsonElement json) {
        var root = json.getAsJsonObject();
        if (integer(root, "format") != 1) throw new IllegalArgumentException("Unsupported entity aspect format");
        var result = new ArrayList<EntityAspectRule>();
        for (JsonElement element : root.getAsJsonArray("entries")) {
            var row = element.getAsJsonObject();
            var quantities = new ArrayList<EntityAspectRule.Quantity>();
            for (JsonElement value : row.getAsJsonArray("aspects")) {
                var quantity = value.getAsJsonObject();
                quantities.add(new EntityAspectRule.Quantity(string(quantity, "aspect"), integer(quantity, "amount")));
            }
            result.add(new EntityAspectRule(string(row, "entity"), string(row, "legacy_name"),
                    EntityAspectRule.Variant.valueOf(string(row, "variant").toUpperCase(Locale.ROOT)),
                    string(row, "legacy_qualifier"), quantities,row.has("golem_material")?string(row,"golem_material"):null));
        }
        return List.copyOf(result);
    }

    private static String string(JsonObject object, String key) {
        var value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Expected string " + key);
        }
        return value.getAsString();
    }

    private static int integer(JsonObject object, String key) {
        var value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Expected integer " + key);
        }
        return value.getAsBigDecimal().intValueExact();
    }
}
