package tc4revival.aspect.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public  class PotionAspectJson {
    private PotionAspectJson() {}
    public static Map<Integer, PotionAspectRules.Definition> definitions(JsonArray rows) {
        var result = new LinkedHashMap<Integer, PotionAspectRules.Definition>();
        for (var value : rows) {
            var row = value.getAsJsonObject();
            var effects = new ArrayList<PotionAspectRules.Effect>();
            for (var item : row.getAsJsonArray("effects")) {
                var effect = item.getAsJsonObject();
                effects.add(new PotionAspectRules.Effect(integer(effect.get("effect")), integer(effect.get("amplifier")), integer(effect.get("duration"))));
            }
            int metadata = integer(row.get("metadata"));
            if (metadata < 0 || metadata > 32767) throw new IllegalArgumentException("Invalid old potion metadata");
            if (result.put(metadata, new PotionAspectRules.Definition(bool(row.get("splash")), bool(row.get("has_effect_list")), effects)) != null) throw new IllegalArgumentException("Duplicate potion metadata");
        }
        return Map.copyOf(result);
    }
    public static JsonObject encode(PotionAspectData data) {
        var root = new JsonObject();
        root.addProperty("format", 1);
        var definitions = new JsonArray();
        data.definitions().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            var row = new JsonObject();
            row.addProperty("metadata", entry.getKey());
            row.addProperty("splash", entry.getValue().splash());
            row.addProperty("has_effect_list", entry.getValue().hasEffectList());
            var effects = new JsonArray();
            for (var effect : entry.getValue().effects()) {
                var value = new JsonObject();
                value.addProperty("effect", effect.id());
                value.addProperty("amplifier", effect.amplifier());
                value.addProperty("duration", effect.duration());
                effects.add(value);
            }
            row.add("effects", effects);
            definitions.add(row);
        });
        root.add("definitions", definitions);
        var represented = new JsonArray();
        data.represented().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            var row = new JsonObject();
            row.addProperty("metadata", entry.getKey());
            row.addProperty("item", entry.getValue().item());
            row.addProperty("potion", entry.getValue().potion());
            row.addProperty("default", entry.getKey().equals(data.defaults().get(entry.getValue())));
            represented.add(row);
        });
        root.add("represented", represented);
        return root;
    }
    public static PotionAspectData decode(JsonElement json) {
        var root = json.getAsJsonObject();
        if (integer(root.get("format")) != 1) throw new IllegalArgumentException("Unsupported potion aspect format");
        var represented = new LinkedHashMap<Integer, PotionAspectData.Variant>();
        var defaults = new LinkedHashMap<PotionAspectData.Variant, Integer>();
        for (var value : root.getAsJsonArray("represented")) {
            var row = value.getAsJsonObject();
            int metadata = integer(row.get("metadata"));
            var variant = new PotionAspectData.Variant(string(row.get("item")), string(row.get("potion")));
            if (represented.put(metadata, variant) != null) throw new IllegalArgumentException("Duplicate represented potion metadata");
            if (bool(row.get("default")) && defaults.put(variant, metadata) != null) throw new IllegalArgumentException("Duplicate default potion variant");
        }
        return new PotionAspectData(definitions(root.getAsJsonArray("definitions")), defaults, represented);
    }
    private static int integer(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected integer");
        return value.getAsBigDecimal().intValueExact();
    }
    private static String string(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Expected string");
        return value.getAsString();
    }
    private static boolean bool(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Expected boolean");
        return value.getAsBoolean();
    }
}
