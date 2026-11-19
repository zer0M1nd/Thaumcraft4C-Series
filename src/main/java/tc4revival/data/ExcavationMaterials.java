package tc4revival.data;

/** Observed old material groups, expanded through the reviewed vanilla state/metadata aliases.
 * Source: docs/validation/2026-09-11-legacy-excavation-materials.json.
 * Lit furnace/ore and double slabs collapse into their modern block states. */
public  class ExcavationMaterials {
    private ExcavationMaterials() {}
    public static net.minecraft.world.level.block.Block[] vanilla() {
        return IDS.strip().split("\\s+").length == 0 ? new net.minecraft.world.level.block.Block[0] :
            java.util.Arrays.stream(IDS.strip().split("\\s+")).map(id -> java.util.Objects.requireNonNull(
                net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.parse(id)),id))
                .toArray(net.minecraft.world.level.block.Block[]::new);
    }
    private static final String IDS = """
minecraft:bedrock
minecraft:black_terracotta
minecraft:blue_terracotta
minecraft:brick_slab
minecraft:brick_stairs
minecraft:bricks
minecraft:brown_terracotta
minecraft:chiseled_quartz_block
minecraft:chiseled_sandstone
minecraft:chiseled_stone_bricks
minecraft:coal_block
minecraft:coal_ore
minecraft:coarse_dirt
minecraft:cobblestone
minecraft:cobblestone_slab
minecraft:cobblestone_stairs
minecraft:cobblestone_wall
minecraft:cracked_stone_bricks
minecraft:cut_sandstone
minecraft:cyan_terracotta
minecraft:diamond_ore
minecraft:dirt
minecraft:dispenser
minecraft:dropper
minecraft:emerald_ore
minecraft:enchanting_table
minecraft:end_portal_frame
minecraft:end_stone
minecraft:ender_chest
minecraft:farmland
minecraft:furnace
minecraft:gold_ore
minecraft:grass_block
minecraft:gravel
minecraft:gray_terracotta
minecraft:green_terracotta
minecraft:hay_block
minecraft:iron_ore
minecraft:lapis_ore
minecraft:light_blue_terracotta
minecraft:light_gray_terracotta
minecraft:lime_terracotta
minecraft:magenta_terracotta
minecraft:mossy_cobblestone
minecraft:mossy_cobblestone_wall
minecraft:mossy_stone_bricks
minecraft:mycelium
minecraft:nether_brick_fence
minecraft:nether_brick_slab
minecraft:nether_brick_stairs
minecraft:nether_bricks
minecraft:nether_quartz_ore
minecraft:netherrack
minecraft:obsidian
minecraft:orange_terracotta
minecraft:petrified_oak_slab
minecraft:pink_terracotta
minecraft:podzol
minecraft:purple_terracotta
minecraft:quartz_block
minecraft:quartz_pillar
minecraft:quartz_slab
minecraft:quartz_stairs
minecraft:red_sand
minecraft:red_terracotta
minecraft:redstone_ore
minecraft:sand
minecraft:sandstone
minecraft:sandstone_slab
minecraft:sandstone_stairs
minecraft:smooth_quartz
minecraft:smooth_sandstone
minecraft:smooth_stone
minecraft:smooth_stone_slab
minecraft:soul_sand
minecraft:spawner
minecraft:stone
minecraft:stone_brick_slab
minecraft:stone_brick_stairs
minecraft:stone_bricks
minecraft:stone_pressure_plate
minecraft:terracotta
minecraft:white_terracotta
minecraft:yellow_terracotta
""";
}