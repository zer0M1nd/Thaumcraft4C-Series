package tc4revival.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.TC4Revival;
import tc4revival.aspect.item.BlockScanMapping;
import tc4revival.aspect.item.BlockScanMapping.Rule;
import tc4revival.aspect.item.BlockScanMapping.Target;
import tc4revival.aspect.item.BlockScanTargets;
import tc4revival.aspect.item.LegacyItemMappings;
import tc4revival.aspect.recipe.RecipeAspectData.Key;

/** Generates explicit old-content allowlists; runtime picking cannot silently include newer blocks. */
public  class BlockScanProvider implements DataProvider {
    private final PackOutput output;
    public BlockScanProvider(PackOutput output) { this.output = output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        try {
            var aliases = LegacyItemMappings.decode(LegacyItemMappingProvider.definition()).items();
            var rules = new ArrayList<Rule>();
            for (var block : ForgeRegistries.BLOCKS) {
                String id = ForgeRegistries.BLOCKS.getKey(block).toString();
                if (!id.startsWith("minecraft:") || id.equals("minecraft:air")) continue;
                if (id.equals("minecraft:lava_cauldron") || id.equals("minecraft:powder_snow_cauldron")) continue;
                if (id.equals("minecraft:farmland")) {
                    // Forge picks the dirt item, then asks the dirt block for metadata at the farmland position.
                    for (int moisture = 0; moisture < 8; moisture++) {
                        int damage = moisture == 1 ? 0 : moisture;
                        rules.add(new Rule(id, Map.of("moisture", Integer.toString(moisture)),
                                new Target(damage == 0 ? "minecraft:dirt" : damage == 2 ? "minecraft:podzol" : null, new Key("minecraft:dirt", damage))));
                    }
                    continue;
                }
                if (id.equals("minecraft:piston_head")) {
                    for (String type : List.of("normal", "sticky")) {
                        String item = "minecraft:" + (type.equals("sticky") ? "sticky_piston" : "piston");
                        rules.add(new Rule(id, Map.of("type", type), new Target(item, aliases.get(item))));
                    }
                    continue;
                }
                String item = ForgeRegistries.ITEMS.getKey(block.asItem()).toString();
                if (block instanceof FlowerPotBlock pot && !id.equals("minecraft:flower_pot")) {
                    item = ForgeRegistries.ITEMS.getKey(pot.getContent().asItem()).toString();
                }
                item = switch (id) {
                    case "minecraft:coarse_dirt" -> "minecraft:dirt";
                    case "minecraft:smooth_stone" -> "minecraft:smooth_stone_slab";
                    case "minecraft:smooth_sandstone" -> "minecraft:sandstone_slab";
                    case "minecraft:smooth_quartz" -> "minecraft:quartz_slab";
                    case "minecraft:brown_mushroom_block", "minecraft:mushroom_stem" -> "minecraft:brown_mushroom";
                    case "minecraft:red_mushroom_block" -> "minecraft:red_mushroom";
                    case "minecraft:water_cauldron" -> "minecraft:cauldron";
                    case "minecraft:pumpkin_stem", "minecraft:attached_pumpkin_stem" -> "minecraft:pumpkin_seeds";
                    case "minecraft:melon_stem", "minecraft:attached_melon_stem" -> "minecraft:melon_seeds";
                    case "minecraft:cocoa" -> "minecraft:cocoa_beans";
                    case "minecraft:tripwire" -> "minecraft:string";
                    default -> item;
                };
                for (String wood : List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak")) {
                    if (id.equals("minecraft:" + wood + "_wood")) item = "minecraft:" + wood + "_log";
                }
                var key = aliases.get(item);
                if (key != null) {
                    rules.add(new Rule(id, Map.of(), new Target(item, key)));
                } else if (id.equals("minecraft:water") || id.equals("minecraft:lava") || id.equals("minecraft:fire") || id.equals("minecraft:end_portal")) {
                    rules.add(new Rule(id, Map.of(), new Target(null, new Key(id, 0))));
                } else if (id.equals("minecraft:nether_portal")) {
                    rules.add(new Rule(id, Map.of(), new Target(null, new Key("minecraft:portal", 0))));
                } else if (id.equals("minecraft:copper_ore") || id.equals("minecraft:deepslate_copper_ore")) {
                    rules.add(new Rule(id, Map.of(), new Target(item, null)));
                }
            }
            rules.sort(Comparator.comparing(Rule::block).thenComparing(rule -> rule.properties().toString()));
            var mapping = new BlockScanMapping(rules);
            BlockScanTargets.validate(mapping);
            TC4Revival.LOGGER.info("Generated {} old block scan rules", rules.size());
            return DataProvider.saveStable(cache, BlockScanMapping.encode(rules), output.getOutputFolder(PackOutput.Target.DATA_PACK)
                    .resolve("thaumcraft/block_scan_targets/vanilla.json"));
        } catch (IOException failure) { return CompletableFuture.failedFuture(failure); }
    }
    @Override public String getName() { return "TC4 old block scan target identities"; }
}
