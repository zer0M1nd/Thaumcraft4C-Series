package tc4revival.aspect.item;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.TC4Revival;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.recipe.RecipeAspects;

/** State-only vanilla scan selection. TC nodes and other TC blocks will supply their own targets. */
public  class BlockScanTargets {
    private static volatile BlockScanMapping mapping;
    private BlockScanTargets() {}
    public static void addReloadListener(AddReloadListenerEvent event) { event.addListener(new Reload()); }
    public static void stopped(ServerStoppedEvent event) { mapping = null; }
    public static BlockScanMapping mapping() {
        var current = mapping;
        if (current == null) throw new IllegalStateException("Block scan mappings have not loaded");
        return current;
    }
    public static Optional<BlockScanMapping.Target> target(BlockState state) {
        return mapping().resolve(ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString(), properties(state));
    }
    public static Map<String, String> properties(BlockState state) {
        var result = new LinkedHashMap<String, String>();
        for (var property : state.getProperties()) result.put(property.getName(), value(state, property));
        return result;
    }
    private static <T extends Comparable<T>> String value(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
    public static AspectAmounts aspects(BlockScanMapping.Target target) {
        if (target.modernItem() != null) {
            // Old ScanResult discards picked NBT and count; never clone a container's contents or enchantments.
            return ItemStackAspects.resolve(new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(target.modernItem()))))
                    .orElseGet(AspectAmounts::new);
        }
        var identity = LegacyItemMappings.identity();
        return new StackAspectBonuses(TCAspects.catalog()).apply(
                RecipeAspects.object(identity.scanKey(target.legacyKey())), null, null, java.util.List.of());
    }
    public static int hash(BlockScanMapping.Target target) {
        return target.legacyKey() == null ? (target.modernItem() + ":-1").hashCode()
                : LegacyItemMappings.identity().scanHash(target.legacyKey());
    }
    public static void validate(BlockScanMapping candidate) {
        for (var rule : candidate.rules()) {
            var id = new ResourceLocation(rule.block());
            if (!ForgeRegistries.BLOCKS.containsKey(id)) throw new IllegalArgumentException("Unknown scan block " + id);
            var definition = ForgeRegistries.BLOCKS.getValue(id).getStateDefinition();
            rule.properties().forEach((name, expected) -> {
                var property = definition.getProperty(name);
                if (property == null || property.getValue(expected).isEmpty()) throw new IllegalArgumentException("Invalid scan property " + id + " " + name + "=" + expected);
            });
            if (rule.target().modernItem() != null && !ForgeRegistries.ITEMS.containsKey(new ResourceLocation(rule.target().modernItem()))) throw new IllegalArgumentException("Unknown picked item " + rule.target().modernItem());
        }
    }
    /** Reject bad registry references per rule, without discarding unrelated datapack entries. */
    public static BlockScanMapping validatedMapping(Map<ResourceLocation, JsonElement> files) {
        var rules = new ArrayList<BlockScanMapping.Rule>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            for (var rule : BlockScanMapping.decode(entry.getValue()).rules()) {
                try {
                    validate(new BlockScanMapping(java.util.List.of(rule)));
                    rules.add(rule);
                } catch (IllegalArgumentException invalid) {
                    TC4Revival.LOGGER.warn("Skipping invalid block scan rule in {} for {}: {}",
                            entry.getKey(), rule.block(), invalid.getMessage());
                }
            }
        });
        return new BlockScanMapping(rules);
    }
    private static class Reload extends SimpleJsonResourceReloadListener {
        Reload() { super(new Gson(), "block_scan_targets"); }
        @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            var candidate = validatedMapping(files);
            mapping = candidate;
            tc4revival.aspect.sync.ServerAspectData.capture("block_scan_targets",files);
            TC4Revival.LOGGER.info("Loaded {} block scan identity rules", candidate.rules().size());
        }
    }
}
