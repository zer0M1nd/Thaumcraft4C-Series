package tc4revival.data;

import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import tc4revival.TC4Revival;
import tc4revival.content.ShardType;

public  class TCItemModelProvider extends ItemModelProvider {
    public TCItemModelProvider(PackOutput output, ExistingFileHelper helper) { super(output, TC4Revival.MOD_ID, helper); }
    @Override protected void registerModels() {
        for(var egg:tc4revival.content.LegacyMobEggs.entries())withExistingParent(egg.path()+"_spawn_egg",mcLoc("item/template_spawn_egg"));
        getBuilder("aura_node").parent(new net.minecraftforge.client.model.generators.ModelFile.UncheckedModelFile(mcLoc("builtin/entity")));
        withExistingParent("book_empty_space",mcLoc("block/cube_all")).texture("all",modLoc("blocks/empty"));
        withExistingParent("book_lava",mcLoc("block/cube_all")).texture("all",mcLoc("block/lava_still"));
        withExistingParent("sanity_checker",mcLoc("item/generated")).texture("layer0",modLoc("item/sanitychecker"));
        var sinisterActive=withExistingParent("sinister_stone_active",mcLoc("item/generated")).texture("layer0",modLoc("item/sinister_stone_active"));
        withExistingParent("sinister_stone",mcLoc("item/generated")).texture("layer0",modLoc("item/sinister_stone")).override().predicate(modLoc("sinister"),1).model(sinisterActive).end();
        withExistingParent("primal_crusher",mcLoc("item/handheld")).texture("layer0",modLoc("item/primal_crusher"));
        var bone=withExistingParent("bone_bow",mcLoc("item/bow")).texture("layer0",modLoc("item/bonebow"));
        for (int stage=0;stage<3;stage++) {
            var model=withExistingParent("bone_bow_pulling_"+stage,mcLoc("item/bow")).texture("layer0",modLoc("item/bonebow_pulling_"+stage));
            bone.override().predicate(modLoc("bone_pull"),stage+1).model(model).end();
        }
        for (int element=0;element<6;element++) {
            String name=ShardType.values()[element].material();
            withExistingParent("primal_arrow_"+name,mcLoc("item/generated")).texture("layer0",modLoc("item/el_arrow_"+name));
        }
        for (String name:java.util.List.of("thaumium_block","golem_fetter"))
            getBuilder(name).parent(new net.minecraftforge.client.model.generators.ModelFile.UncheckedModelFile(modLoc("block/"+name)));
        getBuilder("hungry_chest").parent(new net.minecraftforge.client.model.generators.ModelFile.UncheckedModelFile(mcLoc("builtin/entity"))).texture("particle",modLoc("blocks/woodplain"));
        for (var item:GolemResources.identities()) {
            if (item.legacy().equals("TrunkSpawner")) {
                getBuilder(item.path()).parent(new net.minecraftforge.client.model.generators.ModelFile.UncheckedModelFile(mcLoc("builtin/entity"))).texture("particle",modLoc("models/trunk"));
                continue;
            }
            var base=withExistingParent(item.path(),mcLoc("item/generated")).texture("layer0",modLoc("item/"+item.texture()));
            if (item.legacy().equals("ItemGolemPlacer")) {
                for (int variant=1;variant<=3;variant++) {
                    var model=withExistingParent(item.path()+"_"+variant,mcLoc("item/generated")).texture("layer0",modLoc("item/"+item.texture()));
                    model.texture("layer1",modLoc("item/"+((variant&1)!=0?"golem_over_adv":"blank")));
                    model.texture("layer2",modLoc("item/"+((variant&2)!=0?"golem_over_core":"blank")));
                    base.override().predicate(modLoc("golem_configuration"),variant).model(model).end();
                }
            }
        }
        for(String name:java.util.List.of("brainy_zombie","giant_brainy_zombie","wisp"))withExistingParent(name+"_spawn_egg",mcLoc("item/template_spawn_egg"));
        for(var kind:tc4revival.content.TaintedAnimalEntity.Kind.values())withExistingParent(kind.id()+"_spawn_egg",mcLoc("item/template_spawn_egg"));
        for(String name:java.util.List.of("taint_hive","taint_swarm"))withExistingParent(name+"_spawn_egg",mcLoc("item/template_spawn_egg"));
        withExistingParent("pech_spawn_egg",mcLoc("item/template_spawn_egg"));
        for (int i=0;i<16;i++) withExistingParent("tallow_candle_"+i,modLoc("block/tallow_candle"));
        for (var pair:new String[][]{{"arcane_door","arcanedoor"},{"iron_key","keyiron"},{"gold_key","keygold"}}) withExistingParent(pair[0],mcLoc("item/generated")).texture("layer0",modLoc("item/"+pair[1]));
        withExistingParent("alchemical_construct",modLoc("block/alchemical_construct"));
        withExistingParent("warded_glass",modLoc("block/warded_glass"));
        withExistingParent("arcane_pressure_plate",modLoc("block/arcane_pressure_plate"));
        withExistingParent("advanced_alchemical_construct",modLoc("block/advanced_alchemical_construct"));
        withExistingParent("mnemonic_matrix",modLoc("block/mnemonic_matrix"));
        withExistingParent("liquid_death",mcLoc("item/generated")).texture("layer0",modLoc("block/fluiddeath"));
        withExistingParent("purifying_water_bucket",mcLoc("item/generated")).texture("layer0",modLoc("item/bucket_pure"));
        withExistingParent("liquid_death_bucket",mcLoc("item/generated")).texture("layer0",modLoc("item/bucket_death"));
        withExistingParent("ethereal_bloom",mcLoc("item/generated")).texture("layer0",modLoc("block/purifier_seed"));
        withExistingParent("taint_bottle",mcLoc("item/generated")).texture("layer0",modLoc("item/bottle_taint"));
        withExistingParent("essence_crystal",mcLoc("item/generated")).texture("layer0",modLoc("item/crystalessence"));
        var linkedItemMirror=withExistingParent("item_mirror_linked",mcLoc("item/generated")).texture("layer0",modLoc("block/mirrorframe")).texture("layer1",modLoc("block/mirrorpaneopen"));
        withExistingParent("item_mirror",mcLoc("item/generated")).texture("layer0",modLoc("block/mirrorframe")).texture("layer1",modLoc("block/mirrorpane"))
                .override().predicate(modLoc("linked"),1).model(linkedItemMirror).end();
        withExistingParent("hand_mirror",mcLoc("item/generated")).texture("layer0",modLoc("item/mirrorhand"));
        var linkedMirror=withExistingParent("essentia_mirror_linked",mcLoc("item/generated"))
                .texture("layer0",modLoc("block/mirrorframe2")).texture("layer1",modLoc("block/mirrorpaneopen"));
        withExistingParent("essentia_mirror",mcLoc("item/generated"))
                .texture("layer0",modLoc("block/mirrorframe2")).texture("layer1",modLoc("block/mirrorpane"))
                .override().predicate(modLoc("linked"),1).model(linkedMirror).end();
        withExistingParent("mana_bean",mcLoc("item/generated")).texture("layer0",modLoc("items/mana_bean"));
        for (var kind:tc4revival.content.LootBagItem.Kind.values()) withExistingParent(kind.path,mcLoc("item/generated")).texture("layer0",modLoc("item/"+kind.texture));
        withExistingParent("primordial_pearl", mcLoc("item/generated")).texture("layer0", modLoc("item/eldritch_object_3"));
        for (var type : tc4revival.content.VoidEquipment.Tool.values()) withExistingParent(type.path, mcLoc("item/handheld")).texture("layer0", modLoc("item/" + type.texture));
        for (var type : tc4revival.content.VoidEquipment.Armor.values()) withExistingParent(type.path, mcLoc("item/generated")).texture("layer0", modLoc("item/" + type.texture));
        for (var type : tc4revival.content.ElementalRingType.values()) withExistingParent(type.path(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/bauble_ring_iron"));
        for (var kind:tc4revival.content.MagicalPlantBlock.Kind.values()) withExistingParent(kind.path,mcLoc("item/generated")).texture("layer0",modLoc("block/"+kind.texture()));
        for (String name:tc4revival.content.TCArcane.woodworkNames()) withExistingParent(name,modLoc("block/"+name));
        withExistingParent("silverwood_node_log",modLoc("block/silverwood_node_log"));
        for (String wood:java.util.List.of("greatwood","silverwood")) {
            withExistingParent(wood+"_log",modLoc("block/"+wood+"_log"));
            withExistingParent(wood+"_leaves",modLoc("block/"+wood+"_leaves"));
            withExistingParent(wood+"_sapling",mcLoc("item/generated")).texture("layer0",modLoc("block/"+wood+"sapling"));
        }
        for (var type:ShardType.values()) getBuilder(tc4revival.content.CrystalClusterBlock.path(type)).parent(new net.minecraftforge.client.model.generators.ModelFile.UncheckedModelFile(mcLoc("builtin/entity"))).texture("particle",modLoc("block/crystal"));
        for (var type:tc4revival.content.RunicBaubleType.values()) withExistingParent(type.path(),mcLoc("item/generated")).texture("layer0",modLoc("item/"+type.path()));
        // Depth texture belongs inside the attached 3D focus, not behind its inventory icon.
        withExistingParent("focus_primal",mcLoc("item/generated")).texture("layer0",modLoc("item/focus_primal"));
        withExistingParent("focus_primal_depth",mcLoc("item/generated")).texture("layer0",modLoc("item/focus_primal_depth"));
        withExistingParent("taint_fibres",mcLoc("item/generated")).texture("layer0",modLoc("block/taint_fibres"));
        for (var row:new String[][]{{"taint_grass","taintgrass1"},{"taint_plant","taintgrass2"},{"taint_spore_stalk","taint_spore_stalk_1"},{"taint_spore_stalk_mature","taint_spore_stalk_2"}})
            withExistingParent(row[0],mcLoc("item/generated")).texture("layer0",modLoc("block/"+row[1]));
        getBuilder("bellows").parent(new net.minecraftforge.client.model.generators.ModelFile.UncheckedModelFile(mcLoc("builtin/entity"))).texture("particle",modLoc("blocks/woodplain"));
        withExistingParent("crucible",mcLoc("block/cube_bottom_top")).renderType("minecraft:cutout")
                .texture("bottom",modLoc("block/crucible2")).texture("top",modLoc("block/crucible4")).texture("side",modLoc("block/crucible3"));
        withExistingParent("tainted_goo",mcLoc("item/generated")).texture("layer0",modLoc("item/taint_slime"));
        withExistingParent("tainted_tendril",mcLoc("item/generated")).texture("layer0",modLoc("item/taint_tendril"));
        for (String stone : java.util.List.of("arcane_stone", "arcane_stone_bricks", "arcane_stone_slab", "arcane_stone_stairs"))
            withExistingParent(stone, modLoc("block/" + stone));
        withExistingParent("focus_shock", mcLoc("item/generated")).texture("layer0", modLoc("item/focus_shock"));
        withExistingParent("focus_fire", mcLoc("item/generated")).texture("layer0", modLoc("item/focus_fire"));
        withExistingParent("focus_frost", mcLoc("item/generated")).texture("layer0", modLoc("item/focus_frost"));
        withExistingParent("focus_excavation", mcLoc("item/generated")).texture("layer0", modLoc("item/focus_excavation"));
        withExistingParent("focus_pech", mcLoc("item/generated")).texture("layer0", modLoc("item/focus_pech"));
        withExistingParent("focus_portable_hole", mcLoc("item/generated")).texture("layer0", modLoc("item/focus_portablehole"));
        withExistingParent("focus_trade", mcLoc("item/generated")).texture("layer0", modLoc("item/focus_trade_orn")).texture("layer1", modLoc("item/focus_trade"));
        withExistingParent("focus_trade_orn", mcLoc("item/generated")).texture("layer0", modLoc("item/focus_trade_orn"));
        withExistingParent("focus_whatever_orn", mcLoc("item/generated")).texture("layer0", modLoc("item/focus_whatever_orn"));
        withExistingParent("focus_warding", mcLoc("item/generated")).texture("layer0", modLoc("item/focus_warding_orn")).texture("layer1", modLoc("item/focus_warding"));
        withExistingParent("focus_warding_orn", mcLoc("item/generated")).texture("layer0", modLoc("item/focus_warding_orn"));
        withExistingParent("focus_hellbat", mcLoc("item/generated")).texture("layer0", modLoc("item/focus_hellbat_orn")).texture("layer1", modLoc("item/focus_hellbat"));
        withExistingParent("focus_hellbat_orn", mcLoc("item/generated")).texture("layer0", modLoc("item/focus_hellbat_orn"));
        withExistingParent("goggles", mcLoc("item/generated")).texture("layer0", modLoc("item/gogglesrevealing"));
        for (String part : java.util.List.of("chest", "legs", "boots")) withExistingParent("robe_" + part, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/cloth" + part)).texture("layer1", modLoc("item/cloth" + part + "over"));
        withExistingParent("focus_pouch", mcLoc("item/generated")).texture("layer0", modLoc("item/focuspouch"));
        withExistingParent("primal_charm", mcLoc("item/generated")).texture("layer0", modLoc("item/charm"));
        getBuilder("casting_wand").parent(new net.minecraftforge.client.model.generators.ModelFile.UncheckedModelFile(mcLoc("builtin/entity")))
                .texture("particle", modLoc("item/blank"));
        for (var component : tc4revival.content.WandComponent.all())
            withExistingParent(component.path(), mcLoc("item/generated")).texture("layer0", modLoc("item/" + component.path()));
        withExistingParent("thaumonomicon", mcLoc("item/generated")).texture("layer0", modLoc("item/thaumonomicon"));
        withExistingParent("cheat_thaumonomicon", mcLoc("item/generated")).texture("layer0", modLoc("item/thaumonomiconcheat"));
        for (var type : tc4revival.content.ThaumiumToolType.values()) withExistingParent(type.path(), mcLoc("item/handheld"))
                .texture("layer0", modLoc("item/" + type.texture()));
        for (var type : tc4revival.content.ThaumiumArmorType.values()) withExistingParent(type.path(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + type.texture()));
        for (var type : tc4revival.content.MundaneBaubleType.values()) withExistingParent(type.path(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + type.texture()));
        for (var type : tc4revival.content.FoodType.values()) withExistingParent(type.path(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + type.texture()));
        withExistingParent("zombie_brain", mcLoc("item/generated")).texture("layer0", modLoc("item/brain"));
        for (var type : tc4revival.content.ClusterType.values()) withExistingParent(type.path(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + type.texture()));
        for (var type : tc4revival.content.NuggetType.values()) withExistingParent(type.path(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + type.texture()));
        for (var type : tc4revival.content.MaterialType.values()) withExistingParent(type.path(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + type.texture()));
        for (var type : ShardType.values()) withExistingParent(type.path(), mcLoc("item/generated")).texture("layer0", modLoc("item/" + type.texture()));
        withExistingParent("knowledge_fragment", mcLoc("item/generated")).texture("layer0", modLoc("item/knowledgefragment"));
        withExistingParent("wisp_essence", mcLoc("item/generated")).texture("layer0", modLoc("item/wispessence"));
        withExistingParent("nitor", mcLoc("item/generated")).texture("layer0", modLoc("item/nitor"));
        withExistingParent("wand_pedestal", modLoc("block/wand_pedestal"));
        withExistingParent("arcane_pedestal",modLoc("block/arcane_pedestal"));
        withExistingParent("alumentum",mcLoc("item/generated")).texture("layer0",modLoc("item/alumentum"));
        withExistingParent("essentia_resonator",mcLoc("item/generated")).texture("layer0",modLoc("item/resonator"));
        // The legacy inventory renderer uses the top texture on both top and bottom.
        withExistingParent("alchemy_furnace",mcLoc("block/cube_bottom_top")).texture("side",modLoc("block/al_furnace_front_off"))
                .texture("top",modLoc("block/al_furnace_top")).texture("bottom",modLoc("block/al_furnace_top"));
        withExistingParent("empty_phial",mcLoc("item/generated")).texture("layer0",modLoc("item/phial"));
        withExistingParent("essentia_phial",mcLoc("item/generated")).texture("layer0",modLoc("item/phial")).texture("layer1",modLoc("item/essence"));
        withExistingParent("jar_label",mcLoc("item/generated")).texture("layer0",modLoc("item/label"));
        withExistingParent("wand_pedestal_focus", modLoc("block/wand_pedestal_focus"));
        withExistingParent("lesser_vis_amulet", mcLoc("item/generated")).texture("layer0", modLoc("item/vis_amulet_lesser"));
        withExistingParent("vis_amulet", mcLoc("item/generated")).texture("layer0", modLoc("item/vis_amulet"));
        withExistingParent("scribing_tools", mcLoc("item/generated")).texture("layer0", modLoc("item/inkwell"));
        withExistingParent("pipe_valve_handle",mcLoc("item/generated")).texture("layer0",modLoc("block/pipe_valve"));
        withExistingParent("travel_paving_stone",modLoc("block/travel_paving_stone"));
        withExistingParent("warding_paving_stone",modLoc("block/warding_paving_stone"));
        for(int ore=0;ore<8;ore++)withExistingParent(tc4revival.content.LegacyOreBlock.path(ore),modLoc("block/"+tc4revival.content.LegacyOreBlock.path(ore)+"_inventory"));
        withExistingParent("obsidian_node_totem",modLoc("block/obsidian_totem_inventory"));
        withExistingParent("obsidian_totem",modLoc("block/obsidian_totem_inventory"));
        withExistingParent("metal_grate",modLoc("block/metal_grate"));
        withExistingParent("arcane_spa",modLoc("block/arcane_spa"));
        for (String name : java.util.List.of("essentia_tube", "essentia_valve", "essentia_filter", "essentia_buffer", "essentia_restrictor", "essentia_oneway", "essentia_reservoir", "alembic", "brain_jar", "essentia_jar", "void_jar", "filled_essentia_jar", "filled_void_jar", "infusion_matrix", "node_jar", "alchemy_centrifuge", "essentia_crystallizer", "table", "research_table", "deconstruction_table", "flux_scrubber", "arcane_workbench", "focal_manipulator", "vis_relay", "workbench_charger", "node_stabilizer", "advanced_node_stabilizer", "node_converter")) {
            var model = getBuilder(name).parent(new net.minecraftforge.client.model.generators.ModelFile.UncheckedModelFile(mcLoc("builtin/entity")))
                    .texture("particle", name.equals("vis_relay") || name.equals("workbench_charger") ? mcLoc("block/iron_block")
                            : name.equals("brain_jar") ? modLoc("block/jar_side") : modLoc(name.equals("infusion_matrix") || name.equals("focal_manipulator") || name.contains("node_") ? "block/pedestal_top" : "blocks/woodplain"));
            var transforms = model.transforms();
            transforms.transform(net.minecraft.world.item.ItemDisplayContext.GUI).rotation(30, 225, 0).scale(0.625f).end();
            transforms.transform(net.minecraft.world.item.ItemDisplayContext.GROUND).translation(0, 3, 0).scale(0.25f).end();
            transforms.transform(net.minecraft.world.item.ItemDisplayContext.FIXED).rotation(0, 180, 0).scale(0.5f).end();
            transforms.transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75, 45, 0).translation(0, 2.5f, 0).scale(0.375f).end();
            transforms.transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(75, 45, 0).translation(0, 2.5f, 0).scale(0.375f).end();
            transforms.transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 45, 0).scale(0.4f).end();
            transforms.transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 225, 0).scale(0.4f).end();
        }
        for (String item : java.util.List.of("research_notes", "unknown_research_notes", "discovery")) {
            String texture = item.equals("discovery") ? "discovery" : "researchnotes";
            withExistingParent(item, mcLoc("item/generated")).texture("layer0", modLoc("item/" + texture))
                    .texture("layer1", modLoc("item/" + texture + "overlay"));
        }
        // Custom block geometry and builtin/entity do not inherit block/block's GUI transform.
        // Restore the three-dimensional inventory helper used by the old block renderers.
        for (String name : java.util.List.of("aura_node", "wand_pedestal", "wand_pedestal_focus", "arcane_pedestal",
                "arcane_pressure_plate", "bellows", "traveling_trunk", "hungry_chest"))
            inventoryBlock(getBuilder(name));
        for (int color=0;color<16;color++) inventoryBlock(getBuilder("tallow_candle_"+color));
        for (var type:ShardType.values()) inventoryBlock(getBuilder(tc4revival.content.CrystalClusterBlock.path(type)));
        // This is ModelBakery's synthetic built-in renderer marker, not a model JSON on disk.
        getBuilder("thaumometer").parent(new net.minecraftforge.client.model.generators.ModelFile.UncheckedModelFile(mcLoc("builtin/entity")))
                .texture("particle", modLoc("item/blank"));
    }
    private static void inventoryBlock(net.minecraftforge.client.model.generators.ItemModelBuilder model) {
        var t=model.transforms();
        t.transform(net.minecraft.world.item.ItemDisplayContext.GUI).rotation(30,225,0).scale(.625f).end();
        t.transform(net.minecraft.world.item.ItemDisplayContext.GROUND).translation(0,3,0).scale(.25f).end();
        t.transform(net.minecraft.world.item.ItemDisplayContext.FIXED).rotation(0,180,0).scale(.5f).end();
        t.transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75,45,0).translation(0,2.5f,0).scale(.375f).end();
        t.transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(75,45,0).translation(0,2.5f,0).scale(.375f).end();
        t.transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0,45,0).scale(.4f).end();
        t.transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0,225,0).scale(.4f).end();
    }
}
