package tbmodern.api;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Named, extensible revolver upgrades. Metadata is loaded once, never stored as array indexes. */
public final class RevolverUpgrades {
    public record Definition(String key, String name, int maximum, int instability, Set<String> conflicts) {
        public Definition {
            ResourceLocation.parse(key); Objects.requireNonNull(name);
            if (maximum < 1 || maximum > 32 || instability < 0 || instability > 1000) throw new IllegalArgumentException("Invalid upgrade limits: " + key);
            conflicts = Set.copyOf(conflicts);
            if (conflicts.contains(key)) throw new IllegalArgumentException("Self-conflicting upgrade: " + key);
        }
    }
    private static final Map<String, Definition> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<String, RevolverBehavior> BEHAVIORS = new LinkedHashMap<>();
    private static final Map<ResourceLocation,java.util.function.Predicate<net.minecraft.world.entity.LivingEntity>> SILVER_TARGETS = new LinkedHashMap<>();
    public static final String NBT = "tbRevolverUpgrades";
    static {
        try (var stream = RevolverUpgrades.class.getResourceAsStream("/tbmodern/revolver_upgrades.json")) {
            if (stream == null) throw new IOException("Missing revolver definitions");
            for (var element : JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray()) {
                var row = element.getAsJsonObject(); Set<String> conflicts = new LinkedHashSet<>();
                row.getAsJsonArray("conflicts").forEach(value -> conflicts.add(value.getAsString()));
                register(new Definition(row.get("key").getAsString(),row.get("name").getAsString(),row.get("maximum").getAsInt(),row.get("instability").getAsInt(),conflicts));
            }
            for (var definition : DEFINITIONS.values()) for (String conflict : definition.conflicts())
                if (!DEFINITIONS.containsKey(conflict)) throw new IllegalArgumentException("Unknown conflicting upgrade: " + conflict);
        } catch (IOException exception) { throw new ExceptionInInitializerError(exception); }
    }
    /** Register during common setup on both sides. Conflicts are checked symmetrically. */
    public static synchronized void register(Definition definition) {
        if (DEFINITIONS.putIfAbsent(definition.key(),definition) != null) throw new IllegalArgumentException("Duplicate revolver upgrade: " + definition.key());
    }
    public static Collection<Definition> definitions() { return List.copyOf(DEFINITIONS.values()); }
    public static synchronized void registerBehavior(String key, RevolverBehavior behavior) {
        require(key); Objects.requireNonNull(behavior);
        if(BEHAVIORS.putIfAbsent(key,behavior)!=null)throw new IllegalArgumentException("Duplicate revolver behavior: "+key);
    }
    public record ActiveBehavior(RevolverBehavior behavior, int level) {}
    public static List<ActiveBehavior> behaviors(ItemStack gun) {
        var levels=levels(gun); List<ActiveBehavior> active=new ArrayList<>();
        BEHAVIORS.forEach((key,behavior)->{int level=levels.getOrDefault(key,0);if(level>0)active.add(new ActiveBehavior(behavior,level));});
        return List.copyOf(active);
    }
    /** Dynamic adapters can recognize transformed players; static entities can use the silver_vulnerable entity tag. */
    public static synchronized void registerSilverTarget(ResourceLocation key, java.util.function.Predicate<net.minecraft.world.entity.LivingEntity> predicate) {
        Objects.requireNonNull(key);Objects.requireNonNull(predicate);
        if(SILVER_TARGETS.putIfAbsent(key,predicate)!=null)throw new IllegalArgumentException("Duplicate silver target adapter: "+key);
    }
    public static boolean silverVulnerable(net.minecraft.world.entity.LivingEntity target) {
        return target.getMobType()==net.minecraft.world.entity.MobType.UNDEAD
            || target.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,new ResourceLocation("thaumicbases","silver_vulnerable")))
            || SILVER_TARGETS.values().stream().anyMatch(test->test.test(target));
    }
    public static Definition require(String key) {
        var result = DEFINITIONS.get(key); if (result == null) throw new IllegalArgumentException("Unknown revolver upgrade: " + key); return result;
    }
    public static String nbtKey(String key) {require(key);return "tbUpgrade:"+key;}
    public static Map<String,Integer> levels(ItemStack stack) {
        Map<String,Integer> result = new LinkedHashMap<>();
        if (!stack.hasTag()) return result;
        CompoundTag saved = stack.getTag().getCompound(NBT);
        for (var definition : definitions()) {
            String key = nbtKey(definition.key());
            int raw = stack.getTag().contains(key) ? stack.getTag().getInt(key) : saved.getInt(definition.key());
            int level = Math.min(definition.maximum(),Math.max(0,raw));
            if (level > 0) result.put(definition.key(),level);
        }
        return Collections.unmodifiableMap(result);
    }
    public static int level(ItemStack stack, String name) {
        String key = name.contains(":") ? name : "thaumicbases:" + name;
        return levels(stack).getOrDefault(key,0);
    }
    public static boolean canUpgrade(ItemStack stack, String key) {
        Definition target = require(key); var existing = levels(stack);
        if (existing.getOrDefault(key,0) >= target.maximum()) return false;
        for (String other : existing.keySet()) if (target.conflicts().contains(other) || require(other).conflicts().contains(key)) return false;
        return true;
    }
    public static boolean upgrade(ItemStack stack, String key) {
        if (!canUpgrade(stack,key)) return false;
        stack.getOrCreateTag().putInt(nbtKey(key),level(stack,key)+1); return true;
    }
    public static int shotsPerEssentia(ItemStack stack) {
        int efficient = level(stack,"efficiency");
        int shots=level(stack,"primal") > 0 ? efficient == 5 ? 1 : -1 : 2 + efficient;
        for(var active:behaviors(stack))shots=active.behavior().shotsPerEssentia(stack,active.level(),shots);
        if(shots==-1)return -1;
        return Math.max(1,Math.min(1024,shots));
    }
    public static double speedMultiplier(ItemStack stack) {
        double speed=Math.pow(1.09F,level(stack,"speed"));
        for(var active:behaviors(stack))speed=active.behavior().speedMultiplier(stack,active.level(),speed);
        return Double.isFinite(speed)?Math.max(.01,Math.min(16,speed)):1;
    }
    private RevolverUpgrades() {}
}
