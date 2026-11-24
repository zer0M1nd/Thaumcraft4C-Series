package tc4revival.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import tc4revival.TC4Revival;
import tc4revival.content.TCTables;

public  class TableBlockTags extends BlockTagsProvider {
    public TableBlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
        super(output, lookup, TC4Revival.MOD_ID, helper);
    }
    @Override protected void addTags(HolderLookup.Provider lookup) {
        tag(tc4revival.content.WandBookCreation.BOOKSHELVES).add(net.minecraft.world.level.block.Blocks.BOOKSHELF).addTag(net.minecraftforge.common.Tags.Blocks.BOOKSHELVES);
        tag(BlockTags.MINEABLE_WITH_AXE).add(tc4revival.content.TCArcane.MAGIC_BOX.get());
        tag(tc4revival.content.ArcaneEarListening.WOOD).add(tc4revival.content.TCArcane.MAGIC_BOX.get());
        noteMaterials();
        tag(tc4revival.content.ExcavationFocusItem.FAST_MATERIAL).add(ExcavationMaterials.vanilla())
                .addTag(tc4revival.content.ArcaneEarListening.ROCK);
        for(int ore=0;ore<8;ore++)for(var block:java.util.List.of(tc4revival.content.TCArcane.ore(ore),tc4revival.content.TCArcane.deepslateOre(ore))){tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);if(ore==0||ore==7)tag(BlockTags.NEEDS_IRON_TOOL).add(block);tag(BlockTags.create(net.minecraft.resources.ResourceLocation.parse(tc4revival.content.LegacyOreBlock.tag(ore)))).add(block);tag(BlockTags.create(net.minecraft.resources.ResourceLocation.parse("forge:ores"))).add(block);}
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(tc4revival.content.TCArcane.OBSIDIAN_TOTEM.get(),tc4revival.content.TCArcane.OBSIDIAN_NODE_TOTEM.get(),tc4revival.content.TCArcane.METAL_GRATE.get(),tc4revival.content.TCArcane.TRAVEL_PAVING.get(),tc4revival.content.TCArcane.WARDING_PAVING.get());
        tag(BlockTags.REPLACEABLE_BY_TREES).add(tc4revival.content.TCArcane.WARDING_BARRIER.get());
        var cosmetic=tag(tc4revival.content.WardingBarrierBlock.LEGACY_COSMETIC);
        for(var rule:TableScanProvider.rules()) if(rule.target().legacyKey()!=null&&rule.target().legacyKey().item().equals("Thaumcraft:blockCosmeticSolid")) cosmetic.add(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.parse(rule.block())));
        for(var entry:EldritchIdentities.all()) if(entry.block()&&entry.legacy().equals("blockCosmeticSolid")) cosmetic.add(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:"+entry.name())));
        cosmetic.add(tc4revival.content.TCCleansing.TALLOW_BLOCK.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(tc4revival.content.TCArcane.THAUMIUM_BLOCK.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(tc4revival.content.TCArcane.GOLEM_FETTER.get());
        tag(BlockTags.BEACON_BASE_BLOCKS).add(tc4revival.content.TCArcane.THAUMIUM_BLOCK.get(),tc4revival.content.TCArcane.GOLEM_FETTER.get());
        tag(BlockTags.create(net.minecraft.resources.ResourceLocation.parse("forge:storage_blocks/thaumium"))).add(tc4revival.content.TCArcane.THAUMIUM_BLOCK.get());
        tag(BlockTags.create(net.minecraft.resources.ResourceLocation.parse("forge:storage_blocks"))).add(tc4revival.content.TCArcane.THAUMIUM_BLOCK.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(tc4revival.content.TCCleansing.TALLOW_BLOCK.get());
        for (var entry:EldritchIdentities.all()) if (entry.block() && !entry.name().startsWith("loot_")) {
            var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:"+entry.name()));
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);
            if (entry.legacy().equals("blockEldritch")) tag(tc4revival.content.PortableHoleFocusItem.BLACKLIST).add(block);
        }
        tag(BlockTags.SLABS).add(tc4revival.content.TCEldritch.SLAB.get()); tag(BlockTags.STAIRS).add(tc4revival.content.TCEldritch.STAIRS.get());
        stabilizers().forEach(block->tag(tc4revival.content.InfusionLayout.STABILIZERS).add(block));
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(tc4revival.content.TCArcane.INFERNAL_FURNACE.get(),tc4revival.content.TCArcane.ADVANCED_ALCHEMY_FURNACE.get(),tc4revival.content.TCArcane.ADVANCED_ALCHEMICAL_CONSTRUCT.get(),tc4revival.content.TCArcane.ALCHEMICAL_CONSTRUCT.get(),tc4revival.content.TCArcane.MNEMONIC_MATRIX.get(),tc4revival.content.TCArcane.CHAOS_MATRIX.get(),tc4revival.content.TCArcane.THAUMIUM_CHEST.get(),tc4revival.content.TCArcane.VOID_CHEST.get(),tc4revival.content.TCArcane.THAUMATORIUM.get());
        tag(tc4revival.content.PortableHoleFocusItem.BLACKLIST).add(tc4revival.content.TCArcane.INFERNAL_FURNACE.get());
        for (String name:tc4revival.content.TCArcane.woodworkNames()) {
            var block=tc4revival.content.TCArcane.woodwork(name); tag(BlockTags.MINEABLE_WITH_AXE).add(block);
            tag(name.endsWith("planks") ? BlockTags.PLANKS : name.endsWith("slab") ? BlockTags.WOODEN_SLABS : BlockTags.WOODEN_STAIRS).add(block);
        }
        tag(BlockTags.LOGS).add(tc4revival.content.TCArcane.GREATWOOD_LOG.get(),tc4revival.content.TCArcane.SILVERWOOD_LOG.get(),tc4revival.content.TCArcane.SILVERWOOD_NODE_LOG.get());
        tag(BlockTags.LOGS_THAT_BURN).add(tc4revival.content.TCArcane.GREATWOOD_LOG.get(),tc4revival.content.TCArcane.SILVERWOOD_LOG.get(),tc4revival.content.TCArcane.SILVERWOOD_NODE_LOG.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(tc4revival.content.TCArcane.GREATWOOD_LOG.get(),tc4revival.content.TCArcane.SILVERWOOD_LOG.get(),tc4revival.content.TCArcane.SILVERWOOD_NODE_LOG.get());
        tag(BlockTags.LEAVES).add(tc4revival.content.TCArcane.GREATWOOD_LEAVES.get(),tc4revival.content.TCArcane.SILVERWOOD_LEAVES.get());
        tag(BlockTags.SAPLINGS).add(tc4revival.content.TCArcane.GREATWOOD_SAPLING.get(),tc4revival.content.TCArcane.SILVERWOOD_SAPLING.get());
        tag(tc4revival.content.CrucibleBlock.BELLOWS).add(tc4revival.content.TCArcane.BELLOWS.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(tc4revival.content.TCArcane.BELLOWS.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(tc4revival.content.TCArcane.LEVITATOR.get(),tc4revival.content.TCArcane.ARCANE_EAR.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(tc4revival.content.TCArcane.ARCANE_BORE_BASE.get(),tc4revival.content.TCArcane.ARCANE_BORE.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(tc4revival.content.TCArcane.ARCANE_LAMP.get(),tc4revival.content.TCArcane.GROWTH_LAMP.get(),tc4revival.content.TCArcane.FERTILITY_LAMP.get(),tc4revival.content.TCArcane.AMBER_BLOCK.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(tc4revival.content.TCArcane.CRUCIBLE.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(tc4revival.content.TCArcane.ALEMBIC.get(),tc4revival.content.TCArcane.ALCHEMY_FURNACE.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                tc4revival.content.TCArcane.ALCHEMY_CENTRIFUGE.get(),
                tc4revival.content.TCArcane.ESSENTIA_CRYSTALLIZER.get(),
                tc4revival.content.TCArcane.ESSENTIA_RESERVOIR.get(),
                tc4revival.content.TCArcane.ARCANE_SPA.get(),
                tc4revival.content.TCArcane.FLUX_SCRUBBER.get(),
                tc4revival.content.TCArcane.ARCANE_DOOR.get(),
                tc4revival.content.TCArcane.WARDED_GLASS.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(
                tc4revival.content.TCArcane.ARCANE_PRESSURE_PLATE.get(),
                tc4revival.content.TCArcane.DECONSTRUCTION_TABLE.get(),
                tc4revival.content.TCArcane.HUNGRY_CHEST.get());
        for (int rarity = 0; rarity < 3; rarity++) {
            tag(BlockTags.MINEABLE_WITH_AXE).add(tc4revival.content.TCEldritch.loot(true, rarity));
        }
        tc4revival.content.TCArcane.essentiaTubes().forEach(block->tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block));
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(tc4revival.content.TCArcane.ARCANE_PEDESTAL.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(tc4revival.content.TCArcane.INFUSION_MATRIX.get(),tc4revival.content.TCArcane.INFUSION_PILLAR.get(),tc4revival.content.TCArcane.INFUSION_PILLAR_TOP.get());
        for(var color:net.minecraft.world.item.DyeColor.values())
            tag(tc4revival.content.InfusionLayout.STABILIZERS).addOptional(net.minecraft.resources.ResourceLocation.parse("botania:"+color.getName()+"_shiny_flower"));
        tag(tc4revival.content.PortableHoleFocusItem.BLACKLIST).addTag(BlockTags.BEDS).addTag(BlockTags.DOORS)
                .add(net.minecraft.world.level.block.Blocks.PISTON, net.minecraft.world.level.block.Blocks.STICKY_PISTON,
                        net.minecraft.world.level.block.Blocks.PISTON_HEAD, net.minecraft.world.level.block.Blocks.MOVING_PISTON)
                .addOptional(net.minecraft.resources.ResourceLocation.parse("thaumcraft:eldritch"))
                .addOptional(net.minecraft.resources.ResourceLocation.parse("thaumcraft:eldritch_portal"))
                .addOptional(net.minecraft.resources.ResourceLocation.parse("thaumcraft:flux_goo"))
                .addOptional(net.minecraft.resources.ResourceLocation.parse("thaumcraft:flux_gas"))
                .addOptional(net.minecraft.resources.ResourceLocation.parse("thaumcraft:arcane_furnace"));
        // Removing one giant-block cell destroys the other 63 cells. A portable
        // hole can restore only its own cell, so these structures cannot be phased.
        for(String giant:java.util.List.of("giant_obsidian","giant_cobblestone","giant_log","giant_leaves"))
            tag(tc4revival.content.PortableHoleFocusItem.BLACKLIST)
                    .addOptional(net.minecraft.resources.ResourceLocation.parse("twilightforest:"+giant));
        tag(BlockTags.MINEABLE_WITH_AXE).add(TCTables.TABLE.get(), tc4revival.content.TCArcane.BLOCK.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(tc4revival.content.TCArcane.VIS_RELAY.get(), tc4revival.content.TCArcane.WORKBENCH_CHARGER.get(),
                tc4revival.content.TCArcane.WAND_PEDESTAL.get(), tc4revival.content.TCArcane.WAND_PEDESTAL_FOCUS.get(),
                tc4revival.content.TCArcane.NODE_STABILIZER.get(), tc4revival.content.TCArcane.ADVANCED_NODE_STABILIZER.get(), tc4revival.content.TCArcane.NODE_CONVERTER.get(),
                tc4revival.content.TCArcane.FOCAL_BLOCK.get(), tc4revival.content.TCArcane.ARCANE_STONE.get(),
                tc4revival.content.TCArcane.ARCANE_STONE_BRICKS.get(), tc4revival.content.TCArcane.ARCANE_STONE_SLAB.get(), tc4revival.content.TCArcane.ARCANE_STONE_STAIRS.get());
        tag(BlockTags.SLABS).add(tc4revival.content.TCArcane.ARCANE_STONE_SLAB.get());
        tag(BlockTags.STAIRS).add(tc4revival.content.TCArcane.ARCANE_STONE_STAIRS.get());
    }
    static java.util.List<net.minecraft.world.level.block.Block> stabilizers() {
        var blocks=new java.util.ArrayList<net.minecraft.world.level.block.Block>();
        blocks.add(tc4revival.content.TCEldritch.CRYSTAL.get());
        for(int i=0;i<16;i++)blocks.add(tc4revival.content.TCArcane.candle(i));
        for(var type:tc4revival.content.ShardType.values())blocks.add(tc4revival.content.TCArcane.crystal(type));
        blocks.addAll(java.util.List.of(net.minecraft.world.level.block.Blocks.SKELETON_SKULL,net.minecraft.world.level.block.Blocks.SKELETON_WALL_SKULL,
                net.minecraft.world.level.block.Blocks.WITHER_SKELETON_SKULL,net.minecraft.world.level.block.Blocks.WITHER_SKELETON_WALL_SKULL,
                net.minecraft.world.level.block.Blocks.ZOMBIE_HEAD,net.minecraft.world.level.block.Blocks.ZOMBIE_WALL_HEAD,
                net.minecraft.world.level.block.Blocks.PLAYER_HEAD,net.minecraft.world.level.block.Blocks.PLAYER_WALL_HEAD,
                net.minecraft.world.level.block.Blocks.CREEPER_HEAD,net.minecraft.world.level.block.Blocks.CREEPER_WALL_HEAD));
        return java.util.List.copyOf(blocks);
    }
    /** Old block constructors, not step sounds or mining tags, determine note instruments. */
    private void noteMaterials() {
        var rock=tag(tc4revival.content.ArcaneEarListening.ROCK);
        var glass=tag(tc4revival.content.ArcaneEarListening.GLASS);
        var wood=tag(tc4revival.content.ArcaneEarListening.WOOD);
        var seen=new java.util.HashSet<String>();
        for (var rule:TableScanProvider.rules()) {
            var key=rule.target().legacyKey();
            if (key==null || !seen.add(rule.block())) continue;
            var destination=switch (key.item()) {
                case "Thaumcraft:blockArcaneFurnace", "Thaumcraft:blockCosmeticSolid", "Thaumcraft:blockCosmeticOpaque",
                     "Thaumcraft:blockCosmeticSlabStone", "Thaumcraft:blockCustomOre", "Thaumcraft:blockEldritch",
                     "Thaumcraft:blockStoneDevice", "Thaumcraft:blockStairsArcaneStone", "Thaumcraft:blockStairsEldritch" -> rock;
                case "Thaumcraft:blockCrystal", "Thaumcraft:blockJar", "Thaumcraft:blockMirror" -> glass;
                case "Thaumcraft:blockTable", "Thaumcraft:blockWoodenDevice", "Thaumcraft:blockChestHungry",
                     "Thaumcraft:blockLootCrate" -> wood;
                default -> null;
            };
            if (destination!=null) destination.add(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                    net.minecraft.resources.ResourceLocation.parse(rule.block())));
        }
        // Internal blocks and wood/crystals whose scan mappings live outside TableScanProvider.
        rock.add(tc4revival.content.TCArcane.AMBER_BLOCK.get(),tc4revival.content.TCArcane.INFUSION_PILLAR.get(),
                tc4revival.content.TCArcane.INFUSION_PILLAR_TOP.get(),tc4revival.content.TCArcane.WARDED.get(),
                tc4revival.content.TCArcane.PORTABLE_HOLE.get(),tc4revival.content.TCEldritch.VOID.get());
        glass.add(tc4revival.content.TCArcane.NODE_JAR.get());
        for (var shard:tc4revival.content.ShardType.values()) glass.add(tc4revival.content.TCArcane.crystal(shard));
        wood.add(tc4revival.content.TCArcane.LEVITATOR.get(),tc4revival.content.TCArcane.ARCANE_EAR.get(),
                tc4revival.content.TCArcane.ARCANE_BORE.get(),tc4revival.content.TCArcane.ARCANE_BORE_BASE.get(),
                tc4revival.content.TCArcane.GREATWOOD_LOG.get(),tc4revival.content.TCArcane.SILVERWOOD_LOG.get(),
                tc4revival.content.TCArcane.SILVERWOOD_NODE_LOG.get());
        for (String name:tc4revival.content.TCArcane.woodworkNames()) wood.add(tc4revival.content.TCArcane.woodwork(name));
    }

}
