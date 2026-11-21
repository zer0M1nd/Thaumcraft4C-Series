package tc4revival.data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import tc4revival.aspect.item.BlockScanMapping;
import tc4revival.aspect.recipe.RecipeAspectData.Key;

public  class TableScanProvider implements DataProvider {
    private final PackOutput output;
    public TableScanProvider(PackOutput output) { this.output = output; }
    public static List<BlockScanMapping.Rule> rules() {
        var result = new java.util.ArrayList<>(List.of("single", "master", "extension").stream().map(part -> new BlockScanMapping.Rule("thaumcraft:table", Map.of("part", part),
                new BlockScanMapping.Target(part.equals("single") ? "thaumcraft:table" : "thaumcraft:research_table",
                        new Key("Thaumcraft:blockTable", part.equals("single") ? 0 : 2)))).toList());
        result.add(new BlockScanMapping.Rule("thaumcraft:magic_box",Map.of(),new BlockScanMapping.Target("thaumcraft:magic_box",new Key("Thaumcraft:blockMagicBox",0))));
        result.add(new BlockScanMapping.Rule("thaumcraft:travel_paving_stone",Map.of(),new BlockScanMapping.Target("thaumcraft:travel_paving_stone",new Key("Thaumcraft:blockCosmeticSolid",2))));
        result.add(new BlockScanMapping.Rule("thaumcraft:warding_paving_stone",Map.of(),new BlockScanMapping.Target("thaumcraft:warding_paving_stone",new Key("Thaumcraft:blockCosmeticSolid",3))));
        result.add(new BlockScanMapping.Rule("thaumcraft:warding_barrier",Map.of(),new BlockScanMapping.Target(null,new Key("Thaumcraft:blockAiry",4))));
        for(int i=0;i<8;i++) result.add(new BlockScanMapping.Rule("thaumcraft:"+tc4revival.content.LegacyOreBlock.path(i),Map.of(),new BlockScanMapping.Target("thaumcraft:"+tc4revival.content.LegacyOreBlock.path(i),new Key("Thaumcraft:blockCustomOre",i))));
        result.add(new BlockScanMapping.Rule("thaumcraft:obsidian_totem",Map.of(),new BlockScanMapping.Target("thaumcraft:obsidian_totem",new Key("Thaumcraft:blockCosmeticSolid",0))));
        result.add(new BlockScanMapping.Rule("thaumcraft:obsidian_node_totem",Map.of(),new BlockScanMapping.Target("thaumcraft:obsidian_node_totem",new Key("Thaumcraft:blockCosmeticSolid",8))));
        for(boolean closed:new boolean[]{false,true}) result.add(new BlockScanMapping.Rule("thaumcraft:metal_grate",Map.of("closed",Boolean.toString(closed)),new BlockScanMapping.Target("thaumcraft:metal_grate",new Key("Thaumcraft:blockMetalDevice",5))));
        result.add(new BlockScanMapping.Rule("thaumcraft:arcane_spa",Map.of(),new BlockScanMapping.Target("thaumcraft:arcane_spa",new Key("Thaumcraft:blockStoneDevice",12))));
        result.add(new BlockScanMapping.Rule("thaumcraft:flux_scrubber",Map.of(),new BlockScanMapping.Target("thaumcraft:flux_scrubber",new Key("Thaumcraft:blockStoneDevice",14))));
        result.add(new BlockScanMapping.Rule("thaumcraft:deconstruction_table",Map.of(),new BlockScanMapping.Target("thaumcraft:deconstruction_table",new Key("Thaumcraft:blockTable",14))));
        result.add(new BlockScanMapping.Rule("thaumcraft:arcane_workbench", Map.of(),
                new BlockScanMapping.Target("thaumcraft:arcane_workbench", new Key("Thaumcraft:blockTable", 15))));
        result.add(new BlockScanMapping.Rule("thaumcraft:thaumium_block",Map.of(),new BlockScanMapping.Target("thaumcraft:thaumium_block",new Key("Thaumcraft:blockCosmeticSolid",4))));
        result.add(new BlockScanMapping.Rule("thaumcraft:hungry_chest",Map.of(),new BlockScanMapping.Target("thaumcraft:hungry_chest",new Key("Thaumcraft:blockChestHungry",0))));
        result.add(new BlockScanMapping.Rule("thaumcraft:golem_fetter",Map.of(),new BlockScanMapping.Target("thaumcraft:golem_fetter",new Key("Thaumcraft:blockCosmeticSolid",9))));
        for (var row : new String[][]{{"ethereal_bloom","blockCustomPlant","4"},{"alchemy_centrifuge","blockTube","2"},{"essentia_crystallizer","blockTube","7"},{"vis_relay", "blockMetalDevice", "14"}, {"workbench_charger", "blockMetalDevice", "2"},
                {"nitor", "ItemResource", "1"},
                {"crucible", "blockMetalDevice", "0"},
                {"bellows", "blockWoodenDevice", "0"},
                {"arcane_pedestal","blockStoneDevice","1"},
                {"alchemy_furnace","blockStoneDevice","0"}, {"alembic","blockMetalDevice","1"},
                {"essentia_reservoir","blockEssentiaReservoir","0"},
                {"essentia_mirror","blockMirror","6"},
                {"item_mirror","blockMirror","0"},
                {"liquid_death","blockFluidDeath","0"},
                {"arcane_door","ItemArcaneDoor","0"},
                {"warded_glass","blockCosmeticOpaque","2"},
                {"alchemical_construct","blockMetalDevice","9"}, {"advanced_alchemical_construct","blockMetalDevice","3"}, {"mnemonic_matrix","blockMetalDevice","12"},
                {"essentia_tube","blockTube","0"}, {"essentia_valve","blockTube","1"},
                {"essentia_filter","blockTube","3"}, {"essentia_buffer","blockTube","4"},
                {"essentia_restrictor","blockTube","5"}, {"essentia_oneway","blockTube","6"},
                {"infusion_matrix","blockStoneDevice","2"}, {"brain_jar","blockJar","1"}, {"essentia_jar","blockJar","0"}, {"void_jar","blockJar","3"},
                {"taint_crust","blockTaint","0"}, {"taint_soil","blockTaint","1"}, {"flesh_block","blockTaint","2"},
                {"wand_pedestal", "blockStoneDevice", "5"}, {"wand_pedestal_focus", "blockStoneDevice", "8"},
                {"node_stabilizer", "blockStoneDevice", "9"}, {"advanced_node_stabilizer", "blockStoneDevice", "10"}, {"node_converter", "blockStoneDevice", "11"},
                {"focal_manipulator", "blockStoneDevice", "13"}, {"arcane_stone", "blockCosmeticSolid", "6"},
                {"arcane_stone_bricks", "blockCosmeticSolid", "7"}, {"arcane_stone_slab", "blockCosmeticSlabStone", "0"}, {"arcane_stone_stairs", "blockStairsArcaneStone", "0"}})
            result.add(new BlockScanMapping.Rule("thaumcraft:" + row[0], Map.of(), new BlockScanMapping.Target("thaumcraft:" + row[0], new Key("Thaumcraft:" + row[1], Integer.parseInt(row[2])))));
        for (var entry:EldritchIdentities.all()) if (entry.block())
            result.add(new BlockScanMapping.Rule("thaumcraft:"+entry.name(),Map.of(),new BlockScanMapping.Target("thaumcraft:"+entry.name(),new Key("Thaumcraft:"+entry.legacy(),entry.metadata()))));
        for (var entry:CleansingResources.IDENTITIES) if (entry[1].startsWith("block"))
            result.add(new BlockScanMapping.Rule("thaumcraft:"+entry[0],Map.of(),new BlockScanMapping.Target("thaumcraft:"+entry[0],new Key("Thaumcraft:"+entry[1],Integer.parseInt(entry[2])))));
        for (int variant=0;variant<=4;variant++) result.add(new BlockScanMapping.Rule("thaumcraft:taint_fibres",Map.of("variant",Integer.toString(variant)),
                new BlockScanMapping.Target("thaumcraft:"+new String[]{"taint_fibres","taint_grass","taint_plant","taint_spore_stalk","taint_spore_stalk_mature"}[variant],new Key("Thaumcraft:blockTaintFibres",variant))));
        for (boolean upper:new boolean[]{false,true}) result.add(new BlockScanMapping.Rule("thaumcraft:thaumatorium",Map.of("upper",Boolean.toString(upper)),
                new BlockScanMapping.Target("thaumcraft:alchemical_construct",new Key("Thaumcraft:blockMetalDevice",9))));
        for (boolean powered:new boolean[]{false,true}) result.add(new BlockScanMapping.Rule("thaumcraft:arcane_pressure_plate",Map.of("powered",Boolean.toString(powered)),
                new BlockScanMapping.Target("thaumcraft:arcane_pressure_plate",new Key("Thaumcraft:blockWoodenDevice",2))));
        for (int part=0;part<=4;part++) result.add(new BlockScanMapping.Rule("thaumcraft:advanced_alchemy_furnace",Map.of("part",Integer.toString(part)),
                new BlockScanMapping.Target(null,new Key("Thaumcraft:blockAlchemyFurnace",0))));
        for (int part=0;part<=10;part++) result.add(new BlockScanMapping.Rule("thaumcraft:infernal_furnace",Map.of("part",Integer.toString(part)),
                new BlockScanMapping.Target(null,new Key("Thaumcraft:blockArcaneFurnace",0))));
        for (int i=0;i<16;i++) result.add(new BlockScanMapping.Rule("thaumcraft:tallow_candle_"+i,Map.of(),new BlockScanMapping.Target("thaumcraft:tallow_candle_"+i,new Key("Thaumcraft:blockCandle",i))));
        // Legacy Block.getDamageValue delegates to damageDropped; picked item remains blockStoneDevice.
        result.add(new BlockScanMapping.Rule("thaumcraft:infusion_pillar",Map.of(),new BlockScanMapping.Target(null,new Key("Thaumcraft:blockStoneDevice",7))));
        result.add(new BlockScanMapping.Rule("thaumcraft:infusion_pillar_top",Map.of(),new BlockScanMapping.Target(null,new Key("Thaumcraft:blockStoneDevice",6))));
        for(var row:new String[][]{{"portable_hole","blockHole"},{"eldritch_void","blockEldritchNothing"},{"eldritch_portal","blockPortalEldritch"},{"flux_gas","blockFluxGas"},{"flux_goo","blockFluxGoo"}})
            result.add(new BlockScanMapping.Rule("thaumcraft:"+row[0],Map.of(),new BlockScanMapping.Target(null,new Key("Thaumcraft:"+row[1],0))));
        // Reviewed ordinary picked forms; node block entities still take priority at runtime.
        for (var row:new String[][]{
                {"air_crystal_cluster","air_crystal_cluster","Thaumcraft:blockCrystal","0"},
                {"amber_block","amber_block","Thaumcraft:blockCosmeticOpaque","0"},
                {"arcane_bore","arcane_bore","Thaumcraft:blockWoodenDevice","5"},
                {"arcane_bore_base","arcane_bore_base","Thaumcraft:blockWoodenDevice","4"},
                {"arcane_ear","arcane_ear","Thaumcraft:blockWoodenDevice","1"},
                {"arcane_lamp","arcane_lamp","Thaumcraft:blockMetalDevice","7"},
                {"cinderpearl","cinderpearl","Thaumcraft:blockCustomPlant","3"},
                {"earth_crystal_cluster","earth_crystal_cluster","Thaumcraft:blockCrystal","3"},
                {"entropy_crystal_cluster","entropy_crystal_cluster","Thaumcraft:blockCrystal","5"},
                {"fertility_lamp","fertility_lamp","Thaumcraft:blockMetalDevice","13"},
                {"fire_crystal_cluster","fire_crystal_cluster","Thaumcraft:blockCrystal","1"},
                {"greatwood_leaves","greatwood_leaves","Thaumcraft:blockMagicalLeaves","0"},
                {"greatwood_log","greatwood_log","Thaumcraft:blockMagicalLog","0"},
                {"greatwood_planks","greatwood_planks","Thaumcraft:blockWoodenDevice","6"},
                {"greatwood_sapling","greatwood_sapling","Thaumcraft:blockCustomPlant","0"},
                {"greatwood_slab","greatwood_slab","Thaumcraft:blockCosmeticSlabWood","0"},
                {"greatwood_stairs","greatwood_stairs","Thaumcraft:blockStairsGreatwood","0"},
                {"growth_lamp","growth_lamp","Thaumcraft:blockMetalDevice","8"},
                {"levitator","levitator","Thaumcraft:blockLifter","0"},
                {"mixed_crystal_cluster","mixed_crystal_cluster","Thaumcraft:blockCrystal","6"},
                {"order_crystal_cluster","order_crystal_cluster","Thaumcraft:blockCrystal","4"},
                {"shimmerleaf","shimmerleaf","Thaumcraft:blockCustomPlant","2"},
                {"silverwood_leaves","silverwood_leaves","Thaumcraft:blockMagicalLeaves","1"},
                {"silverwood_log","silverwood_log","Thaumcraft:blockMagicalLog","1"},
                {"silverwood_node_log","silverwood_log","Thaumcraft:blockMagicalLog","1"},
                {"silverwood_planks","silverwood_planks","Thaumcraft:blockWoodenDevice","7"},
                {"silverwood_sapling","silverwood_sapling","Thaumcraft:blockCustomPlant","1"},
                {"silverwood_slab","silverwood_slab","Thaumcraft:blockCosmeticSlabWood","1"},
                {"silverwood_stairs","silverwood_stairs","Thaumcraft:blockStairsSilverwood","0"},
                {"vishroom","vishroom","Thaumcraft:blockCustomPlant","5"},
                {"water_crystal_cluster","water_crystal_cluster","Thaumcraft:blockCrystal","2"},
                {"mana_pod","mana_bean","Thaumcraft:ItemManaBean","0"}})
            result.add(new BlockScanMapping.Rule("thaumcraft:"+row[0],Map.of(),new BlockScanMapping.Target("thaumcraft:"+row[1],new Key(row[2],Integer.parseInt(row[3])))));
        return List.copyOf(result);
    }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        return DataProvider.saveStable(cache, BlockScanMapping.encode(rules()), output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve("thaumcraft/block_scan_targets/tables.json"));
    }
    @Override public String getName() { return "TC4 table scan identities"; }
}
