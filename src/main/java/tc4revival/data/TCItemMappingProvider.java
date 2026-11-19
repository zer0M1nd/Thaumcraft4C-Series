package tc4revival.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import tc4revival.content.ShardType;

/** Semantic identities for implemented TC item families; the vanilla migration table remains separate. */
public  class TCItemMappingProvider implements DataProvider {
    private final PackOutput output;
    public TCItemMappingProvider(PackOutput output) { this.output = output; }
    public static JsonObject definition() {
        var root = new JsonObject();
        root.addProperty("format", 1);
        var items = new JsonArray();
        var node=new JsonObject();node.addProperty("item","thaumcraft:aura_node");node.addProperty("legacy_item","Thaumcraft:blockAiry");node.addProperty("metadata",0);items.add(node);
        var magicBox=new JsonObject();magicBox.addProperty("item","thaumcraft:magic_box");magicBox.addProperty("legacy_item","Thaumcraft:blockMagicBox");magicBox.addProperty("metadata",0);items.add(magicBox);
        var crusher=new JsonObject(); crusher.addProperty("item","thaumcraft:primal_crusher"); crusher.addProperty("legacy_item","Thaumcraft:ItemPrimalCrusher"); crusher.addProperty("metadata",0); items.add(crusher);
        var bow=new JsonObject(); bow.addProperty("item","thaumcraft:bone_bow"); bow.addProperty("legacy_item","Thaumcraft:ItemBowBone"); bow.addProperty("metadata",0); items.add(bow);
        for (int element=0;element<6;element++) {
            var arrow=new JsonObject(); arrow.addProperty("item","thaumcraft:primal_arrow_"+ShardType.values()[element].material());
            arrow.addProperty("legacy_item","Thaumcraft:PrimalArrow"); arrow.addProperty("metadata",element); items.add(arrow);
        }
        var hungryChest=new JsonObject(); hungryChest.addProperty("item","thaumcraft:hungry_chest"); hungryChest.addProperty("legacy_item","Thaumcraft:blockChestHungry"); hungryChest.addProperty("metadata",0); items.add(hungryChest);
        var fetter=new JsonObject(); fetter.addProperty("item","thaumcraft:golem_fetter"); fetter.addProperty("legacy_item","Thaumcraft:blockCosmeticSolid"); fetter.addProperty("metadata",9); items.add(fetter);
        var thaumiumBlock=new JsonObject(); thaumiumBlock.addProperty("item","thaumcraft:thaumium_block"); thaumiumBlock.addProperty("legacy_item","Thaumcraft:blockCosmeticSolid"); thaumiumBlock.addProperty("metadata",4); items.add(thaumiumBlock);
        for (var identity:GolemResources.identities()) {
            var row=new JsonObject(); row.addProperty("item","thaumcraft:"+identity.path()); row.addProperty("legacy_item","Thaumcraft:"+identity.legacy()); row.addProperty("metadata",identity.metadata()); items.add(row);
        }
        var elementalPick=new JsonObject(); elementalPick.addProperty("item","thaumcraft:elemental_pickaxe"); elementalPick.addProperty("legacy_item","Thaumcraft:ItemPickaxeElemental"); elementalPick.addProperty("metadata",0); items.add(elementalPick);
        var elementalHoe=new JsonObject(); elementalHoe.addProperty("item","thaumcraft:elemental_hoe"); elementalHoe.addProperty("legacy_item","Thaumcraft:ItemHoeElemental"); elementalHoe.addProperty("metadata",0); items.add(elementalHoe);
        for (String tool:java.util.List.of("Axe","Shovel","Sword")) {
            var entry=new JsonObject(); entry.addProperty("item","thaumcraft:elemental_"+tool.toLowerCase(java.util.Locale.ROOT));
            entry.addProperty("legacy_item","Thaumcraft:Item"+tool+"Elemental"); entry.addProperty("metadata",0); items.add(entry);
        }
        for (var entry:ArcaneBoreResources.IDENTITIES) {
            var row=new JsonObject(); row.addProperty("item","thaumcraft:"+entry[0]); row.addProperty("legacy_item","Thaumcraft:"+entry[1]); row.addProperty("metadata",Integer.parseInt(entry[2])); items.add(row);
        }
        for (var entry:ArcaneLampResources.IDENTITIES) {
            var row=new JsonObject(); row.addProperty("item","thaumcraft:"+entry[0]); row.addProperty("legacy_item","Thaumcraft:"+entry[1]); row.addProperty("metadata",Integer.parseInt(entry[2])); items.add(row);
        }
        for (var entry:FortressResources.all()) {
            var row=new JsonObject(); row.addProperty("item","thaumcraft:"+entry[0]); row.addProperty("legacy_item","Thaumcraft:"+entry[1]); row.addProperty("metadata",0); items.add(row);
        }
        for (var entry:CleansingResources.IDENTITIES) {
            var row=new JsonObject(); row.addProperty("item","thaumcraft:"+entry[0]); row.addProperty("legacy_item","Thaumcraft:"+entry[1]); row.addProperty("metadata",Integer.parseInt(entry[2])); items.add(row);
        }
        for (var entry:UtilityDeviceResources.IDENTITIES) {
            var row=new JsonObject(); row.addProperty("item","thaumcraft:"+entry[0]); row.addProperty("legacy_item","Thaumcraft:"+entry[1]); row.addProperty("metadata",Integer.parseInt(entry[2])); items.add(row);
        }
        for (var entry:EldritchIdentities.all()) {
            var row=new JsonObject(); row.addProperty("item","thaumcraft:"+entry.name()); row.addProperty("legacy_item","Thaumcraft:"+entry.legacy());
            row.addProperty("metadata",entry.metadata()); items.add(row);
        }
        for (var armor:tc4revival.content.CultistEquipment.Armor.values()) {
            var row=new JsonObject(); row.addProperty("item","thaumcraft:"+armor.path); row.addProperty("legacy_item","Thaumcraft:"+armor.legacy); row.addProperty("metadata",0); items.add(row);
        }
        for (String name:java.util.List.of("crimson_rites","crimson_sword")) {
            var row=new JsonObject(); row.addProperty("item","thaumcraft:"+name); row.addProperty("legacy_item","Thaumcraft:"+(name.equals("crimson_rites") ? "ItemEldritchObject" : "ItemSwordCrimson")); row.addProperty("metadata",name.equals("crimson_rites") ? 1 : 0); items.add(row);
        }
        var bean=new JsonObject(); bean.addProperty("item","thaumcraft:mana_bean"); bean.addProperty("legacy_item","Thaumcraft:ItemManaBean"); bean.addProperty("metadata",0); items.add(bean);
        for (var kind:tc4revival.content.LootBagItem.Kind.values()) {
            var row=new JsonObject(); row.addProperty("item","thaumcraft:"+kind.path); row.addProperty("legacy_item","Thaumcraft:ItemLootBag"); row.addProperty("metadata",kind.metadata); items.add(row);
        }
        var pearl = new JsonObject(); pearl.addProperty("item", "thaumcraft:primordial_pearl");
        pearl.addProperty("legacy_item", "Thaumcraft:ItemEldritchObject"); pearl.addProperty("metadata", 3); items.add(pearl);
        for (var type : tc4revival.content.VoidEquipment.Tool.values()) {
            var row = new JsonObject(); row.addProperty("item", "thaumcraft:" + type.path);
            row.addProperty("legacy_item", "Thaumcraft:" + type.legacyName); row.addProperty("metadata", 0); items.add(row);
        }
        for (var type : tc4revival.content.VoidEquipment.Armor.values()) {
            var row = new JsonObject(); row.addProperty("item", "thaumcraft:" + type.path);
            row.addProperty("legacy_item", "Thaumcraft:" + type.legacyName); row.addProperty("metadata", 0); items.add(row);
        }
        for (var type : tc4revival.content.ElementalRingType.values()) {
            var row = new JsonObject(); row.addProperty("item", "thaumcraft:" + type.path());
            row.addProperty("legacy_item", "Thaumcraft:ItemBaubleBlanks"); row.addProperty("metadata", type.metadata()); items.add(row);
        }
        for (var kind:tc4revival.content.MagicalPlantBlock.Kind.values()) {
            var row=new JsonObject(); row.addProperty("item","thaumcraft:"+kind.path); row.addProperty("legacy_item","Thaumcraft:blockCustomPlant"); row.addProperty("metadata",kind.metadata); items.add(row);
        }
        for (int wood=0;wood<2;wood++) for (String kind:new String[]{"planks","slab","stairs"}) {
            String name=wood==0 ? "greatwood" : "silverwood"; var row=new JsonObject(); row.addProperty("item","thaumcraft:"+name+"_"+kind);
            row.addProperty("legacy_item","Thaumcraft:"+switch (kind) { case "planks"->"blockWoodenDevice"; case "slab"->"blockCosmeticSlabWood"; default->wood==0 ? "blockStairsGreatwood" : "blockStairsSilverwood"; });
            row.addProperty("metadata",kind.equals("planks") ? 6+wood : kind.equals("slab") ? wood : 0); items.add(row);
        }
        for (int wood=0;wood<2;wood++) for (String kind:new String[]{"log","leaves","sapling"}) {
            var row=new JsonObject(); row.addProperty("item","thaumcraft:"+(wood==0 ? "greatwood_" : "silverwood_")+kind);
            row.addProperty("legacy_item","Thaumcraft:"+switch (kind) { case "log"->"blockMagicalLog"; case "leaves"->"blockMagicalLeaves"; default->"blockCustomPlant"; });
            row.addProperty("metadata",wood); items.add(row);
        }
        for (var type:ShardType.values()) {
            var row=new JsonObject(); row.addProperty("item","thaumcraft:"+tc4revival.content.CrystalClusterBlock.path(type));
            row.addProperty("legacy_item","Thaumcraft:blockCrystal"); row.addProperty("metadata",type.metadata()); items.add(row);
        }
        for (var type:tc4revival.content.RunicBaubleType.values()) {
            var row=new JsonObject(); row.addProperty("item","thaumcraft:"+type.path()); row.addProperty("legacy_item","Thaumcraft:"+type.legacy()); row.addProperty("metadata",type.metadata()); items.add(row);
        }
        for (var component : tc4revival.content.WandComponent.all()) {
            var row = new JsonObject(); row.addProperty("item", "thaumcraft:" + component.path());
            row.addProperty("legacy_item", "Thaumcraft:" + component.legacyItem()); row.addProperty("metadata", component.metadata()); items.add(row);
        }
        for (var type : tc4revival.content.ThaumiumToolType.values()) {
            var row = new JsonObject(); row.addProperty("item", "thaumcraft:" + type.path());
            row.addProperty("legacy_item", "Thaumcraft:" + type.legacyName()); row.addProperty("metadata", 0); items.add(row);
        }
        for (var type : tc4revival.content.ThaumiumArmorType.values()) {
            var row = new JsonObject(); row.addProperty("item", "thaumcraft:" + type.path());
            row.addProperty("legacy_item", "Thaumcraft:" + type.legacyName()); row.addProperty("metadata", 0); items.add(row);
        }
        for (var type : tc4revival.content.MundaneBaubleType.values()) {
            var row = new JsonObject(); row.addProperty("item", "thaumcraft:" + type.path());
            row.addProperty("legacy_item", "Thaumcraft:ItemBaubleBlanks"); row.addProperty("metadata", type.metadata()); items.add(row);
        }
        for (var type : tc4revival.content.FoodType.values()) {
            var row = new JsonObject(); row.addProperty("item", "thaumcraft:" + type.path());
            row.addProperty("legacy_item", "Thaumcraft:" + type.legacyName()); row.addProperty("metadata", 0); items.add(row);
        }
        for (var type : ShardType.values()) {
            var row = new JsonObject();
            row.addProperty("item", type.id());
            row.addProperty("legacy_item", "Thaumcraft:ItemShard");
            row.addProperty("metadata", type.metadata());
            items.add(row);
        }
        var tool = new JsonObject();
        tool.addProperty("item", "thaumcraft:thaumometer");
        tool.addProperty("legacy_item", "Thaumcraft:ItemThaumometer");
        tool.addProperty("metadata", 0);
        items.add(tool);
        String[][] researchItems = {{"knowledge_fragment", "Thaumcraft:ItemResource", "9"},
                {"silverwood_node_log","Thaumcraft:blockMagicalLog","2"},
                {"thaumonomicon", "Thaumcraft:ItemThaumonomicon", "0"}, {"cheat_thaumonomicon", "Thaumcraft:ItemThaumonomicon", "42"},
                {"zombie_brain", "Thaumcraft:ItemZombieBrain", "0"},
                {"sanity_checker","Thaumcraft:ItemSanityChecker","0"},
                {"sinister_stone","Thaumcraft:ItemCompassStone","0"},
                {"travel_paving_stone","Thaumcraft:blockCosmeticSolid","2"},
                {"warding_paving_stone","Thaumcraft:blockCosmeticSolid","3"},
                {"cinnabar_ore", "Thaumcraft:blockCustomOre", "0"},
                {"air_infused_stone", "Thaumcraft:blockCustomOre", "1"},
                {"fire_infused_stone", "Thaumcraft:blockCustomOre", "2"},
                {"water_infused_stone", "Thaumcraft:blockCustomOre", "3"},
                {"earth_infused_stone", "Thaumcraft:blockCustomOre", "4"},
                {"order_infused_stone", "Thaumcraft:blockCustomOre", "5"},
                {"entropy_infused_stone", "Thaumcraft:blockCustomOre", "6"},
                {"amber_ore", "Thaumcraft:blockCustomOre", "7"},
                {"obsidian_node_totem", "Thaumcraft:blockCosmeticSolid", "8"},
                {"obsidian_totem", "Thaumcraft:blockCosmeticSolid", "0"},
                {"metal_grate", "Thaumcraft:blockMetalDevice", "5"},
                {"arcane_spa", "Thaumcraft:blockStoneDevice", "12"},
                {"flux_scrubber", "Thaumcraft:blockStoneDevice", "14"},
                {"deconstruction_table", "Thaumcraft:blockTable", "14"},
                {"arcane_workbench", "Thaumcraft:blockTable", "15"},
                {"primal_charm", "Thaumcraft:ItemResource", "15"}, {"casting_wand", "Thaumcraft:WandCasting", "0"},
                {"wisp_essence", "Thaumcraft:ItemWispEssence", "0"},
                {"taint_hive_spawn_egg","Thaumcraft:ItemSpawnerEgg","20"},
                {"taint_swarm_spawn_egg","Thaumcraft:ItemSpawnerEgg","21"},
                {"tainted_chicken_spawn_egg","Thaumcraft:ItemSpawnerEgg","22"},
                {"tainted_cow_spawn_egg","Thaumcraft:ItemSpawnerEgg","23"},
                {"tainted_creeper_spawn_egg","Thaumcraft:ItemSpawnerEgg","24"},
                {"tainted_pig_spawn_egg","Thaumcraft:ItemSpawnerEgg","25"},
                {"tainted_sheep_spawn_egg","Thaumcraft:ItemSpawnerEgg","26"},
                {"tainted_villager_spawn_egg","Thaumcraft:ItemSpawnerEgg","27"},
                {"tainted_goo", "Thaumcraft:ItemResource", "11"}, {"tainted_tendril", "Thaumcraft:ItemResource", "12"},
                {"nitor", "Thaumcraft:ItemResource", "1"},
                {"crucible", "Thaumcraft:blockMetalDevice", "0"},
                {"bellows", "Thaumcraft:blockWoodenDevice", "0"},
                {"arcane_pedestal","Thaumcraft:blockStoneDevice","1"},
                {"alumentum","Thaumcraft:ItemResource","0"},
                {"essentia_resonator","Thaumcraft:ItemResonator","0"},
                {"alchemy_furnace","Thaumcraft:blockStoneDevice","0"}, {"alembic","Thaumcraft:blockMetalDevice","1"},
                {"essentia_reservoir","Thaumcraft:blockEssentiaReservoir","0"},
                {"essentia_mirror","Thaumcraft:blockMirror","6"},
                {"item_mirror","Thaumcraft:blockMirror","0"},
                {"hand_mirror","Thaumcraft:HandMirror","0"},
                {"liquid_death_bucket","Thaumcraft:ItemBucketDeath","0"},
                {"purifying_water_bucket","Thaumcraft:ItemBucketPure","0"},
                {"liquid_death","Thaumcraft:blockFluidDeath","0"},
                {"taint_bottle","Thaumcraft:ItemBottleTaint","0"},
                {"essence_crystal","Thaumcraft:ItemCrystalEssence","0"},
                {"warded_glass","Thaumcraft:blockCosmeticOpaque","2"},
                {"arcane_door","Thaumcraft:ItemArcaneDoor","0"}, {"iron_key","Thaumcraft:ArcaneDoorKey","0"}, {"gold_key","Thaumcraft:ArcaneDoorKey","1"},
                {"arcane_pressure_plate","Thaumcraft:blockWoodenDevice","2"},
                {"ethereal_bloom","Thaumcraft:blockCustomPlant","4"},
                {"alchemy_centrifuge","Thaumcraft:blockTube","2"},
                {"essentia_crystallizer","Thaumcraft:blockTube","7"},
                {"alchemical_construct","Thaumcraft:blockMetalDevice","9"}, {"advanced_alchemical_construct","Thaumcraft:blockMetalDevice","3"}, {"mnemonic_matrix","Thaumcraft:blockMetalDevice","12"},
                {"essentia_tube","Thaumcraft:blockTube","0"}, {"essentia_valve","Thaumcraft:blockTube","1"},
                {"essentia_filter","Thaumcraft:blockTube","3"}, {"essentia_buffer","Thaumcraft:blockTube","4"},
                {"essentia_restrictor","Thaumcraft:blockTube","5"}, {"essentia_oneway","Thaumcraft:blockTube","6"},
                {"infusion_matrix","Thaumcraft:blockStoneDevice","2"},
                {"brain_jar","Thaumcraft:blockJar","1"}, {"essentia_jar","Thaumcraft:blockJar","0"}, {"void_jar","Thaumcraft:blockJar","3"},
                {"filled_essentia_jar","Thaumcraft:BlockJarFilledItem","0"}, {"filled_void_jar","Thaumcraft:BlockJarFilledItem","3"},
                {"jar_label","Thaumcraft:ItemResource","13"}, {"empty_phial","Thaumcraft:ItemEssence","0"}, {"essentia_phial","Thaumcraft:ItemEssence","1"},
                {"node_jar", "Thaumcraft:BlockJarNodeItem", "0"},
                {"taint_crust","Thaumcraft:blockTaint","0"}, {"taint_soil","Thaumcraft:blockTaint","1"},
                {"flesh_block","Thaumcraft:blockTaint","2"}, {"taint_fibres","Thaumcraft:blockTaintFibres","0"},
                {"taint_grass","Thaumcraft:blockTaintFibres","1"}, {"taint_plant","Thaumcraft:blockTaintFibres","2"},
                {"taint_spore_stalk","Thaumcraft:blockTaintFibres","3"}, {"taint_spore_stalk_mature","Thaumcraft:blockTaintFibres","4"},
                {"wand_pedestal", "Thaumcraft:blockStoneDevice", "5"}, {"wand_pedestal_focus", "Thaumcraft:blockStoneDevice", "8"},
                {"lesser_vis_amulet", "Thaumcraft:ItemAmuletVis", "0"}, {"vis_amulet", "Thaumcraft:ItemAmuletVis", "1"},
                {"focus_pouch", "Thaumcraft:FocusPouch", "0"},
                {"focus_shock", "Thaumcraft:FocusShock", "0"},
                {"focus_fire", "Thaumcraft:FocusFire", "0"}, {"focus_frost", "Thaumcraft:FocusFrost", "0"},
                {"focus_excavation", "Thaumcraft:FocusExcavation", "0"},
                {"focus_pech", "Thaumcraft:FocusPech", "0"},
                {"focus_portable_hole", "Thaumcraft:FocusPortableHole", "0"},
                {"focus_trade", "Thaumcraft:FocusTrade", "0"},
                {"focus_warding", "Thaumcraft:FocusWarding", "0"},
                {"focus_hellbat", "Thaumcraft:FocusHellbat", "0"},
                {"focus_primal","Thaumcraft:FocusPrimal","0"},
                {"goggles", "Thaumcraft:ItemGoggles", "0"},
                {"robe_chest", "Thaumcraft:ItemChestplateRobe", "0"},
                {"robe_legs", "Thaumcraft:ItemLeggingsRobe", "0"},
                {"robe_boots", "Thaumcraft:ItemBootsRobe", "0"},
                {"focal_manipulator", "Thaumcraft:blockStoneDevice", "13"},
                {"vis_relay", "Thaumcraft:blockMetalDevice", "14"}, {"workbench_charger", "Thaumcraft:blockMetalDevice", "2"},
                {"node_stabilizer", "Thaumcraft:blockStoneDevice", "9"}, {"advanced_node_stabilizer", "Thaumcraft:blockStoneDevice", "10"}, {"node_converter", "Thaumcraft:blockStoneDevice", "11"},
                {"arcane_stone", "Thaumcraft:blockCosmeticSolid", "6"}, {"arcane_stone_bricks", "Thaumcraft:blockCosmeticSolid", "7"},
                {"arcane_stone_slab", "Thaumcraft:blockCosmeticSlabStone", "0"}, {"arcane_stone_stairs", "Thaumcraft:blockStairsArcaneStone", "0"},
                {"research_notes", "Thaumcraft:ItemResearchNotes", "0"}, {"unknown_research_notes", "Thaumcraft:ItemResearchNotes", "42"},
                {"discovery", "Thaumcraft:ItemResearchNotes", "64"}, {"scribing_tools", "Thaumcraft:ItemInkwell", "0"},
                {"table", "Thaumcraft:blockTable", "0"}, {"research_table", "Thaumcraft:blockTable", "2"}};
        for (String[] mapping : researchItems) {
            var row = new JsonObject();
            row.addProperty("item", "thaumcraft:" + mapping[0]); row.addProperty("legacy_item", mapping[1]);
            row.addProperty("metadata", Integer.parseInt(mapping[2])); items.add(row);
        }
        for (int i=0;i<16;i++) { var row=new JsonObject(); row.addProperty("item","thaumcraft:tallow_candle_"+i); row.addProperty("legacy_item","Thaumcraft:blockCandle"); row.addProperty("metadata",i); items.add(row); }
        root.add("items", items);
        for (var type : tc4revival.content.MaterialType.values()) {
            var row = new JsonObject(); row.addProperty("item", "thaumcraft:" + type.path());
            row.addProperty("legacy_item", "Thaumcraft:ItemResource"); row.addProperty("metadata", type.metadata()); items.add(row);
        }
        root.add("groups", new JsonArray());
        for (var type : tc4revival.content.ClusterType.values()) {
            var row = new JsonObject(); row.addProperty("item", "thaumcraft:" + type.path());
            row.addProperty("legacy_item", "Thaumcraft:ItemNugget"); row.addProperty("metadata", type.metadata()); items.add(row);
        }
        for (var type : tc4revival.content.NuggetType.values()) {
            var row = new JsonObject(); row.addProperty("item", "thaumcraft:" + type.path());
            row.addProperty("legacy_item", "Thaumcraft:ItemNugget"); row.addProperty("metadata", type.metadata()); items.add(row);
        }
        return root;
    }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        return DataProvider.saveStable(cache, definition(), output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve("thaumcraft/legacy_item_mappings/thaumcraft.json"));
    }
    @Override public String getName() { return "TC4 implemented item family identities"; }
}
