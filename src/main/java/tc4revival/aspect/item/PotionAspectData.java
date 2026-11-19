package tc4revival.aspect.item;

import java.util.Map;

public record PotionAspectData(Map<Integer, PotionAspectRules.Definition> definitions,
                               Map<Variant, Integer> defaults, Map<Integer, Variant> represented) {
    public record Variant(String item, String potion) {}
    public PotionAspectData {
        definitions = Map.copyOf(definitions);
        defaults = Map.copyOf(defaults);
        represented = Map.copyOf(represented);
        for (var entry : defaults.entrySet()) if (!entry.getKey().equals(represented.get(entry.getValue()))) {
            throw new IllegalArgumentException("Potion default does not match its represented variant");
        }
        for (int metadata : represented.keySet()) if (!definitions.containsKey(metadata)) throw new IllegalArgumentException("Missing potion definition " + metadata);
    }
    public Integer metadata(Variant variant, Integer preservedMetadata) {
        return preservedMetadata != null && variant.equals(represented.get(preservedMetadata))
                ? preservedMetadata : defaults.get(variant);
    }
}
