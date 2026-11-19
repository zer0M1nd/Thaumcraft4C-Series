package tc4revival.aspect.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public  class ItemAspectJson {
    private ItemAspectJson() {}

    public static JsonObject encode(List<ItemAspectRule> rules) {
        var root = new JsonObject();
        root.addProperty("format", 1);
        var entries = new JsonArray();
        for (var rule : rules) {
            var row = new JsonObject();
            var items = new JsonArray();
            rule.items().forEach(items::add);
            row.add("items", items);
            if (rule.requiredTag() != null) row.addProperty("required_tag", rule.requiredTag());
            if (rule.generated()) row.addProperty("generated",true);
            if (!rule.nbt().isEmpty()) row.add("nbt",rule.nbt().json());
            var aspects = new JsonArray();
            for (var quantity : rule.aspects()) {
                var value = new JsonObject();
                value.addProperty("aspect", quantity.aspect());
                value.addProperty("amount", quantity.amount());
                aspects.add(value);
            }
            row.add("aspects", aspects);
            entries.add(row);
        }
        root.add("entries", entries);
        return root;
    }

    public static List<ItemAspectRule> decode(JsonElement json) {
        return decodePrepared(thaumcraft.api.aspects.ItemAspectProviders.prepareRules(json.getAsJsonObject()));
    }
    private static List<ItemAspectRule> decodePrepared(JsonElement json) {
        var root = json.getAsJsonObject();
        if (integer(root.get("format")) != 1) throw new IllegalArgumentException("Unsupported item aspect format");
        var result = new ArrayList<ItemAspectRule>();
        for (var entry : root.getAsJsonArray("entries")) {
            var row = entry.getAsJsonObject();
            var items = new ArrayList<String>();
            row.getAsJsonArray("items").forEach(item -> items.add(string(item)));
            var aspects = new ArrayList<ItemAspectRule.Quantity>();
            for (var value : row.getAsJsonArray("aspects")) {
                var quantity = value.getAsJsonObject();
                aspects.add(new ItemAspectRule.Quantity(string(quantity.get("aspect")), integer(quantity.get("amount"))));
            }
            result.add(new ItemAspectRule(items, row.has("required_tag") ? string(row.get("required_tag")) : null, aspects,
                    row.has("generated")?row.get("generated").getAsBoolean():root.has("generated")&&root.get("generated").getAsBoolean(),
                    row.has("nbt")?new ItemNbtPredicate(row.getAsJsonObject("nbt")):ItemNbtPredicate.EMPTY));
        }
        return List.copyOf(result);
    }

    private static String string(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Expected a string");
        }
        return value.getAsString();
    }

    /** Optional mods may be absent; retain valid members and quantities from the same rule. */
    public static List<ItemAspectRule> decodeKnown(JsonElement json, tc4revival.aspect.AspectCatalog catalog,
                                                 java.util.function.Predicate<String> exists) {
        return decodeKnown(json,catalog,exists,AspectModConditions::loaded);
    }
    public static List<ItemAspectRule> decodeKnown(JsonElement json, tc4revival.aspect.AspectCatalog catalog,
                                                 java.util.function.Predicate<String> exists,java.util.function.Predicate<String> modLoaded) {
        var result = new ArrayList<ItemAspectRule>();
        var root=thaumcraft.api.aspects.ItemAspectProviders.prepareRules(json.getAsJsonObject());if(!AspectModConditions.matches(root,modLoaded))return List.of();
        var decoded=decodePrepared(root);var entries=root.getAsJsonArray("entries");
        for (int i=0;i<decoded.size();i++) {
            var rule=decoded.get(i);if(!AspectModConditions.matches(entries.get(i).getAsJsonObject(),modLoaded))continue;
            var items = rule.items().stream().filter(id->id.equals("*")||exists.test(id)).toList();
            if (!items.isEmpty()) result.add(new ItemAspectRule(items, rule.requiredTag(),
                    rule.aspects().stream().filter(q -> catalog.find(q.aspect()).isPresent()).toList(), rule.generated(),rule.nbt()));
        }
        return List.copyOf(result);
    }

    private static int integer(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Expected an integer");
        }
        return value.getAsBigDecimal().intValueExact();
    }
}
