package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.NoteBlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class NoteMaterialServerTests {
    @GameTest(template="wand_test_empty")
    public static void legacySupportMaterialsDriveNotesAndArcaneEars(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(2,2,2)); var source=pos.east(4);
        // Exercise all shipped support blocks through the actual note event.
        String[][] groups={
            {"arcane_stone","arcane_stone_bricks","arcane_stone_slab","arcane_stone_stairs","thaumium_block",
             "warded_glass","amber_block","tallow_block","golem_fetter","obsidian_totem","obsidian_node_totem",
             "cinnabar_ore","amber_ore","air_infused_stone","earth_infused_stone","water_infused_stone",
             "fire_infused_stone","order_infused_stone","entropy_infused_stone","alchemy_furnace","infernal_furnace",
             "arcane_pedestal","infusion_matrix","infusion_pillar","infusion_pillar_top","node_stabilizer",
             "advanced_node_stabilizer","node_converter","focal_manipulator","wand_pedestal","wand_pedestal_focus",
             "arcane_spa","flux_scrubber","travel_paving_stone","warding_paving_stone","warded","portable_hole",
             "obsidian_tile","eldritch_stone","eldritch_rock","eldritch_crust","eldritch_stone_nonspawning",
             "eldritch_pedestal_stone","eldritch_stairs","eldritch_slab","eldritch_void","eldritch_altar",
             "eldritch_obelisk","eldritch_obelisk_segment","eldritch_cap","eldritch_lock","eldritch_carved_stone",
             "eldritch_glowing_stone","eldritch_glyphed_stone","eldritch_rune_trap","eldritch_crab_spawner","eldritch_door_frame"},
            {"node_jar","essentia_jar","void_jar","brain_jar","item_mirror","essentia_mirror","eldritch_crystal"},
            {"table","arcane_workbench","deconstruction_table","hungry_chest","bellows","arcane_ear","levitator",
             "arcane_bore","arcane_bore_base","arcane_pressure_plate","greatwood_log","silverwood_log",
             "silverwood_node_log","greatwood_planks","silverwood_planks","greatwood_slab","silverwood_slab",
             "greatwood_stairs","silverwood_stairs","loot_crate_0","loot_crate_1","loot_crate_2"},
            {"crucible","alembic","alchemical_construct","advanced_alchemical_construct","advanced_alchemy_furnace",
             "essentia_reservoir","essentia_tube","arcane_door","loot_urn_0","loot_urn_1","loot_urn_2","flesh_block"}
        };
        int count=0;
        try {
            level.setBlock(source,Blocks.NOTE_BLOCK.defaultBlockState(),18);
            for (int group=0;group<groups.length;group++) for (String name:groups[group]) {
                var key=ResourceLocation.parse("thaumcraft:"+name);
                test.assertTrue(ForgeRegistries.BLOCKS.containsKey(key),"Expected legacy support is registered: "+key);
                var block=ForgeRegistries.BLOCKS.getValue(key);
                for (var state:block.getStateDefinition().getPossibleStates())
                    test.assertTrue(ArcaneEarListening.tone(state)==ArcaneEarListening.instrument(state.instrument()),"Registered native instrument for every state: "+key);
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
                // Finish the previous support's dismantling before placing another monument at the same cell.
                level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),18);
                level.setBlock(source.below(),Blocks.AIR.defaultBlockState(),18);
                level.setBlock(pos.below(),block.defaultBlockState(),18);
                level.setBlock(source.below(),block.defaultBlockState(),18);
                level.setBlock(pos,TCArcane.ARCANE_EAR.get().defaultBlockState(),18);
                var ear=(ArcaneEarBlockEntity)level.getBlockEntity(pos); ear.updateTone(); ArcaneEarListening.register(ear);
                var event=new NoteBlockEvent.Play(level,source,level.getBlockState(source),0,block.defaultBlockState().instrument());
                MinecraftForge.EVENT_BUS.post(event);
                test.assertTrue(event.getInstrument()==block.defaultBlockState().instrument() && ear.signalTicks()==10,
                        "Note event remains unchanged and its receiver responds: "+key);
                count++;
            }
            test.assertTrue(ArcaneEarListening.tone(Blocks.EMERALD_BLOCK.defaultBlockState())==ArcaneEarListening.instrument(NoteBlockInstrument.BIT)
                    && ArcaneEarListening.tone(Blocks.SOUL_SAND.defaultBlockState())==ArcaneEarListening.instrument(NoteBlockInstrument.COW_BELL)
                    && ArcaneEarListening.tone(Blocks.GLOWSTONE.defaultBlockState())==ArcaneEarListening.instrument(NoteBlockInstrument.PLING),"Modern special instruments are distinguishable");
            tc4revival.TC4Revival.LOGGER.info("PASS legacySupportMaterialsDriveNotesAndArcaneEars: {} supports plus crystals",count);
            test.succeed();
        } finally {
            for (var cleanup:new BlockPos[]{pos,source,pos.below(),source.below()}) level.setBlock(cleanup,Blocks.AIR.defaultBlockState(),18);
        }
    }
}
