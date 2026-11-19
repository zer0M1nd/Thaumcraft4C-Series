package tc4revival.data;

import java.util.List;
import java.util.Set;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import tc4revival.TC4Revival;
import tc4revival.content.TCTables;

public  class TableResources extends BlockStateProvider {
    public TableResources(PackOutput output, ExistingFileHelper helper) { super(output, TC4Revival.MOD_ID, helper); }
    @Override protected void registerStatesAndModels() {
        simpleBlockWithItem(tc4revival.content.TCArcane.MAGIC_BOX.get(),models().cubeAll("magic_box",modLoc("blocks/woodplain")));
        for(int ore=0;ore<8;ore++) {
            String name=tc4revival.content.LegacyOreBlock.path(ore),texture=ore==0?"cinnibar":ore==7?"amberore":"infusedorestone";
            for(boolean inventory:new boolean[]{false,true}) {
                var model=models().getBuilder(name+(inventory?"_inventory":"")).parent(models().getExistingFile(mcLoc("block/block"))).renderType("minecraft:cutout").texture("base",modLoc("block/"+texture)).texture("overlay",modLoc("block/infusedore")).texture("particle","#base");
                model.element().from(0,0,0).to(16,16,16).allFaces((side,face)->face.texture("#base").cullface(side)).end();
                // The old stone layer has transparent veins. Close the inventory cube behind both
                // inset layers so the seam cannot reveal the world at grazing first-person angles.
                if(inventory&&ore>0&&ore<7) {
                    model.texture("backing",mcLoc("block/stone"));
                    model.element().from(.09f,.09f,.09f).to(15.91f,15.91f,15.91f).allFaces((side,face)->face.texture("#backing")).end();
                }
                if(ore>0&&ore<7) {float inset=inventory?.08f:0;model.element().from(inset,inset,inset).to(16-inset,16-inset,16-inset).allFaces((side,face)->face.texture("#overlay").tintindex(0).cullface(side)).end();}
                if(!inventory)simpleBlock(tc4revival.content.TCArcane.ore(ore),model);
            }
        }
        for(int ore=0;ore<8;ore++) {
            String name=tc4revival.content.DeepslateOreBlock.name(ore);
            String overlay=ore==0?"cinnabar":ore==7?"amber":"infused";
            var model=models().getBuilder(name).parent(models().getExistingFile(mcLoc("block/block"))).renderType("minecraft:cutout")
                    .texture("base",mcLoc("block/deepslate")).texture("overlay",modLoc("block/deepslate_"+overlay+"_overlay")).texture("particle","#base");
            model.element().from(0,0,0).to(16,16,16).allFaces((side,face)->face.texture("#base").cullface(side)).end();
            final boolean tinted=ore>0&&ore<7;
            model.element().from(0,0,0).to(16,16,16).allFaces((side,face)->{face.texture("#overlay").cullface(side);if(tinted)face.tintindex(0);}).end();
            simpleBlockWithItem(tc4revival.content.TCArcane.deepslateOre(ore),model);
        }
        models().cubeAll("obsidian_totem_inventory",modLoc("block/obsidiantile"));
        for(var totem:java.util.List.of(tc4revival.content.TCArcane.OBSIDIAN_TOTEM.get(),tc4revival.content.TCArcane.OBSIDIAN_NODE_TOTEM.get())) getVariantBuilder(totem).forAllStates(state->{
            int form=state.getValue(tc4revival.content.ObsidianTotemBlock.FORM),phase=state.getValue(tc4revival.content.ObsidianTotemBlock.PHASE);
            var model=models().getBuilder(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(totem).getPath()+"_"+form+"_"+phase).parent(models().getExistingFile(mcLoc("block/block"))).texture("particle",modLoc("block/obsidiantile"));
            var element=model.element().from(0,0,0).to(16,16,16);
            for(var side:net.minecraft.core.Direction.values()) {
                String texture=side.getAxis().isVertical()?"obsidiantile":form==0?"obsidiantotembase":form==1?"obsidiantotembaseshaded":"obsidiantotem"+(tc4revival.content.ObsidianTotemBlock.faceIndex(phase,side.get3DDataValue())+1);
                model.texture(side.getName(),modLoc("block/"+texture));element.face(side).texture("#"+side.getName()).cullface(side).end();
            }
            element.end();return net.minecraftforge.client.model.generators.ConfiguredModel.builder().modelFile(model).build();
        });
        getVariantBuilder(tc4revival.content.TCArcane.METAL_GRATE.get()).forAllStates(state->{
            boolean closed=state.getValue(tc4revival.content.MetalGrateBlock.CLOSED);
            var model=models().getBuilder(closed?"metal_grate_closed":"metal_grate").parent(models().getExistingFile(mcLoc("block/block"))).renderType("minecraft:cutout")
                    .texture("all",modLoc("block/grate")).texture("hatch",modLoc("block/grate_hatch")).texture("inside",modLoc("block/metalbase")).texture("particle","#all");
            model.element().from(0,13,0).to(16,16,16).allFaces((side,face)->face.texture("#all")).end();
            for(float offset:new float[]{2.968f,8.968f}) {
                model.element().from(offset,13,0).to(offset,16,16).face(net.minecraft.core.Direction.EAST).texture("#inside").end().end();
                model.element().from(16-offset,13,0).to(16-offset,16,16).face(net.minecraft.core.Direction.WEST).texture("#inside").end().end();
                model.element().from(0,13,offset).to(16,16,offset).face(net.minecraft.core.Direction.SOUTH).texture("#inside").end().end();
                model.element().from(0,13,16-offset).to(16,16,16-offset).face(net.minecraft.core.Direction.NORTH).texture("#inside").end().end();
            }
            if(closed)model.element().from(1,14,1).to(15,15,15).allFaces((side,face)->face.texture("#hatch")).end();
            return net.minecraftforge.client.model.generators.ConfiguredModel.builder().modelFile(model).build();
        });
        var candle=models().getBuilder("tallow_candle").texture("all",modLoc("block/candle")).texture("wick",modLoc("block/candlestub")).texture("particle","#all");
        candle.element().from(6,0,6).to(10,8,10).allFaces((side,face)->face.texture("#all").tintindex(0)).end();
        candle.element().from(7.6f,8,7.6f).to(8.4f,10,8.4f).allFaces((side,face)->face.texture("#wick")).end();
        for (int i=0;i<16;i++) simpleBlock(tc4revival.content.TCArcane.candle(i),candle);
        doorBlockWithRenderType(tc4revival.content.TCArcane.ARCANE_DOOR.get(),modLoc("block/adoorbot"),modLoc("block/adoortop"),"minecraft:cutout");
        simpleBlock(tc4revival.content.TCArcane.ALCHEMICAL_CONSTRUCT.get(),models().cubeAll("alchemical_construct",modLoc("block/alchemyblock")));
        simpleBlock(tc4revival.content.TCArcane.WARDED_GLASS.get(),models().cubeAll("warded_glass",modLoc("block/warded_glass_1")).renderType("minecraft:translucent"));
        getVariantBuilder(tc4revival.content.TCArcane.ARCANE_PRESSURE_PLATE.get()).forAllStates(state->{
            boolean pressed=state.getValue(tc4revival.content.ArcanePressurePlateBlock.POWERED);
            int mode=state.getValue(tc4revival.content.ArcanePressurePlateBlock.MODE);
            String name="arcane_pressure_plate"+(mode==0 ? "" : "_"+mode)+(pressed ? "_down" : "");
            var model=models().getBuilder(name).texture("all",modLoc("block/applate"+(mode+1))).texture("particle","#all")
                    .element().from(1,0,1).to(15,pressed ? .5f : 1,15).allFaces((side,face)->face.texture("#all")).end();
            return net.minecraftforge.client.model.generators.ConfiguredModel.builder().modelFile(model).build();
        });
        simpleBlock(tc4revival.content.TCArcane.ADVANCED_ALCHEMICAL_CONSTRUCT.get(),models().cubeAll("advanced_alchemical_construct",modLoc("block/alchemyblockadv")));
        simpleBlock(tc4revival.content.TCArcane.ADVANCED_ALCHEMY_FURNACE.get(),models().getBuilder("advanced_alchemy_furnace").texture("particle",modLoc("block/metalbase")));
        simpleBlock(tc4revival.content.TCArcane.THAUMATORIUM.get(),models().getBuilder("thaumatorium").texture("particle",modLoc("block/alchemyblock")));
        var matrix=models().getBuilder("mnemonic_matrix").parent(models().getExistingFile(mcLoc("block/block"))).texture("all",modLoc("block/brainbox")).texture("particle",modLoc("block/brainbox"));
        matrix.element().from(3,3,3).to(13,13,13).allFaces((side,face)->face.texture("#all")).end();
        getVariantBuilder(tc4revival.content.TCArcane.MNEMONIC_MATRIX.get()).forAllStates(state->{
            var facing=state.getValue(tc4revival.content.MnemonicMatrixBlock.FACING);
            var model=models().getBuilder("mnemonic_matrix_"+facing.getName()).texture("all",modLoc("block/brainbox")).texture("particle",modLoc("block/brainbox"));
            model.element().from(3,3,3).to(13,13,13).allFaces((side,face)->face.texture("#all")).end();
            var connector=model.element();
            switch(facing) {
                case DOWN->connector.from(6,0,6).to(10,3,10); case UP->connector.from(6,13,6).to(10,16,10);
                case NORTH->connector.from(6,6,0).to(10,10,3); case SOUTH->connector.from(6,6,13).to(10,10,16);
                case WEST->connector.from(0,6,6).to(3,10,10); case EAST->connector.from(13,6,6).to(16,10,10);
            }
            connector.allFaces((side,face)->face.texture("#all")).end();
            return net.minecraftforge.client.model.generators.ConfiguredModel.builder().modelFile(model).build();
        });
        infernalFurnaceModels();
        getVariantBuilder(tc4revival.content.TCArcane.MANA_POD.get()).forAllStates(state->{
            int age=state.getValue(tc4revival.content.ManaPodBlock.AGE),stem=Math.min(age,2);
            return net.minecraftforge.client.model.generators.ConfiguredModel.builder().modelFile(
                    models().cross("mana_pod_"+stem,modLoc("blocks/manapod_stem_"+stem)).renderType("minecraft:cutout")).build();
        });
        for (var kind:tc4revival.content.MagicalPlantBlock.Kind.values()) simpleBlock(tc4revival.content.TCArcane.plant(kind),models().cross(kind.path,modLoc("block/"+kind.texture())).renderType("minecraft:cutout"));
        axisBlock(tc4revival.content.TCArcane.GREATWOOD_LOG.get(),modLoc("block/greatwoodside"),modLoc("block/greatwoodtop"));
        axisBlock(tc4revival.content.TCArcane.SILVERWOOD_LOG.get(),modLoc("block/silverwoodside"),modLoc("block/silverwoodtop"));
        axisBlock(tc4revival.content.TCArcane.SILVERWOOD_NODE_LOG.get(),modLoc("block/silverwoodknotside"),modLoc("block/silverwoodknottop"));
        for (boolean silver:new boolean[]{false,true}) {
            String wood=silver ? "silverwood" : "greatwood";
            var planks=tc4revival.content.TCArcane.woodwork(wood+"_planks"); var texture=modLoc("block/planks_"+wood);
            simpleBlock(planks,models().cubeAll(wood+"_planks",texture));
            slabBlock((net.minecraft.world.level.block.SlabBlock)tc4revival.content.TCArcane.woodwork(wood+"_slab"),modLoc("block/"+wood+"_planks"),texture);
            stairsBlock((net.minecraft.world.level.block.StairBlock)tc4revival.content.TCArcane.woodwork(wood+"_stairs"),texture);
            var leaves=silver ? tc4revival.content.TCArcane.SILVERWOOD_LEAVES.get() : tc4revival.content.TCArcane.GREATWOOD_LEAVES.get();
            simpleBlock(leaves,models().withExistingParent(wood+"_leaves",mcLoc("block/leaves")).texture("all",modLoc("block/"+wood+"leaves")).renderType("minecraft:cutout_mipped"));
            models().withExistingParent(wood+"_leaves_fast",mcLoc("block/leaves")).texture("all",modLoc("block/"+wood+"leaveslow")).renderType("minecraft:cutout_mipped");
            simpleBlock(silver ? tc4revival.content.TCArcane.SILVERWOOD_SAPLING.get() : tc4revival.content.TCArcane.GREATWOOD_SAPLING.get(),models().cross(wood+"_sapling",modLoc("block/"+wood+"sapling")).renderType("minecraft:cutout"));
        }
        for (var type:tc4revival.content.ShardType.values()) simpleBlock(tc4revival.content.TCArcane.crystal(type),models().getBuilder(tc4revival.content.CrystalClusterBlock.path(type)).texture("particle",modLoc("block/crystal")));
        pedestalModels();
        for (var entry:java.util.Map.of("infusion_matrix",tc4revival.content.TCArcane.INFUSION_MATRIX.get(),
                "infusion_pillar",tc4revival.content.TCArcane.INFUSION_PILLAR.get(),"infusion_pillar_top",tc4revival.content.TCArcane.INFUSION_PILLAR_TOP.get()).entrySet())
            simpleBlock(entry.getValue(),models().getBuilder(entry.getKey()).texture("particle",modLoc("block/pedestal_top")));
        crucibleModel();
        for (String texture:List.of("pipe_1","pipe_2","pipe_3","pipe_filter","pipe_filter_core","pipe_buffer","pipe_restrict","pipe_oneway")) models().cubeAll(texture,modLoc("block/"+texture));
        for (var tube:tc4revival.content.TCArcane.essentiaTubes()) {
            String name=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(tube).getPath();
            simpleBlock(tube,models().getBuilder(name).texture("particle",modLoc("block/pipe_1")));
        }
        simpleBlock(tc4revival.content.TCArcane.ALEMBIC.get(),models().getBuilder("alembic").texture("particle",modLoc("block/metalbase")));
        simpleBlock(tc4revival.content.TCArcane.ITEM_MIRROR.get(),models().getBuilder("item_mirror").texture("particle",modLoc("block/mirrorframe")));
        simpleBlock(tc4revival.content.TCArcane.ESSENTIA_MIRROR.get(),models().getBuilder("essentia_mirror").texture("particle",modLoc("block/mirrorframe2")));
        simpleBlock(tc4revival.content.TCArcane.ESSENTIA_RESERVOIR.get(),models().getBuilder("essentia_reservoir")
                .texture("all",modLoc("block/essentiareservoir")).texture("particle",modLoc("block/essentiareservoir"))
                .renderType("minecraft:translucent").element().from(2,2,2).to(14,14,14).allFaces((side,face)->face.texture("#all")).end());
        getVariantBuilder(tc4revival.content.TCArcane.ALCHEMY_FURNACE.get()).forAllStates(state->{
            boolean lit=state.getValue(tc4revival.content.AlchemyFurnaceBlock.LIT),filled=state.getValue(tc4revival.content.AlchemyFurnaceBlock.CONTENTS);
            var model=models().cubeBottomTop("alchemy_furnace"+(lit ? "_lit" : "")+(filled ? "_filled" : ""),modLoc("block/al_furnace_front_"+(lit ? "on" : "off")),modLoc("block/al_furnace_side"),modLoc("block/al_furnace_top"+(filled ? "_filled" : "")));
            return net.minecraftforge.client.model.generators.ConfiguredModel.builder().modelFile(model).build();
        });
        var jar=models().getBuilder("node_jar").renderType("minecraft:translucent")
                .texture("particle",modLoc("block/jar_side")).texture("side",modLoc("block/jar_side"))
                .texture("top",modLoc("block/jar_top")).texture("bottom",modLoc("block/jar_bottom"));
        cuboid(jar,new int[]{3,0,3,13,12,13});
        cuboid(jar,new int[]{5,12,5,11,14,11});
        simpleBlock(tc4revival.content.TCArcane.NODE_JAR.get(),jar);
        for (String name:List.of("essentia_jar","void_jar","brain_jar")) {
            var model=models().getBuilder(name).renderType("minecraft:translucent").texture("particle",modLoc("block/jar_side"))
                    .texture("side",modLoc("block/jar_side"+(name.equals("void_jar") ? "_void" : "")))
                    .texture("top",modLoc("block/jar_top"+(name.equals("void_jar") ? "_void" : ""))).texture("bottom",modLoc("block/jar_bottom"));
            cuboid(model,new int[]{3,0,3,13,12,13}); cuboid(model,new int[]{5,12,5,11,14,11});
            simpleBlock(name.equals("brain_jar") ? tc4revival.content.TCArcane.BRAIN_JAR.get() : name.equals("void_jar") ? tc4revival.content.TCArcane.VOID_JAR.get() : tc4revival.content.TCArcane.ESSENTIA_JAR.get(),model);
        }
        simpleBlock(tc4revival.content.TCArcane.BELLOWS.get(),models().getBuilder("bellows").texture("particle",modLoc("blocks/woodplain")));
        simpleBlock(tc4revival.content.TCArcane.FLUX_GAS.get(), models().getBuilder("flux_gas").texture("particle", modLoc("block/fluxgas")));
        simpleBlock(tc4revival.content.TCArcane.LIQUID_DEATH.get(),models().getBuilder("liquid_death").texture("particle",modLoc("block/fluiddeath")));
        simpleBlock(tc4revival.content.TCArcane.FLUX_GOO.get(), models().getBuilder("flux_goo").texture("particle", modLoc("block/fluxgoo")));
        simpleBlock(tc4revival.content.TCArcane.NITOR.get(), models().getBuilder("nitor").texture("particle", modLoc("item/nitor")));
        simpleBlock(tc4revival.content.TCArcane.AURA_NODE.get(), models().getBuilder("aura_node").texture("particle", modLoc("item/blank")));
        for (var entry : java.util.Map.of("node_stabilizer", tc4revival.content.TCArcane.NODE_STABILIZER.get(),
                "advanced_node_stabilizer", tc4revival.content.TCArcane.ADVANCED_NODE_STABILIZER.get(), "node_converter", tc4revival.content.TCArcane.NODE_CONVERTER.get()).entrySet())
            simpleBlock(entry.getValue(), models().getBuilder(entry.getKey()).texture("particle", modLoc("block/pedestal_top")));
        simpleBlock(tc4revival.content.TCArcane.ENERGIZED_NODE.get(), models().getBuilder("energized_node").texture("particle", modLoc("item/blank")));
        simpleBlock(tc4revival.content.TCArcane.ETHEREAL_BLOOM.get(),models().getBuilder("ethereal_bloom").texture("particle",modLoc("block/purifier_seed")));
        simpleBlock(tc4revival.content.TCArcane.ALCHEMY_CENTRIFUGE.get(),models().getBuilder("alchemy_centrifuge").texture("particle",mcLoc("block/iron_block")));
        simpleBlock(tc4revival.content.TCArcane.ESSENTIA_CRYSTALLIZER.get(),models().getBuilder("essentia_crystallizer").texture("particle",mcLoc("block/iron_block")));
        simpleBlock(tc4revival.content.TCArcane.VIS_RELAY.get(), models().getBuilder("vis_relay").texture("particle", mcLoc("block/iron_block")));
        simpleBlock(tc4revival.content.TCArcane.WORKBENCH_CHARGER.get(), models().getBuilder("workbench_charger").texture("particle", mcLoc("block/iron_block")));
        simpleBlock(tc4revival.content.TCArcane.FOCAL_BLOCK.get(), models().getBuilder("focal_manipulator").texture("particle", modLoc("block/pedestal_top")));
        simpleBlock(tc4revival.content.TCArcane.ARCANE_STONE.get(), models().cubeAll("arcane_stone", modLoc("block/pedestal_top")));
        simpleBlock(tc4revival.content.TCArcane.ARCANE_STONE_BRICKS.get(), models().cubeAll("arcane_stone_bricks", modLoc("block/arcane_stone")));
        slabBlock(tc4revival.content.TCArcane.ARCANE_STONE_SLAB.get(), modLoc("block/arcane_stone_bricks"), modLoc("block/arcane_stone"));
        stairsBlock(tc4revival.content.TCArcane.ARCANE_STONE_STAIRS.get(), modLoc("block/arcane_stone"));
        simpleBlock(tc4revival.content.TCArcane.SHOCK_FIELD.get(), models().getBuilder("shock_field").texture("particle", modLoc("item/blank")));
        simpleBlock(tc4revival.content.TCArcane.PORTABLE_HOLE.get(), models().getBuilder("portable_hole").texture("particle", modLoc("item/blank")));
        simpleBlock(tc4revival.content.TCArcane.WARDED.get(), models().getBuilder("warded").texture("particle", modLoc("item/blank")));
        // Geometry is drawn by the block entity; this model supplies the original woodplain breaking particles.
        var emptyTable=models().getBuilder("table").texture("particle",modLoc("blocks/woodplain"));
        var singleTable=models().getExistingFile(modLoc("block/table_single"));
        var researchTable=models().getExistingFile(modLoc("block/table_master"));
        getVariantBuilder(TCTables.TABLE.get()).forAllStates(state->{
            var part=state.getValue(tc4revival.content.TableBlock.PART);
            var facing=state.getValue(tc4revival.content.TableBlock.FACING);
            int rotation=part==tc4revival.content.TableBlock.Part.SINGLE
                    ? (facing.getAxis()==net.minecraft.core.Direction.Axis.X?90:0)
                    : switch(facing){case SOUTH->90;case WEST->180;case NORTH->270;default->0;};
            return net.minecraftforge.client.model.generators.ConfiguredModel.builder()
                    .modelFile(part==tc4revival.content.TableBlock.Part.SINGLE?singleTable:part==tc4revival.content.TableBlock.Part.MASTER?researchTable:emptyTable)
                    .rotationY(rotation).build();
        });
        simpleBlock(tc4revival.content.TCArcane.WARDING_BARRIER.get(),models().getBuilder("warding_barrier"));
        simpleBlock(tc4revival.content.TCArcane.TRAVEL_PAVING.get(),models().cubeAll("travel_paving_stone",modLoc("blocks/paving_stone_travel")));
        simpleBlock(tc4revival.content.TCArcane.WARDING_PAVING.get(),models().cubeAll("warding_paving_stone",modLoc("blocks/paving_stone_warding")));
        simpleBlock(tc4revival.content.TCArcane.ARCANE_SPA.get(),models().cubeBottomTop("arcane_spa",modLoc("block/spa_side"),modLoc("block/pedestal_top"),modLoc("block/spa_top")));
        simpleBlock(tc4revival.content.TCArcane.FLUX_SCRUBBER.get(), models().getBuilder("flux_scrubber").texture("particle", modLoc("block/pedestal_top")));
        simpleBlock(tc4revival.content.TCArcane.DECONSTRUCTION_TABLE.get(), models().getBuilder("deconstruction_table").texture("particle", modLoc("blocks/woodplain")));
        simpleBlock(tc4revival.content.TCArcane.BLOCK.get(), models().getBuilder("arcane_workbench").texture("particle", modLoc("blocks/woodplain")));
    }
    private void infernalFurnaceModels() {
        getVariantBuilder(tc4revival.content.TCArcane.INFERNAL_FURNACE.get()).forAllStates(state->{
            int part=state.getValue(tc4revival.content.InfernalFurnaceBlock.PART),layer=state.getValue(tc4revival.content.InfernalFurnaceBlock.LAYER);
            var facing=state.getValue(tc4revival.content.InfernalFurnaceBlock.FACING);
            String name="infernal_furnace_"+part+"_"+layer+"_"+facing.getName();
            var model=models().getBuilder(name).texture("particle",modLoc("block/furnace7"));
            if (part==10) {
                model.renderType("minecraft:cutout").texture("frame",modLoc("block/furnace13"))
                        .texture("inside",modLoc("block/furnace15")).texture("fire",mcLoc("block/fire_0"));
                infernalFace(model,facing,6,16,"frame");
                infernalFace(model,facing,3.2f,16,"inside");
                infernalFace(model,facing,1.6f,24,"fire");
            } else {
                var element=model.element().from(0,0,0).to(16,16,16);
                for (var face:net.minecraft.core.Direction.values()) {
                    model.texture(face.getName(),part==0 ? mcLoc("block/lava_still")
                            : modLoc("block/furnace"+tc4revival.content.InfernalFurnaceTextures.texture(part,layer,facing,face)));
                    element.face(face).texture("#"+face.getName()).cullface(face).end();
                }
                element.end();
            }
            return net.minecraftforge.client.model.generators.ConfiguredModel.builder().modelFile(model).build();
        });
    }
    private static void infernalFace(net.minecraftforge.client.model.generators.BlockModelBuilder model,net.minecraft.core.Direction face,
            float depth,float height,String texture) {
        float plane=face.getAxisDirection()==net.minecraft.core.Direction.AxisDirection.POSITIVE ? depth : 16-depth;
        var element=model.element().shade(false);
        if (face.getAxis()==net.minecraft.core.Direction.Axis.X) element.from(plane,0,0).to(plane,height,16);
        else element.from(0,0,plane).to(16,height,plane);
        element.face(face).uvs(0,0,16,16).texture("#"+texture).end(); element.end();
    }
    private void pedestalModels() {
        var ordinary=models().getBuilder("arcane_pedestal").parent(models().getExistingFile(mcLoc("block/block"))).texture("particle",modLoc("block/pedestal_side"))
                .texture("bottom",modLoc("block/pedestal_top")).texture("top",modLoc("block/pedestal_top")).texture("side",modLoc("block/pedestal_side"));
        for (int[] box:new int[][]{{0,0,0,16,4,16},{4,4,4,12,12,12},{2,12,2,14,16,14}}) cuboid(ordinary,box);
        simpleBlock(tc4revival.content.TCArcane.ARCANE_PEDESTAL.get(),ordinary);
        var pedestal = models().getBuilder("wand_pedestal").texture("particle", modLoc("block/pedestal_top"))
                .texture("bottom", modLoc("block/pedestal_top")).texture("top", modLoc("block/wandpedestal_top")).texture("side", modLoc("block/wandpedestal_side"));
        for (int[] box : new int[][]{{0, 0, 0, 16, 4, 16}, {2, 4, 2, 14, 8, 14}, {4, 8, 4, 12, 16, 12}}) cuboid(pedestal, box);
        simpleBlock(tc4revival.content.TCArcane.WAND_PEDESTAL.get(), pedestal);
        var focus = models().getBuilder("wand_pedestal_focus").texture("particle", modLoc("block/wandpedestal_focus_side"))
                .texture("bottom", modLoc("block/wandpedestal_focus_bot")).texture("top", modLoc("block/wandpedestal_focus_top")).texture("side", modLoc("block/wandpedestal_focus_side"));
        for (int[] box : new int[][]{{5, 0, 5, 11, 1, 11}, {1, 0, 7, 5, 1, 9}, {11, 0, 7, 15, 1, 9}, {7, 0, 1, 9, 1, 5}, {7, 0, 11, 9, 1, 15},
                {1, 1, 7, 3, 7, 9}, {7, 1, 1, 9, 7, 3}, {13, 1, 7, 15, 7, 9}, {7, 1, 13, 9, 7, 15}}) cuboid(focus, box);
        simpleBlock(tc4revival.content.TCArcane.WAND_PEDESTAL_FOCUS.get(), focus);
    }
    private void crucibleModel() {
        var model=models().getBuilder("crucible").renderType("minecraft:cutout").texture("particle",modLoc("block/metalbase"));
        for (int i=1;i<=6;i++) model.texture("face"+i,modLoc("block/crucible"+i));
        model.element().from(0,0,0).to(16,16,16).allFaces((face,builder)->builder.texture(face==net.minecraft.core.Direction.DOWN ? "#face2" : face==net.minecraft.core.Direction.UP ? "#face1" : "#face3")).end();
        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            float x1=0,x2=16,z1=0,z2=16;
            if (direction.getAxis()==net.minecraft.core.Direction.Axis.X) x1=x2=direction==net.minecraft.core.Direction.EAST ? 1.968f : 14.032f;
            else z1=z2=direction==net.minecraft.core.Direction.SOUTH ? 1.968f : 14.032f;
            model.element().from(x1,0,z1).to(x2,16,z2).face(direction).texture("#face5").uvs(0,0,16,16).end().end();
        }
        model.element().from(0,4,0).to(16,4,16).face(net.minecraft.core.Direction.UP).texture("#face6").uvs(0,0,16,16).end()
                .face(net.minecraft.core.Direction.DOWN).texture("#face6").uvs(0,0,16,16).end().end();
        simpleBlock(tc4revival.content.TCArcane.CRUCIBLE.get(),model);
    }
    private static void cuboid(net.minecraftforge.client.model.generators.BlockModelBuilder model, int[] box) {
        model.element().from(box[0], box[1], box[2]).to(box[3], box[4], box[5])
                .allFaces((direction, face) -> face.texture(direction == net.minecraft.core.Direction.DOWN ? "#bottom" : direction == net.minecraft.core.Direction.UP ? "#top" : "#side")).end();
    }
    public static LootTableProvider loot(PackOutput output) {
        return new LootTableProvider(output, Set.of(), List.of(new LootTableProvider.SubProviderEntry(Drops::new, LootContextParamSets.BLOCK)));
    }
    private static  class Drops extends BlockLootSubProvider {
        Drops() { super(Set.of(), FeatureFlags.REGISTRY.allFlags()); }
        @Override protected void generate() {
            var door=tc4revival.content.TCArcane.ARCANE_DOOR.get();
            add(door,net.minecraft.world.level.storage.loot.LootTable.lootTable().withPool(
                    net.minecraft.world.level.storage.loot.LootPool.lootPool().when(tc4revival.content.WardedBlocksBreakable.condition())
                            .when(net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition.hasBlockStateProperties(door)
                                    .setProperties(net.minecraft.advancements.critereon.StatePropertiesPredicate.Builder.properties()
                                            .hasProperty(net.minecraft.world.level.block.DoorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER)))
                            .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(tc4revival.content.TCItems.ARCANE_DOOR.get()))));
            var defaultBean=tc4revival.content.ManaBeanItem.of(tc4revival.aspect.TCAspects.catalog().require("herba"));
            add(tc4revival.content.TCArcane.MANA_POD.get(),net.minecraft.world.level.storage.loot.LootTable.lootTable()
                    .withPool(net.minecraft.world.level.storage.loot.LootPool.lootPool().setRolls(new tc4revival.content.ManaPodCount())
                            .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(tc4revival.content.TCItems.MANA_BEAN.get())
                                    .apply(net.minecraft.world.level.storage.loot.functions.SetNbtFunction.setTag(defaultBean.getTag()))
                                    .apply(net.minecraft.world.level.storage.loot.functions.CopyNbtFunction.copyData(
                                            net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider.BLOCK_ENTITY).copy("aspect","Aspects[0].key")))));
            for (var type:tc4revival.content.ShardType.values()) {
                var table=net.minecraft.world.level.storage.loot.LootTable.lootTable();
                for (int index=0;index<6;index++) table.withPool(net.minecraft.world.level.storage.loot.LootPool.lootPool()
                        .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(tc4revival.content.TCItems.shard(type==tc4revival.content.ShardType.BALANCED ? tc4revival.content.ShardType.values()[index] : type))));
                add(tc4revival.content.TCArcane.crystal(type),table);
            }
            for (var block : getKnownBlocks()) {
                if(block==tc4revival.content.TCArcane.MANA_POD.get() || block==tc4revival.content.TCArcane.ARCANE_DOOR.get()) continue;
                if (block instanceof tc4revival.content.EssentiaJarBlock || block instanceof tc4revival.content.NodeJarBlock
                        || block instanceof tc4revival.content.ItemMirrorBlock || block instanceof tc4revival.content.EssentiaMirrorBlock) {
                    add(block,net.minecraft.world.level.storage.loot.LootTable.lootTable().withPool(
                            net.minecraft.world.level.storage.loot.LootPool.lootPool().add(net.minecraft.world.level.storage.loot.entries.LootItem
                                    .lootTableItem(block).when(tc4revival.content.PortableBlockAvailable.available()).apply(tc4revival.content.PortableBlockLoot.portable()))));
                    continue;
                }
                if (block instanceof tc4revival.content.TallowCandleBlock || block instanceof tc4revival.content.ArcanePressurePlateBlock
                        || block instanceof tc4revival.content.WardedGlassBlock) {
                    add(block,net.minecraft.world.level.storage.loot.LootTable.lootTable().withPool(
                            net.minecraft.world.level.storage.loot.LootPool.lootPool().add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(block))));
                    continue;
                }
                if (block==tc4revival.content.TCArcane.ADVANCED_ALCHEMY_FURNACE.get()) {
                    var table=net.minecraft.world.level.storage.loot.LootTable.lootTable();
                    for (int part=0;part<=4;part++) table.withPool(partDrop(block,tc4revival.content.AdvancedAlchemyBlock.PART,part,
                            tc4revival.content.AdvancedAlchemyStructure.restored(part).getBlock()));
                    add(block,table); continue;
                }
                if (block==tc4revival.content.TCArcane.INFERNAL_FURNACE.get()) {
                    var table=net.minecraft.world.level.storage.loot.LootTable.lootTable();
                    // The inlet (part zero) has no physical constituent to drop.
                    for (int part=1;part<=10;part++) table.withPool(partDrop(block,tc4revival.content.InfernalFurnaceBlock.PART,part,
                            tc4revival.content.InfernalFurnaceStructure.restoredMaterial(part)));
                    add(block,table); continue;
                }
                if (block==tc4revival.content.TCArcane.THAUMATORIUM.get()) {
                    add(block,net.minecraft.world.level.storage.loot.LootTable.lootTable().withPool(
                            net.minecraft.world.level.storage.loot.LootPool.lootPool().add(net.minecraft.world.level.storage.loot.entries.LootItem
                                    .lootTableItem(tc4revival.content.TCItems.ALCHEMICAL_CONSTRUCT.get()))));
                    continue;
                }
                if(block instanceof tc4revival.content.LegacyOreBlock ore){
                    if(ore.metadata()==0) dropSelf(block);
                    else {
                        var table=net.minecraft.world.level.storage.loot.LootTable.lootTable()
                                .withPool(net.minecraft.world.level.storage.loot.LootPool.lootPool().when(HAS_SILK_TOUCH)
                                        .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(block)));
                        var ordinary=net.minecraft.world.level.storage.loot.LootPool.lootPool().when(HAS_NO_SILK_TOUCH);
                        if(ore.metadata()==7) ordinary.add(net.minecraft.world.level.storage.loot.entries.LootItem
                                .lootTableItem(tc4revival.content.TCItems.material(tc4revival.content.MaterialType.AMBER))
                                .apply(net.minecraft.world.level.storage.loot.functions.SetItemCountFunction.setCount(new tc4revival.content.LegacyOreCount(1))));
                        else ordinary.setRolls(new tc4revival.content.LegacyOreCount(2)).add(net.minecraft.world.level.storage.loot.entries.LootItem
                                .lootTableItem(tc4revival.content.TCItems.shard(tc4revival.content.ShardType.values()[ore.metadata()-1])));
                        add(block,table.withPool(ordinary));
                    }
                    continue;
                }
                if (block instanceof tc4revival.content.CrystalClusterBlock) continue;
                if (block instanceof tc4revival.content.MagicalLeavesBlock leaves) {
                    var pending=net.minecraft.advancements.critereon.StatePropertiesPredicate.Builder.properties().hasProperty(tc4revival.content.MagicalLeavesBlock.CHECK_DECAY,true).hasProperty(tc4revival.content.MagicalLeavesBlock.PERSISTENT,false);
                    var shears=net.minecraft.world.level.storage.loot.predicates.MatchTool.toolMatches(net.minecraft.advancements.critereon.ItemPredicate.Builder.item().of(net.minecraft.world.item.Items.SHEARS));
                    add(block,net.minecraft.world.level.storage.loot.LootTable.lootTable()
                            .withPool(net.minecraft.world.level.storage.loot.LootPool.lootPool().when(shears)
                                    .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(block)))
                            .withPool(net.minecraft.world.level.storage.loot.LootPool.lootPool().when(shears.invert())
                            .when(net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).setProperties(pending))
                            .when(net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition.randomChance(leaves.silver() ? 1f/250 : 1f/200))
                            .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(leaves.silver() ? tc4revival.content.TCItems.SILVERWOOD_SAPLING.get() : tc4revival.content.TCItems.GREATWOOD_SAPLING.get()))));
                }
                else if (block==tc4revival.content.TCArcane.OBSIDIAN_NODE_TOTEM.get()) dropOther(block,tc4revival.content.TCEldritch.OBSIDIAN_TILE.get());
                else if (block==tc4revival.content.TCArcane.SILVERWOOD_NODE_LOG.get()) dropOther(block,tc4revival.content.TCItems.SILVERWOOD_LOG.get());
                else if (block==tc4revival.content.TCArcane.INFUSION_PILLAR.get()) dropOther(block,tc4revival.content.TCArcane.ARCANE_STONE_BRICKS.get());
                else if (block==tc4revival.content.TCArcane.INFUSION_PILLAR_TOP.get()) dropOther(block,tc4revival.content.TCArcane.ARCANE_STONE.get());
                else if (block instanceof net.minecraft.world.level.block.SlabBlock) add(block,this::createSlabItemTable);
                else dropSelf(block);
            }
        }
        private net.minecraft.world.level.storage.loot.LootPool.Builder partDrop(Block block,
                net.minecraft.world.level.block.state.properties.IntegerProperty property,int part,Block material) {
            return net.minecraft.world.level.storage.loot.LootPool.lootPool()
                    .when(net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                            .setProperties(net.minecraft.advancements.critereon.StatePropertiesPredicate.Builder.properties().hasProperty(property,part)))
                    .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(material));
        }
        @Override protected Iterable<Block> getKnownBlocks() {
            var blocks=new java.util.ArrayList<Block>(tc4revival.content.TCArcane.essentiaTubes());
            blocks.add(tc4revival.content.TCArcane.MANA_POD.get());
            blocks.add(tc4revival.content.TCArcane.MAGIC_BOX.get());
            blocks.add(tc4revival.content.TCArcane.ARCANE_DOOR.get());
            for (int color=0;color<16;color++) blocks.add(tc4revival.content.TCArcane.candle(color));
            blocks.addAll(List.of(tc4revival.content.TCArcane.ESSENTIA_JAR.get(),tc4revival.content.TCArcane.VOID_JAR.get(),
                    tc4revival.content.TCArcane.NODE_JAR.get(),tc4revival.content.TCArcane.ITEM_MIRROR.get(),tc4revival.content.TCArcane.ESSENTIA_MIRROR.get(),
                    tc4revival.content.TCArcane.ARCANE_PRESSURE_PLATE.get(),tc4revival.content.TCArcane.WARDED_GLASS.get()));
            blocks.addAll(List.of(tc4revival.content.TCArcane.ADVANCED_ALCHEMY_FURNACE.get(),
                    tc4revival.content.TCArcane.INFERNAL_FURNACE.get(),tc4revival.content.TCArcane.THAUMATORIUM.get()));
            for(int ore=0;ore<8;ore++){blocks.add(tc4revival.content.TCArcane.ore(ore));blocks.add(tc4revival.content.TCArcane.deepslateOre(ore));}
            for (var kind:tc4revival.content.MagicalPlantBlock.Kind.values()) blocks.add(tc4revival.content.TCArcane.plant(kind));
            for (String name:tc4revival.content.TCArcane.woodworkNames()) blocks.add(tc4revival.content.TCArcane.woodwork(name));
            blocks.addAll(List.of(tc4revival.content.TCArcane.GREATWOOD_LOG.get(),tc4revival.content.TCArcane.SILVERWOOD_LOG.get(),tc4revival.content.TCArcane.SILVERWOOD_NODE_LOG.get(),
                    tc4revival.content.TCArcane.GREATWOOD_LEAVES.get(),tc4revival.content.TCArcane.SILVERWOOD_LEAVES.get(),tc4revival.content.TCArcane.GREATWOOD_SAPLING.get(),tc4revival.content.TCArcane.SILVERWOOD_SAPLING.get()));
            for (var type:tc4revival.content.ShardType.values()) blocks.add(tc4revival.content.TCArcane.crystal(type));
            blocks.addAll(List.of(tc4revival.content.TCArcane.OBSIDIAN_TOTEM.get(),tc4revival.content.TCArcane.OBSIDIAN_NODE_TOTEM.get(),tc4revival.content.TCArcane.METAL_GRATE.get(),tc4revival.content.TCArcane.TRAVEL_PAVING.get(),tc4revival.content.TCArcane.WARDING_PAVING.get(),tc4revival.content.TCArcane.ARCANE_SPA.get(),tc4revival.content.TCArcane.FLUX_SCRUBBER.get(),tc4revival.content.TCArcane.DECONSTRUCTION_TABLE.get(),TCTables.TABLE.get(), tc4revival.content.TCArcane.BLOCK.get(), tc4revival.content.TCArcane.FOCAL_BLOCK.get(),
                    tc4revival.content.TCArcane.CRUCIBLE.get(),tc4revival.content.TCArcane.ALCHEMICAL_CONSTRUCT.get(),tc4revival.content.TCArcane.ADVANCED_ALCHEMICAL_CONSTRUCT.get(),tc4revival.content.TCArcane.MNEMONIC_MATRIX.get(),
                    tc4revival.content.TCArcane.ALEMBIC.get(),tc4revival.content.TCArcane.ALCHEMY_FURNACE.get(),tc4revival.content.TCArcane.ESSENTIA_RESERVOIR.get(),
                    tc4revival.content.TCArcane.BELLOWS.get(),
                    tc4revival.content.TCArcane.ARCANE_PEDESTAL.get(),
                    tc4revival.content.TCArcane.INFUSION_MATRIX.get(),tc4revival.content.TCArcane.INFUSION_PILLAR.get(),tc4revival.content.TCArcane.INFUSION_PILLAR_TOP.get(),
                    tc4revival.content.TCArcane.NITOR.get(),
                    tc4revival.content.TCArcane.WAND_PEDESTAL.get(), tc4revival.content.TCArcane.WAND_PEDESTAL_FOCUS.get(),
                    tc4revival.content.TCArcane.BRAIN_JAR.get(),
                    tc4revival.content.TCArcane.ETHEREAL_BLOOM.get(),
                    tc4revival.content.TCArcane.ALCHEMY_CENTRIFUGE.get(),
                    tc4revival.content.TCArcane.ESSENTIA_CRYSTALLIZER.get(),
                    tc4revival.content.TCArcane.VIS_RELAY.get(), tc4revival.content.TCArcane.WORKBENCH_CHARGER.get(),
                    tc4revival.content.TCArcane.NODE_STABILIZER.get(), tc4revival.content.TCArcane.ADVANCED_NODE_STABILIZER.get(), tc4revival.content.TCArcane.NODE_CONVERTER.get(),
                    tc4revival.content.TCArcane.ARCANE_STONE.get(), tc4revival.content.TCArcane.ARCANE_STONE_BRICKS.get(),
                    tc4revival.content.TCArcane.ARCANE_STONE_SLAB.get(), tc4revival.content.TCArcane.ARCANE_STONE_STAIRS.get()));
            return blocks;
        }
    }
}
