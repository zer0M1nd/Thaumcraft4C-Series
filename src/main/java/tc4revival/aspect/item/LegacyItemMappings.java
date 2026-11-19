package tc4revival.aspect.item;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.TC4Revival;
import tc4revival.aspect.recipe.RecipeAspectData.Key;
import tc4revival.aspect.recipe.RecipeAspects;

public  class LegacyItemMappings {
    private static volatile Definition definition;
    private record IdentityCache(Definition definition,tc4revival.aspect.recipe.RecipeAspectData data,LegacyItemIdentity identity) {}
    private static IdentityCache cached;
    private LegacyItemMappings() {}
    public record Definition(Map<String, Key> items, Map<Key, Integer> groups, Map<String, Key> spawnEggs) {
        public Definition { items = Map.copyOf(items); groups = Map.copyOf(groups); spawnEggs = Map.copyOf(spawnEggs); }
    }
    public static void addReloadListener(AddReloadListenerEvent event) { event.addListener(new Reload()); }
    public static synchronized void stopped(ServerStoppedEvent event) { definition = null; cached=null; }
    public static Map<String, Key> spawnEggs() {
        var current = definition;
        if (current == null) throw new IllegalStateException("Legacy item mappings have not loaded");
        return current.spawnEggs();
    }
    public static synchronized LegacyItemIdentity identity() {
        var current = definition;
        if (current == null) throw new IllegalStateException("Legacy item mappings have not loaded");
        var data=RecipeAspects.data();
        if(cached==null||cached.definition()!=current||cached.data()!=data)cached=new IdentityCache(current,data,new LegacyItemIdentity(current.items(),current.groups(),data));
        return cached.identity();
    }
    public static Definition decode(JsonElement json) {
        var root = json.getAsJsonObject();
        if (integer(root.get("format")) != 1) throw new IllegalArgumentException("Unsupported legacy item mapping format");
        var items = new LinkedHashMap<String, Key>();
        for (var value : root.getAsJsonArray("items")) {
            var row = value.getAsJsonObject();
            String id = string(row.get("item"));
            if (!id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+") || id.equals("minecraft:air")) throw new IllegalArgumentException("Invalid modern item ID: " + id);
            if (items.put(id, new Key(string(row.get("legacy_item")), integer(row.get("metadata")))) != null) throw new IllegalArgumentException("Duplicate modern item: " + id);
        }
        var groups = new LinkedHashMap<Key, Integer>();
        for (var value : root.getAsJsonArray("groups")) {
            var row = value.getAsJsonObject();
            var key = new Key(string(row.get("item")), integer(row.get("metadata")));
            var members = row.getAsJsonArray("group");
            if (members.isEmpty()) throw new IllegalArgumentException("Empty scan group");
            if (groups.put(key, integer(members.get(0))) != null) throw new IllegalArgumentException("Duplicate scan group key");
        }
        var eggs = new LinkedHashMap<String, Key>();
        if (root.has("spawn_eggs")) for (var value : root.getAsJsonArray("spawn_eggs")) {
            var row = value.getAsJsonObject();
            String item = string(row.get("item"));
            int metadata = integer(row.get("metadata"));
            if (!item.matches("[a-z0-9_.-]+:[a-z0-9/._-]+_spawn_egg") || metadata < 0 || metadata > 32767 || items.containsKey(item)) throw new IllegalArgumentException("Invalid old spawn egg alias " + item);
            if (eggs.put(item, new Key("minecraft:spawn_egg", metadata)) != null) throw new IllegalArgumentException("Duplicate spawn egg alias " + item);
        }
        return new Definition(items, groups, eggs);
    }
    private static String string(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Expected a string");
        return value.getAsString();
    }
    private static int integer(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected an integer");
        return value.getAsBigDecimal().intValueExact();
    }
    private static  class Reload extends SimpleJsonResourceReloadListener {
        Reload() { super(new Gson(), "legacy_item_mappings"); }
        @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            var items = new LinkedHashMap<String, Key>();
            var groups = new LinkedHashMap<Key, Integer>();
            var eggs = new LinkedHashMap<String, Key>();
            files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                var parsed = decode(entry.getValue());
                parsed.items().forEach((id, key) -> {
                    if (!ForgeRegistries.ITEMS.containsKey(new ResourceLocation(id))) throw new IllegalArgumentException("Unknown modern item: " + id);
                    if (items.put(id, key) != null) throw new IllegalArgumentException("Duplicate item across mapping files: " + id);
                });
                parsed.groups().forEach((key, metadata) -> {
                    if (groups.put(key, metadata) != null) throw new IllegalArgumentException("Duplicate group across mapping files: " + key);
                });
                parsed.spawnEggs().forEach((id, key) -> {
                    if (!(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id)) instanceof net.minecraft.world.item.SpawnEggItem)) throw new IllegalArgumentException("Not a spawn egg item: " + id);
                    if (eggs.put(id, key) != null) throw new IllegalArgumentException("Duplicate spawn egg across mapping files: " + id);
                });
            });
            if (eggs.keySet().stream().anyMatch(items::containsKey)) throw new IllegalArgumentException("Spawn egg also declared as an ordinary alias");
            definition = new Definition(items, groups, eggs);
            tc4revival.aspect.sync.ServerAspectData.capture("legacy_item_mappings",files);
            TC4Revival.LOGGER.info("Loaded {} modern legacy item aliases, {} scan grouping keys and {} old spawn eggs", items.size(), groups.size(), eggs.size());
        }
    }
}
