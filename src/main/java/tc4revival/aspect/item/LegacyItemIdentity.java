package tc4revival.aspect.item;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import tc4revival.aspect.recipe.RecipeAspectData;
import tc4revival.aspect.recipe.RecipeAspectData.Key;

/** Old identity survives flattening: scan grouping and recipe metadata have different meanings. */
public  class LegacyItemIdentity {
    private final Map<String, Key> items;
    private final Map<Key, Integer> groups;
    private final RecipeAspectData data;
    private final Set<Key> registered;

    public LegacyItemIdentity(Map<String, Key> items, Map<Key, Integer> groups, RecipeAspectData data) {
        this.items = Map.copyOf(items);
        this.groups = Map.copyOf(groups);
        this.data = data;
        registered = data.registrations().stream().map(RecipeAspectData.Registration::key).collect(Collectors.toUnmodifiableSet());
        for (var key : items.values()) if (!data.items().containsKey(key.item())) throw new IllegalArgumentException("Unknown legacy item: " + key.item());
    }

    public LegacyItemIdentity withData(RecipeAspectData next) { return new LegacyItemIdentity(items,groups,next); }
    public Map<String, Key> items() { return items; }
    public Key recipeKey(String modernId) {
        var mapped=items.get(modernId);
        return mapped!=null?mapped:data.items().containsKey(modernId)?new Key(modernId,0):null;
    }
    public Key stackKey(String modernId, Integer mapId, Map<String, Key> spawnEggs) {
        if (modernId.equals("minecraft:filled_map")) return new Key(modernId, mapId == null ? 0 : Math.max(0, mapId));
        return spawnEggs.getOrDefault(modernId, recipeKey(modernId));
    }
    public Key scanKey(Key key) { return new Key(key.item(), groups.getOrDefault(key, key.metadata())); }
    public Key canonicalKey(Key key) {
        var properties = data.items().get(key.item());
        return properties.damageable() || !properties.hasSubtypes() ? new Key(key.item(), RecipeAspectData.WILDCARD) : key;
    }

    public int scanHash(Key original) {
        var properties = data.items().get(original.item());
        int metadata = properties.damageable() || !properties.hasSubtypes() ? -1 : original.metadata();
        metadata = groups.getOrDefault(new Key(original.item(), metadata), metadata);
        if (metadata == -1 && !registered.contains(new Key(original.item(), -1))) {
            for (int candidate = 0; candidate < 16; candidate++) {
                if (registered.contains(new Key(original.item(), candidate))) {
                    // Persisted scan identities must remain stable across upgrades.
                    metadata = candidate + 1;
                    break;
                }
            }
        }
        return (original.item() + ":" + metadata).hashCode();
    }
}
