package tc4revival.content;

import java.util.EnumMap;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tc4revival.TC4Revival;

public  class TCItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TC4Revival.MOD_ID);
    public static final RegistryObject<BoneBowItem> BONE_BOW=ITEMS.register("bone_bow",BoneBowItem::new);
    public static final RegistryObject<PrimalCrusherItem> PRIMAL_CRUSHER=ITEMS.register("primal_crusher",PrimalCrusherItem::new);
    public static final java.util.List<RegistryObject<PrimalArrowItem>> PRIMAL_ARROWS=java.util.stream.IntStream.range(0,6)
            .mapToObj(element->ITEMS.register("primal_arrow_"+ShardType.values()[element].material(),()->new PrimalArrowItem(element))).toList();
    public static final RegistryObject<Item> THAUMIUM_BLOCK=ITEMS.register("thaumium_block",()->new net.minecraft.world.item.BlockItem(TCArcane.THAUMIUM_BLOCK.get(),new Item.Properties()));
    public static final RegistryObject<Item> GOLEM_FETTER=ITEMS.register("golem_fetter",()->new net.minecraft.world.item.BlockItem(TCArcane.GOLEM_FETTER.get(),new Item.Properties()));
    private static final EnumMap<GolemMaterial,RegistryObject<GolemPlacerItem>> GOLEMS=new EnumMap<>(GolemMaterial.class);
    private static final EnumMap<GolemCoreType,RegistryObject<GolemComponentItem>> GOLEM_CORES=new EnumMap<>(GolemCoreType.class);
    private static final java.util.List<RegistryObject<GolemComponentItem>> GOLEM_UPGRADES=new java.util.ArrayList<>(),GOLEM_DECORATIONS=new java.util.ArrayList<>();
    static {
        for (var material:GolemMaterial.values()) GOLEMS.put(material,ITEMS.register("golem_"+material.name().toLowerCase(java.util.Locale.ROOT),()->new GolemPlacerItem(new Item.Properties(),material,TCEntities.GOLEM)));
        for (var core:GolemCoreType.values()) GOLEM_CORES.put(core,ITEMS.register("golem_core_"+core.name().toLowerCase(java.util.Locale.ROOT),()->new GolemComponentItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),GolemComponentItem.Kind.CORE,core.ordinal())));
        String[] elements={"air","earth","fire","water","order","entropy"},decorations={"hat","glasses","bowtie","fez","dart","visor","armor","mace"};
        for (int i=0;i<elements.length;i++) { final int index=i; GOLEM_UPGRADES.add(ITEMS.register("golem_upgrade_"+elements[i],()->new GolemComponentItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),GolemComponentItem.Kind.UPGRADE,index))); }
        for (int i=0;i<decorations.length;i++) { final int index=i; GOLEM_DECORATIONS.add(ITEMS.register("golem_decoration_"+decorations[i],()->new GolemComponentItem(new Item.Properties(),GolemComponentItem.Kind.DECORATION,index))); }
    }
    public static final RegistryObject<GolemComponentItem> GOLEM_CORE_BLANK=ITEMS.register("golem_core_blank",()->new GolemComponentItem(new Item.Properties(),GolemComponentItem.Kind.CORE,100));
    public static final RegistryObject<GolemBellItem> GOLEM_BELL=ITEMS.register("golem_bell",()->new GolemBellItem(new Item.Properties(),TCItems::golem,TCItems::golemCore,TCItems::golemUpgrade));
    public static final RegistryObject<TravelingTrunkItem> TRAVELING_TRUNK=ITEMS.register("traveling_trunk",()->new TravelingTrunkItem(new Item.Properties()));
    public static final RegistryObject<LegacyWoodBlockItem> MAGIC_BOX=ITEMS.register("magic_box",()->new LegacyWoodBlockItem(TCArcane.MAGIC_BOX.get(),new Item.Properties()));
    public static final RegistryObject<Item> THAUMIUM_CHEST = ITEMS.register("thaumium_chest", () -> new net.minecraft.world.item.BlockItem(TCArcane.THAUMIUM_CHEST.get(), new Item.Properties()));
    public static final RegistryObject<Item> VOID_CHEST = ITEMS.register("void_chest", () -> new net.minecraft.world.item.BlockItem(TCArcane.VOID_CHEST.get(), new Item.Properties()));
    public static final RegistryObject<HungryChestItem> HUNGRY_CHEST=ITEMS.register("hungry_chest",()->new HungryChestItem(TCArcane.HUNGRY_CHEST.get(),new Item.Properties()));
    public static GolemPlacerItem golem(GolemMaterial material) { return GOLEMS.get(material).get(); }
    public static GolemComponentItem golemCore(int index) { return index==100?GOLEM_CORE_BLANK.get():GOLEM_CORES.get(GolemCoreType.fromLegacy(index)).get(); }
    public static GolemComponentItem golemUpgrade(int index) { return GOLEM_UPGRADES.get(index).get(); }
    public static GolemComponentItem golemDecoration(int index) { return GOLEM_DECORATIONS.get(index).get(); }
    public static final RegistryObject<Item> ELEMENTAL_PICKAXE=ITEMS.register("elemental_pickaxe",ElementalPickaxeItem::new);
    public static final RegistryObject<Item> ELEMENTAL_HOE=ITEMS.register("elemental_hoe",ElementalHoeItem::new);
    public static final RegistryObject<Item> ELEMENTAL_AXE=ITEMS.register("elemental_axe",ElementalAxeItem::new);
    public static final RegistryObject<Item> ELEMENTAL_SHOVEL=ITEMS.register("elemental_shovel",ElementalShovelItem::new);
    public static final RegistryObject<Item> ELEMENTAL_SWORD=ITEMS.register("elemental_sword",ElementalSwordItem::new);
    public static final RegistryObject<Item> ARCANE_BORE_BASE=ITEMS.register("arcane_bore_base",()->new ArcaneBoreItem(true));
    public static final RegistryObject<Item> ARCANE_BORE=ITEMS.register("arcane_bore",()->new ArcaneBoreItem(false));
    public static final RegistryObject<Item> AMBER_BLOCK=ITEMS.register("amber_block",()->new net.minecraft.world.item.BlockItem(TCArcane.AMBER_BLOCK.get(),new Item.Properties()));
    private static final java.util.EnumMap<MagicalPlantBlock.Kind,RegistryObject<net.minecraft.world.item.BlockItem>> PLANTS=new java.util.EnumMap<>(MagicalPlantBlock.Kind.class);
    static { for (var kind:MagicalPlantBlock.Kind.values()) PLANTS.put(kind,ITEMS.register(kind.path,()->new LegacyPlantItem(TCArcane.plant(kind)))); }
    public static net.minecraft.world.item.BlockItem plant(MagicalPlantBlock.Kind kind) { return PLANTS.get(kind).get(); }
    private static final java.util.Map<String,RegistryObject<net.minecraft.world.item.BlockItem>> WOODWORK=new java.util.LinkedHashMap<>();
    static { for (String name:TCArcane.woodworkNames()) WOODWORK.put(name,ITEMS.register(name,()->new LegacyWoodBlockItem(TCArcane.woodwork(name),new Item.Properties()))); }
    public static net.minecraft.world.item.BlockItem woodwork(String name) { return WOODWORK.get(name).get(); }
    public static final RegistryObject<net.minecraft.world.item.BlockItem> GREATWOOD_SAPLING=ITEMS.register("greatwood_sapling",()->new LegacyPlantItem(TCArcane.GREATWOOD_SAPLING.get()));
    public static final RegistryObject<net.minecraft.world.item.BlockItem> SILVERWOOD_SAPLING=ITEMS.register("silverwood_sapling",()->new LegacyPlantItem(TCArcane.SILVERWOOD_SAPLING.get()));
    public static final RegistryObject<net.minecraft.world.item.BlockItem> GREATWOOD_LOG=ITEMS.register("greatwood_log",()->new net.minecraft.world.item.BlockItem(TCArcane.GREATWOOD_LOG.get(),new Item.Properties()));
    public static final RegistryObject<net.minecraft.world.item.BlockItem> SILVERWOOD_LOG=ITEMS.register("silverwood_log",()->new net.minecraft.world.item.BlockItem(TCArcane.SILVERWOOD_LOG.get(),new Item.Properties()));
    public static final RegistryObject<net.minecraft.world.item.BlockItem> SILVERWOOD_NODE_LOG=ITEMS.register("silverwood_node_log",()->new LegacyWoodBlockItem(TCArcane.SILVERWOOD_NODE_LOG.get(),new Item.Properties()));
    public static final RegistryObject<net.minecraft.world.item.BlockItem> GREATWOOD_LEAVES=ITEMS.register("greatwood_leaves",()->new net.minecraft.world.item.BlockItem(TCArcane.GREATWOOD_LEAVES.get(),new Item.Properties()));
    public static final RegistryObject<net.minecraft.world.item.BlockItem> SILVERWOOD_LEAVES=ITEMS.register("silverwood_leaves",()->new net.minecraft.world.item.BlockItem(TCArcane.SILVERWOOD_LEAVES.get(),new Item.Properties()));
    private static final EnumMap<ShardType,RegistryObject<CrystalClusterItem>> CRYSTALS=new EnumMap<>(ShardType.class);
    static { for (var type:ShardType.values()) CRYSTALS.put(type,ITEMS.register(CrystalClusterBlock.path(type),()->new CrystalClusterItem(TCArcane.crystal(type)))); }
    public static CrystalClusterItem crystal(ShardType type) { return CRYSTALS.get(type).get(); }
    private static final EnumMap<ShardType, RegistryObject<Item>> SHARDS = new EnumMap<>(ShardType.class);
    private static final EnumMap<MaterialType, RegistryObject<Item>> MATERIALS = new EnumMap<>(MaterialType.class);
    private static final EnumMap<NuggetType, RegistryObject<Item>> NUGGETS = new EnumMap<>(NuggetType.class);
    private static final EnumMap<ClusterType, RegistryObject<Item>> CLUSTERS = new EnumMap<>(ClusterType.class);
    private static final EnumMap<FoodType, RegistryObject<Item>> FOODS = new EnumMap<>(FoodType.class);
    private static final EnumMap<MundaneBaubleType, RegistryObject<Item>> BAUBLES = new EnumMap<>(MundaneBaubleType.class);
    private static final EnumMap<ElementalRingType, RegistryObject<ElementalRingItem>> ELEMENTAL_RINGS = new EnumMap<>(ElementalRingType.class);
    private static final EnumMap<RunicBaubleType,RegistryObject<RunicBaubleItem>> RUNIC_BAUBLES=new EnumMap<>(RunicBaubleType.class);
    private static final EnumMap<ThaumiumArmorType, RegistryObject<Item>> THAUMIUM_ARMOR = new EnumMap<>(ThaumiumArmorType.class);
    private static final EnumMap<ThaumiumToolType, RegistryObject<Item>> THAUMIUM_TOOLS = new EnumMap<>(ThaumiumToolType.class);
    private static final EnumMap<VoidEquipment.Tool, RegistryObject<Item>> VOID_TOOLS = new EnumMap<>(VoidEquipment.Tool.class);
    private static final EnumMap<VoidEquipment.Armor, RegistryObject<Item>> VOID_ARMOR = new EnumMap<>(VoidEquipment.Armor.class);
    private static final EnumMap<LootBagItem.Kind,RegistryObject<Item>> LOOT_BAGS=new EnumMap<>(LootBagItem.Kind.class);
    public static final RegistryObject<Item> THAUMOMETER = ITEMS.register("thaumometer", ThaumometerItem::new);
    public static final RegistryObject<AlumentumItem> ALUMENTUM = ITEMS.register("alumentum",AlumentumItem::new);
    public static final RegistryObject<EssentiaResonatorItem> ESSENTIA_RESONATOR = ITEMS.register("essentia_resonator",EssentiaResonatorItem::new);
    public static final RegistryObject<Item> CRUCIBLE = ITEMS.register("crucible",()->new net.minecraft.world.item.BlockItem(TCArcane.CRUCIBLE.get(),new Item.Properties()));
    public static final RegistryObject<AlembicItem> ALEMBIC = ITEMS.register("alembic",AlembicItem::new);
    public static final RegistryObject<EssentiaReservoirItem> ESSENTIA_RESERVOIR = ITEMS.register("essentia_reservoir",EssentiaReservoirItem::new);
    private static final java.util.List<RegistryObject<Item>> CANDLES=new java.util.ArrayList<>();
    static { for (int i=0;i<16;i++) { final int color=i; CANDLES.add(ITEMS.register("tallow_candle_"+i,()->new net.minecraft.world.item.BlockItem(TCArcane.candle(color),new Item.Properties()))); } }
    public static Item candle(int color) { return CANDLES.get(color).get(); }
    public static final RegistryObject<Item> TAINT_HIVE_EGG=ITEMS.register("taint_hive_spawn_egg",()->new TaintedSpawnEggItem(TCEntities.TAINT_HIVE,0x800060));
    public static final RegistryObject<Item> TAINT_SWARM_EGG=ITEMS.register("taint_swarm_spawn_egg",()->new TaintedSpawnEggItem(TCEntities.TAINT_SWARM,0x800050));
    private static final java.util.Map<TaintedAnimalEntity.Kind,RegistryObject<Item>> TAINTED_EGGS=new java.util.EnumMap<>(TaintedAnimalEntity.Kind.class);
    static {
        for(var kind:TaintedAnimalEntity.Kind.values())TAINTED_EGGS.put(kind,ITEMS.register(kind.id()+"_spawn_egg",
                ()->new TaintedSpawnEggItem(kind)));
    }
    public static Item taintedEgg(TaintedAnimalEntity.Kind kind) { return TAINTED_EGGS.get(kind).get(); }
    public static final RegistryObject<Item> BRAINY_ZOMBIE_EGG=ITEMS.register("brainy_zombie_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(TCEntities.BRAINY_ZOMBIE,0xffc0ff,0x8000,new Item.Properties()));
    public static final RegistryObject<Item> GIANT_BRAINY_ZOMBIE_EGG=ITEMS.register("giant_brainy_zombie_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(TCEntities.GIANT_BRAINY_ZOMBIE,0xffc0ff,0x4000,new Item.Properties()));
    public static final RegistryObject<Item> WISP_EGG=ITEMS.register("wisp_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(TCEntities.WISP,0xffc0ff,0xffffff,new Item.Properties()));
    public static final RegistryObject<Item> PECH_EGG=ITEMS.register("pech_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(TCEntities.PECH,0x665544,0x224422,new Item.Properties()));
    static {
        for(var egg:LegacyMobEggs.entries())ITEMS.register(egg.path()+"_spawn_egg",
                ()->new net.minecraftforge.common.ForgeSpawnEggItem(egg.type(),egg.primary(),egg.secondary(),new Item.Properties()));
    }
    public static final RegistryObject<Item> ARCANE_DOOR=ITEMS.register("arcane_door",ArcaneDoorItem::new);
    public static final RegistryObject<Item> IRON_KEY=ITEMS.register("iron_key",()->new ArcaneKeyItem(false));
    public static final RegistryObject<Item> GOLD_KEY=ITEMS.register("gold_key",()->new ArcaneKeyItem(true));
    public static final RegistryObject<Item> ARCANE_PRESSURE_PLATE=ITEMS.register("arcane_pressure_plate",()->new LegacyWoodBlockItem(TCArcane.ARCANE_PRESSURE_PLATE.get(),new Item.Properties()));
    public static final RegistryObject<Item> WARDED_GLASS=ITEMS.register("warded_glass",()->new net.minecraft.world.item.BlockItem(TCArcane.WARDED_GLASS.get(),new Item.Properties()));
    public static final RegistryObject<Item> ADVANCED_ALCHEMICAL_CONSTRUCT=ITEMS.register("advanced_alchemical_construct",()->new net.minecraft.world.item.BlockItem(TCArcane.ADVANCED_ALCHEMICAL_CONSTRUCT.get(),new Item.Properties()));
    public static final RegistryObject<Item> ALCHEMICAL_CONSTRUCT=ITEMS.register("alchemical_construct",()->new net.minecraft.world.item.BlockItem(TCArcane.ALCHEMICAL_CONSTRUCT.get(),new Item.Properties()));
    public static final RegistryObject<Item> MNEMONIC_MATRIX=ITEMS.register("mnemonic_matrix",()->new net.minecraft.world.item.BlockItem(TCArcane.MNEMONIC_MATRIX.get(),new Item.Properties()));
    public static final RegistryObject<Item> CHAOS_MATRIX=ITEMS.register("chaos_matrix",()->new net.minecraft.world.item.BlockItem(TCArcane.CHAOS_MATRIX.get(),new Item.Properties()));
    public static final RegistryObject<PureWaterBucketItem> PURE_WATER_BUCKET=ITEMS.register("purifying_water_bucket",PureWaterBucketItem::new);
    public static final RegistryObject<LiquidDeathBucketItem> LIQUID_DEATH_BUCKET=ITEMS.register("liquid_death_bucket",LiquidDeathBucketItem::new);
    public static final RegistryObject<Item> LIQUID_DEATH_ITEM=ITEMS.register("liquid_death",()->new net.minecraft.world.item.BlockItem(TCArcane.LIQUID_DEATH.get(),new Item.Properties()));
    public static final RegistryObject<TaintBottleItem> TAINT_BOTTLE=ITEMS.register("taint_bottle",TaintBottleItem::new);
    public static final RegistryObject<EssenceCrystalItem> ESSENCE_CRYSTAL=ITEMS.register("essence_crystal",EssenceCrystalItem::new);
    public static final RegistryObject<ItemMirrorItem> ITEM_MIRROR=ITEMS.register("item_mirror",ItemMirrorItem::new);
    public static final RegistryObject<HandMirrorItem> HAND_MIRROR=ITEMS.register("hand_mirror",HandMirrorItem::new);
    public static final RegistryObject<EssentiaMirrorItem> ESSENTIA_MIRROR = ITEMS.register("essentia_mirror",EssentiaMirrorItem::new);
    public static final RegistryObject<EssentiaTubeItem> ESSENTIA_TUBE = ITEMS.register("essentia_tube",()->new EssentiaTubeItem(TCArcane.ESSENTIA_TUBE.get()));
    public static final RegistryObject<EssentiaTubeItem> ESSENTIA_VALVE = ITEMS.register("essentia_valve",()->new EssentiaTubeItem(TCArcane.ESSENTIA_VALVE.get()));
    public static final RegistryObject<EssentiaTubeItem> ESSENTIA_FILTER = ITEMS.register("essentia_filter",()->new EssentiaTubeItem(TCArcane.ESSENTIA_FILTER.get()));
    public static final RegistryObject<EssentiaTubeItem> ESSENTIA_BUFFER = ITEMS.register("essentia_buffer",()->new EssentiaTubeItem(TCArcane.ESSENTIA_BUFFER.get()));
    public static final RegistryObject<EssentiaTubeItem> ESSENTIA_RESTRICTOR = ITEMS.register("essentia_restrictor",()->new EssentiaTubeItem(TCArcane.ESSENTIA_RESTRICTOR.get()));
    public static final RegistryObject<EssentiaTubeItem> ESSENTIA_ONEWAY = ITEMS.register("essentia_oneway",()->new EssentiaTubeItem(TCArcane.ESSENTIA_ONEWAY.get()));
    public static final RegistryObject<Item> ALCHEMY_FURNACE = ITEMS.register("alchemy_furnace",()->new net.minecraft.world.item.BlockItem(TCArcane.ALCHEMY_FURNACE.get(),new Item.Properties()));
    public static final RegistryObject<Item> BELLOWS = ITEMS.register("bellows",BellowsItem::new);
    public static final RegistryObject<Item> ARCANE_PEDESTAL = ITEMS.register("arcane_pedestal",()->new net.minecraft.world.item.BlockItem(TCArcane.ARCANE_PEDESTAL.get(),new Item.Properties()));
    public static final RegistryObject<NodeJarItem> NODE_JAR = ITEMS.register("node_jar",NodeJarItem::new);
    public static final RegistryObject<CreativeAuraNodeItem> AURA_NODE = ITEMS.register("aura_node",CreativeAuraNodeItem::new);
    public static final RegistryObject<InfusionMatrixItem> INFUSION_MATRIX = ITEMS.register("infusion_matrix",InfusionMatrixItem::new);
    public static final RegistryObject<EssentiaJarItem> ESSENTIA_JAR = ITEMS.register("essentia_jar",()->new EssentiaJarItem(false,false));
    public static final RegistryObject<EssentiaJarItem> VOID_JAR = ITEMS.register("void_jar",()->new EssentiaJarItem(true,false));
    public static final RegistryObject<EssentiaJarItem> FILLED_ESSENTIA_JAR = ITEMS.register("filled_essentia_jar",()->new EssentiaJarItem(false,true));
    public static final RegistryObject<EssentiaJarItem> FILLED_VOID_JAR = ITEMS.register("filled_void_jar",()->new EssentiaJarItem(true,true));
    public static final RegistryObject<JarLabelItem> JAR_LABEL = ITEMS.register("jar_label",JarLabelItem::new);
    public static final RegistryObject<EssentiaPhialItem> EMPTY_PHIAL = ITEMS.register("empty_phial",()->new EssentiaPhialItem(false));
    public static final RegistryObject<EssentiaPhialItem> ESSENTIA_PHIAL = ITEMS.register("essentia_phial",()->new EssentiaPhialItem(true));
    public static final RegistryObject<Item> TAINT_FIBRES = ITEMS.register("taint_fibres",()->new TaintFibreItem(0));
    public static final RegistryObject<Item> TAINT_GRASS = ITEMS.register("taint_grass",()->new TaintFibreItem(1));
    public static final RegistryObject<Item> TAINT_PLANT = ITEMS.register("taint_plant",()->new TaintFibreItem(2));
    public static final RegistryObject<Item> TAINT_SPORE_STALK = ITEMS.register("taint_spore_stalk",()->new TaintFibreItem(3));
    public static final RegistryObject<Item> TAINT_SPORE_STALK_MATURE = ITEMS.register("taint_spore_stalk_mature",()->new TaintFibreItem(4));
    public static final RegistryObject<Item> TAINT_CRUST = ITEMS.register("taint_crust",()->new net.minecraft.world.item.BlockItem(TCArcane.TAINT_CRUST.get(),new Item.Properties()));
    public static final RegistryObject<Item> TAINT_SOIL = ITEMS.register("taint_soil",()->new net.minecraft.world.item.BlockItem(TCArcane.TAINT_SOIL.get(),new Item.Properties()));
    public static final RegistryObject<Item> FLESH_BLOCK = ITEMS.register("flesh_block",()->new net.minecraft.world.item.BlockItem(TCArcane.FLESH_BLOCK.get(),new Item.Properties()));
    public static final RegistryObject<GogglesItem> GOGGLES = ITEMS.register("goggles", GogglesItem::new);
    public static final RegistryObject<Item> ARCANE_LAMP=ITEMS.register("arcane_lamp",()->new net.minecraft.world.item.BlockItem(TCArcane.ARCANE_LAMP.get(),new Item.Properties()));
    public static final RegistryObject<Item> GROWTH_LAMP=ITEMS.register("growth_lamp",()->new net.minecraft.world.item.BlockItem(TCArcane.GROWTH_LAMP.get(),new Item.Properties()));
    public static final RegistryObject<Item> FERTILITY_LAMP=ITEMS.register("fertility_lamp",()->new net.minecraft.world.item.BlockItem(TCArcane.FERTILITY_LAMP.get(),new Item.Properties()));
    public static final RegistryObject<Item> LEVITATOR=ITEMS.register("levitator",()->new LegacyWoodBlockItem(TCArcane.LEVITATOR.get(),new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_EAR=ITEMS.register("arcane_ear",()->new LegacyWoodBlockItem(TCArcane.ARCANE_EAR.get(),new Item.Properties()));
    public static final RegistryObject<HoverHarnessItem> HOVER_HARNESS = ITEMS.register("hover_harness", HoverHarnessItem::new);
    public static final RegistryObject<HoverGirdleItem> HOVER_GIRDLE = ITEMS.register("hover_girdle", HoverGirdleItem::new);
    public static final RegistryObject<TravellerBootsItem> TRAVELLER_BOOTS = ITEMS.register("traveller_boots", TravellerBootsItem::new);
    public static final RegistryObject<VoidRobeArmorItem> VOID_ROBE_HELMET=ITEMS.register("void_robe_helmet",()->new VoidRobeArmorItem(net.minecraft.world.item.ArmorItem.Type.HELMET));
    public static final RegistryObject<VoidRobeArmorItem> VOID_ROBE_CHEST=ITEMS.register("void_robe_chestplate",()->new VoidRobeArmorItem(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<VoidRobeArmorItem> VOID_ROBE_LEGS=ITEMS.register("void_robe_leggings",()->new VoidRobeArmorItem(net.minecraft.world.item.ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<FortressArmorItem> FORTRESS_HELMET=ITEMS.register("fortress_helmet",()->new FortressArmorItem(net.minecraft.world.item.ArmorItem.Type.HELMET));
    public static final RegistryObject<FortressArmorItem> FORTRESS_CHESTPLATE=ITEMS.register("fortress_chestplate",()->new FortressArmorItem(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<FortressArmorItem> FORTRESS_LEGGINGS=ITEMS.register("fortress_leggings",()->new FortressArmorItem(net.minecraft.world.item.ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<RobeArmorItem> ROBE_CHEST = ITEMS.register("robe_chest", () -> new RobeArmorItem(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<RobeArmorItem> ROBE_LEGS = ITEMS.register("robe_legs", () -> new RobeArmorItem(net.minecraft.world.item.ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<RobeArmorItem> ROBE_BOOTS = ITEMS.register("robe_boots", () -> new RobeArmorItem(net.minecraft.world.item.ArmorItem.Type.BOOTS));
    public static final RegistryObject<CastingWandItem> CASTING_WAND = ITEMS.register("casting_wand", CastingWandItem::new);
    private static final java.util.Map<String, RegistryObject<Item>> WAND_COMPONENTS = new java.util.LinkedHashMap<>();
    public static final RegistryObject<Item> THAUMONOMICON = ITEMS.register("thaumonomicon", () -> new ThaumonomiconItem(false));
    public static final RegistryObject<Item> CHEAT_THAUMONOMICON = ITEMS.register("cheat_thaumonomicon", () -> new ThaumonomiconItem(true));
    public static final RegistryObject<Item> KNOWLEDGE_FRAGMENT = ITEMS.register("knowledge_fragment", KnowledgeFragmentItem::new);
    public static final RegistryObject<ManaBeanItem> MANA_BEAN = ITEMS.register("mana_bean", ManaBeanItem::new);
    public static final RegistryObject<TaintedResourceItem> TAINTED_GOO = ITEMS.register("tainted_goo", TaintedResourceItem::new);
    public static final RegistryObject<TaintedResourceItem> TAINTED_TENDRIL = ITEMS.register("tainted_tendril", TaintedResourceItem::new);
    public static final RegistryObject<Item> PRIMAL_CHARM = ITEMS.register("primal_charm", PrimalCharmItem::new);
    public static final RegistryObject<Item> PRIMORDIAL_PEARL = ITEMS.register("primordial_pearl", PrimordialPearlItem::new);
    public static final RegistryObject<Item> FOCUS_POUCH = ITEMS.register("focus_pouch", FocusPouchItem::new);
    public static final RegistryObject<ShockFocusItem> SHOCK_FOCUS = ITEMS.register("focus_shock", ShockFocusItem::new);
    public static final RegistryObject<FireFocusItem> FIRE_FOCUS = ITEMS.register("focus_fire", FireFocusItem::new);
    public static final RegistryObject<FrostFocusItem> FROST_FOCUS = ITEMS.register("focus_frost", FrostFocusItem::new);
    public static final RegistryObject<ExcavationFocusItem> EXCAVATION_FOCUS = ITEMS.register("focus_excavation", ExcavationFocusItem::new);
    public static final RegistryObject<PechFocusItem> PECH_FOCUS = ITEMS.register("focus_pech", PechFocusItem::new);
    public static final RegistryObject<PortableHoleFocusItem> PORTABLE_HOLE_FOCUS = ITEMS.register("focus_portable_hole", PortableHoleFocusItem::new);
    public static final RegistryObject<TradeFocusItem> TRADE_FOCUS = ITEMS.register("focus_trade", TradeFocusItem::new);
    public static final RegistryObject<WardingFocusItem> WARDING_FOCUS = ITEMS.register("focus_warding", WardingFocusItem::new);
    public static final RegistryObject<HellbatFocusItem> HELLBAT_FOCUS = ITEMS.register("focus_hellbat", HellbatFocusItem::new);
    public static final RegistryObject<PrimalFocusItem> PRIMAL_FOCUS = ITEMS.register("focus_primal",PrimalFocusItem::new);
    public static final RegistryObject<Item> FOCAL_MANIPULATOR = ITEMS.register("focal_manipulator", FocalManipulatorItem::new);
    public static final RegistryObject<Item> ETHEREAL_BLOOM=ITEMS.register("ethereal_bloom",()->new net.minecraft.world.item.BlockItem(TCArcane.ETHEREAL_BLOOM.get(),new Item.Properties()));
    public static final RegistryObject<Item> ALCHEMY_CENTRIFUGE=ITEMS.register("alchemy_centrifuge",AlchemyCentrifugeItem::new);
    public static final RegistryObject<Item> ESSENTIA_CRYSTALLIZER=ITEMS.register("essentia_crystallizer",EssentiaCrystallizerItem::new);
    public static final RegistryObject<Item> VIS_RELAY = ITEMS.register("vis_relay", () -> new VisRelayItem(false));
    public static final RegistryObject<Item> WORKBENCH_CHARGER = ITEMS.register("workbench_charger", () -> new VisRelayItem(true));
    public static final RegistryObject<Item> NODE_STABILIZER = ITEMS.register("node_stabilizer", () -> new NodeDeviceItem(TCArcane.NODE_STABILIZER.get()));
    public static final RegistryObject<Item> ADVANCED_NODE_STABILIZER = ITEMS.register("advanced_node_stabilizer", () -> new NodeDeviceItem(TCArcane.ADVANCED_NODE_STABILIZER.get()));
    public static final RegistryObject<Item> NODE_CONVERTER = ITEMS.register("node_converter", () -> new NodeDeviceItem(TCArcane.NODE_CONVERTER.get()));
    public static final RegistryObject<BrainJarItem> BRAIN_JAR=ITEMS.register("brain_jar",BrainJarItem::new);
    public static final RegistryObject<WispEssenceItem> WISP_ESSENCE = ITEMS.register("wisp_essence", WispEssenceItem::new);
    public static final RegistryObject<Item> NITOR = ITEMS.register("nitor", () -> new net.minecraft.world.item.BlockItem(TCArcane.NITOR.get(), new Item.Properties()));
    public static final RegistryObject<VisAmuletItem> LESSER_VIS_AMULET = ITEMS.register("lesser_vis_amulet", () -> new VisAmuletItem(false));
    public static final RegistryObject<VisAmuletItem> VIS_AMULET = ITEMS.register("vis_amulet", () -> new VisAmuletItem(true));
    public static final RegistryObject<Item> WAND_PEDESTAL = ITEMS.register("wand_pedestal", () -> new net.minecraft.world.item.BlockItem(TCArcane.WAND_PEDESTAL.get(), new Item.Properties()));
    public static final RegistryObject<Item> WAND_PEDESTAL_FOCUS = ITEMS.register("wand_pedestal_focus", () -> new net.minecraft.world.item.BlockItem(TCArcane.WAND_PEDESTAL_FOCUS.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_STONE = ITEMS.register("arcane_stone", () -> new net.minecraft.world.item.BlockItem(TCArcane.ARCANE_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_STONE_BRICKS = ITEMS.register("arcane_stone_bricks", () -> new net.minecraft.world.item.BlockItem(TCArcane.ARCANE_STONE_BRICKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_STONE_SLAB = ITEMS.register("arcane_stone_slab", () -> new net.minecraft.world.item.BlockItem(TCArcane.ARCANE_STONE_SLAB.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_STONE_STAIRS = ITEMS.register("arcane_stone_stairs", () -> new net.minecraft.world.item.BlockItem(TCArcane.ARCANE_STONE_STAIRS.get(), new Item.Properties()));
    public static final RegistryObject<Item> ZOMBIE_BRAIN = ITEMS.register("zombie_brain", ZombieBrainItem::new);
    public static final RegistryObject<Item> RESEARCH_NOTES = ITEMS.register("research_notes", () -> new ResearchNotesItem(false, false));
    public static final RegistryObject<Item> UNKNOWN_RESEARCH_NOTES = ITEMS.register("unknown_research_notes", () -> new ResearchNotesItem(true, false));
    public static final RegistryObject<Item> DISCOVERY = ITEMS.register("discovery", () -> new ResearchNotesItem(false, true));
    public static final RegistryObject<Item> TABLE = ITEMS.register("table", () -> new TableItem(false));
    public static final RegistryObject<Item> SANITY_CHECKER=ITEMS.register("sanity_checker",()->new Item(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SINISTER_STONE=ITEMS.register("sinister_stone",SinisterStoneItem::new);
    public static final RegistryObject<Item> TRAVEL_PAVING=ITEMS.register("travel_paving_stone",()->new net.minecraft.world.item.BlockItem(TCArcane.TRAVEL_PAVING.get(),new Item.Properties()));
    static { for(int i=0;i<8;i++){final int metadata=i;ITEMS.register(LegacyOreBlock.path(i),()->new net.minecraft.world.item.BlockItem(TCArcane.ore(metadata),new Item.Properties()));} }
    static { for(int i=0;i<8;i++){final int metadata=i;ITEMS.register(DeepslateOreBlock.name(i),()->new DeepslateOreItem(TCArcane.deepslateOre(metadata)));} }
    public static final RegistryObject<Item> OBSIDIAN_NODE_TOTEM=ITEMS.register("obsidian_node_totem",()->new net.minecraft.world.item.BlockItem(TCArcane.OBSIDIAN_NODE_TOTEM.get(),new Item.Properties()));
    public static final RegistryObject<Item> OBSIDIAN_TOTEM=ITEMS.register("obsidian_totem",()->new net.minecraft.world.item.BlockItem(TCArcane.OBSIDIAN_TOTEM.get(),new Item.Properties()));
    public static final RegistryObject<Item> METAL_GRATE=ITEMS.register("metal_grate",()->new net.minecraft.world.item.BlockItem(TCArcane.METAL_GRATE.get(),new Item.Properties()));
    public static final RegistryObject<Item> WARDING_PAVING=ITEMS.register("warding_paving_stone",()->new net.minecraft.world.item.BlockItem(TCArcane.WARDING_PAVING.get(),new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_SPA = ITEMS.register("arcane_spa", ()->new net.minecraft.world.item.BlockItem(TCArcane.ARCANE_SPA.get(),new Item.Properties()));
    public static final RegistryObject<Item> FLUX_SCRUBBER = ITEMS.register("flux_scrubber", FluxScrubberItem::new);
    public static final RegistryObject<Item> DECONSTRUCTION_TABLE = ITEMS.register("deconstruction_table", DeconstructionTableItem::new);
    public static final RegistryObject<Item> ARCANE_WORKBENCH = ITEMS.register("arcane_workbench", ArcaneWorkbenchItem::new);
    public static final RegistryObject<Item> RESEARCH_TABLE = ITEMS.register("research_table", () -> new TableItem(true));
    public static final RegistryObject<Item> SCRIBING_TOOLS = ITEMS.register("scribing_tools", ScribingToolsItem::new);
    static {
        for (var type:RunicBaubleType.values()) RUNIC_BAUBLES.put(type,ITEMS.register(type.path(),()->new RunicBaubleItem(type)));
        for (var component : WandComponent.all()) WAND_COMPONENTS.put(component.path(), ITEMS.register(component.path(), () -> new Item(new Item.Properties())));
        for (var type : ThaumiumToolType.values()) THAUMIUM_TOOLS.put(type, ITEMS.register(type.path(), () -> ThaumiumTools.create(type)));
        for (var type : ThaumiumArmorType.values()) THAUMIUM_ARMOR.put(type, ITEMS.register(type.path(), () -> new ThaumiumArmorItem(type)));
        for (var type : VoidEquipment.Tool.values()) VOID_TOOLS.put(type, ITEMS.register(type.path, () -> VoidTools.create(type)));
        for (var type : VoidEquipment.Armor.values()) VOID_ARMOR.put(type, ITEMS.register(type.path, () -> new VoidArmorItem(type)));
        for (var kind:LootBagItem.Kind.values()) LOOT_BAGS.put(kind,ITEMS.register(kind.path,()->new LootBagItem(kind)));
        for (var type : MundaneBaubleType.values()) BAUBLES.put(type, ITEMS.register(type.path(), MundaneBaubleItem::new));
        for (var type : ElementalRingType.values()) ELEMENTAL_RINGS.put(type, ITEMS.register(type.path(), () -> new ElementalRingItem(type)));
        for (var type : FoodType.values()) FOODS.put(type, ITEMS.register(type.path(), type::create));
        for (var type : ClusterType.values()) CLUSTERS.put(type, ITEMS.register(type.path(), () -> new Item(new Item.Properties().stacksTo(64))));
        for (var type : NuggetType.values()) NUGGETS.put(type, ITEMS.register(type.path(), () -> new Item(new Item.Properties().stacksTo(64))));
        for (var type : ShardType.values()) SHARDS.put(type, ITEMS.register(type.path(), () -> new Item(new Item.Properties().stacksTo(64))));
        for (var type : MaterialType.values()) MATERIALS.put(type, ITEMS.register(type.path(), () -> new MaterialItem(type)));
    }
    private TCItems() {}
    public static void register(IEventBus bus) { ITEMS.register(bus); }
    public static Item shard(ShardType type) { return SHARDS.get(type).get(); }
    public static Item wandComponent(String path) { return WAND_COMPONENTS.get(path).get(); }
    public static Item material(MaterialType type) { return MATERIALS.get(type).get(); }
    public static Item nugget(NuggetType type) { return NUGGETS.get(type).get(); }
    public static Item cluster(ClusterType type) { return CLUSTERS.get(type).get(); }
    public static Item food(FoodType type) { return FOODS.get(type).get(); }
    public static Item bauble(MundaneBaubleType type) { return BAUBLES.get(type).get(); }
    public static ElementalRingItem elementalRing(ElementalRingType type) { return ELEMENTAL_RINGS.get(type).get(); }
    public static RunicBaubleItem runicBauble(RunicBaubleType type) { return RUNIC_BAUBLES.get(type).get(); }
    public static Item armor(ThaumiumArmorType type) { return THAUMIUM_ARMOR.get(type).get(); }
    public static Item tool(ThaumiumToolType type) { return THAUMIUM_TOOLS.get(type).get(); }
    public static Item voidTool(VoidEquipment.Tool type) { return VOID_TOOLS.get(type).get(); }
    public static Item voidArmor(VoidEquipment.Armor type) { return VOID_ARMOR.get(type).get(); }
    public static Item lootBag(LootBagItem.Kind kind) { return LOOT_BAGS.get(kind).get(); }
}
