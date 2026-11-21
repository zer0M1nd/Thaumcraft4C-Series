package tc4revival.aspect.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import tc4revival.aspect.recipe.RecipeAspectData.Key;

/** A block's old picked identity, independently of its inventory item and block-entity NBT. */
public  class BlockScanMapping {
    public record Target(String modernItem, Key legacyKey) {
        public Target {
            if (modernItem == null && legacyKey == null) throw new IllegalArgumentException("Missing block scan identity");
            if (modernItem != null) validId(modernItem);
        }
    }
    public record Rule(String block, Map<String, String> properties, Target target) {
        public Rule {
            validId(block);
            properties = Map.copyOf(properties);
            if (target == null) throw new IllegalArgumentException("Missing block scan target");
        }
        public boolean matches(Map<String, String> state) {
            return properties.entrySet().stream().allMatch(entry -> entry.getValue().equals(state.get(entry.getKey())));
        }
    }
    private final List<Rule> rules;
    private final Map<String, List<Rule>> byBlock;
    public BlockScanMapping(List<Rule> rules) {
        this.rules = List.copyOf(rules);
        byBlock = rules.stream().collect(java.util.stream.Collectors.groupingBy(Rule::block));
        byBlock.forEach((block, entries) -> {
            for (int i = 0; i < entries.size(); i++) for (int j = i + 1; j < entries.size(); j++) {
                var left = entries.get(i).properties();
                var right = entries.get(j).properties();
                boolean disjoint = left.entrySet().stream().anyMatch(e -> right.containsKey(e.getKey()) && !right.get(e.getKey()).equals(e.getValue()));
                if (!disjoint) throw new IllegalArgumentException("Overlapping block scan rules: " + block);
            }
        });
    }
    public List<Rule> rules() { return rules; }
    public Optional<Target> resolve(String block, Map<String, String> state) {
        return byBlock.getOrDefault(block, List.of()).stream().filter(rule -> rule.matches(state)).map(Rule::target).findFirst();
    }
    public static JsonObject encode(List<Rule> rules) {
        var root = new JsonObject();
        root.addProperty("format", 1);
        var rows = new JsonArray();
        for (var rule : rules) {
            var row = new JsonObject();
            row.addProperty("block", rule.block());
            var properties = new JsonObject();
            rule.properties().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> properties.addProperty(e.getKey(), e.getValue()));
            row.add("properties", properties);
            if (rule.target().modernItem() != null) row.addProperty("item", rule.target().modernItem());
            if (rule.target().legacyKey() != null) {
                row.addProperty("legacy_item", rule.target().legacyKey().item());
                row.addProperty("metadata", rule.target().legacyKey().metadata());
            }
            rows.add(row);
        }
        root.add("rules", rows);
        return root;
    }
    public static BlockScanMapping decode(JsonElement json) {
        var root = json.getAsJsonObject();
        if (number(root.get("format")) != 1) throw new IllegalArgumentException("Unsupported block scan mapping format");
        var rules = new java.util.ArrayList<Rule>();
        for (var value : root.getAsJsonArray("rules")) {
            var row = value.getAsJsonObject();
            var properties = new LinkedHashMap<String, String>();
            row.getAsJsonObject("properties").entrySet().forEach(e -> properties.put(e.getKey(), string(e.getValue())));
            var key = row.has("legacy_item") ? new Key(string(row.get("legacy_item")), number(row.get("metadata"))) : null;
            rules.add(new Rule(string(row.get("block")), properties, new Target(row.has("item") ? string(row.get("item")) : null, key)));
        }
        return new BlockScanMapping(rules);
    }
    private static void validId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+") || id.equals("minecraft:air")) throw new IllegalArgumentException("Invalid block scan resource ID: " + id);
    }
    private static String string(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Expected a string");
        return value.getAsString();
    }
    private static int number(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected an integer");
        return value.getAsBigDecimal().intValueExact();
    }
}
