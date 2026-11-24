package tc4revival.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public  class TCArcane {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "thaumcraft");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "thaumcraft");
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "thaumcraft");
    public static final RegistryObject<MagicBoxBlock> MAGIC_BOX=BLOCKS.register("magic_box",MagicBoxBlock::new);
    public static final RegistryObject<BlockEntityType<MagicBoxBlockEntity>> MAGIC_BOX_ENTITY=ENTITIES.register("magic_box",()->BlockEntityType.Builder.of(MagicBoxBlockEntity::new,MAGIC_BOX.get()).build(null));
    public static final RegistryObject<MenuType<MagicBoxMenu>> MAGIC_BOX_MENU=MENUS.register("magic_box",()->IForgeMenuType.create(MagicBoxMenu::new));
    public static final RegistryObject<TravelPavingBlock> TRAVEL_PAVING=BLOCKS.register("travel_paving_stone",TravelPavingBlock::new);
    private static final java.util.List<RegistryObject<LegacyOreBlock>> ORES=new java.util.ArrayList<>();
    static { for(int i=0;i<8;i++){final int metadata=i;ORES.add(BLOCKS.register(LegacyOreBlock.path(i),()->new LegacyOreBlock(metadata)));} }
    public static LegacyOreBlock ore(int metadata){return ORES.get(metadata).get();}
    private static final java.util.List<RegistryObject<DeepslateOreBlock>> DEEPSLATE_ORES=new java.util.ArrayList<>();
    static {for(int i=0;i<8;i++){final int metadata=i;DEEPSLATE_ORES.add(BLOCKS.register(DeepslateOreBlock.name(i),()->new DeepslateOreBlock(metadata)));}}
    public static DeepslateOreBlock deepslateOre(int metadata){return DEEPSLATE_ORES.get(metadata).get();}
    public static final RegistryObject<ObsidianTotemBlock> OBSIDIAN_TOTEM=BLOCKS.register("obsidian_totem",()->new ObsidianTotemBlock(false));
    public static final RegistryObject<ObsidianTotemBlock> OBSIDIAN_NODE_TOTEM=BLOCKS.register("obsidian_node_totem",()->new ObsidianTotemBlock(true));
    public static final RegistryObject<MetalGrateBlock> METAL_GRATE=BLOCKS.register("metal_grate",MetalGrateBlock::new);
    public static final RegistryObject<BlockEntityType<MetalGrateBlockEntity>> METAL_GRATE_ENTITY=ENTITIES.register("metal_grate",()->BlockEntityType.Builder.of(MetalGrateBlockEntity::new,METAL_GRATE.get()).build(null));
    public static final RegistryObject<WardingPavingBlock> WARDING_PAVING=BLOCKS.register("warding_paving_stone",WardingPavingBlock::new);
    public static final RegistryObject<WardingBarrierBlock> WARDING_BARRIER=BLOCKS.register("warding_barrier",WardingBarrierBlock::new);
    public static final RegistryObject<BlockEntityType<WardingBarrierBlockEntity>> WARDING_BARRIER_ENTITY=ENTITIES.register("warding_barrier",()->BlockEntityType.Builder.of(WardingBarrierBlockEntity::new,WARDING_BARRIER.get()).build(null));
    public static final RegistryObject<BlockEntityType<WardingPavingBlockEntity>> WARDING_PAVING_ENTITY=ENTITIES.register("warding_paving_stone",()->BlockEntityType.Builder.of(WardingPavingBlockEntity::new,WARDING_PAVING.get()).build(null));
    public static final RegistryObject<ArcaneSpaBlock> ARCANE_SPA=BLOCKS.register("arcane_spa",ArcaneSpaBlock::new);
    public static final RegistryObject<BlockEntityType<ArcaneSpaBlockEntity>> ARCANE_SPA_ENTITY=ENTITIES.register("arcane_spa",()->BlockEntityType.Builder.of(ArcaneSpaBlockEntity::new,ARCANE_SPA.get()).build(null));
    public static final RegistryObject<MenuType<ArcaneSpaMenu>> ARCANE_SPA_MENU=MENUS.register("arcane_spa",()->IForgeMenuType.create(ArcaneSpaMenu::new));
    public static final RegistryObject<FluxScrubberBlock> FLUX_SCRUBBER=BLOCKS.register("flux_scrubber",FluxScrubberBlock::new);
    public static final RegistryObject<BlockEntityType<FluxScrubberBlockEntity>> FLUX_SCRUBBER_ENTITY=ENTITIES.register("flux_scrubber",()->BlockEntityType.Builder.of(FluxScrubberBlockEntity::new,FLUX_SCRUBBER.get()).build(null));
    public static final RegistryObject<DeconstructionTableBlock> DECONSTRUCTION_TABLE=BLOCKS.register("deconstruction_table",DeconstructionTableBlock::new);
    public static final RegistryObject<BlockEntityType<DeconstructionTableBlockEntity>> DECONSTRUCTION_TABLE_ENTITY=ENTITIES.register("deconstruction_table",()->BlockEntityType.Builder.of(DeconstructionTableBlockEntity::new,DECONSTRUCTION_TABLE.get()).build(null));
    public static final RegistryObject<MenuType<DeconstructionTableMenu>> DECONSTRUCTION_TABLE_MENU=MENUS.register("deconstruction_table",()->IForgeMenuType.create(DeconstructionTableMenu::new));
    public static final RegistryObject<Block> THAUMIUM_BLOCK=BLOCKS.register("thaumium_block",()->new Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(4,6).requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.STONE)));
    public static final RegistryObject<GolemFetterBlock> GOLEM_FETTER=BLOCKS.register("golem_fetter",GolemFetterBlock::new);
    public static final RegistryObject<StorageChestBlock> THAUMIUM_CHEST = BLOCKS.register("thaumium_chest", () -> new StorageChestBlock(54));
    public static final RegistryObject<StorageChestBlock> VOID_CHEST = BLOCKS.register("void_chest", () -> new StorageChestBlock(81));
    public static final RegistryObject<BlockEntityType<StorageChestBlockEntity>> STORAGE_CHEST_ENTITY = ENTITIES.register("storage_chest",
            () -> BlockEntityType.Builder.of(StorageChestBlockEntity::new, THAUMIUM_CHEST.get(), VOID_CHEST.get()).build(null));
    public static final java.util.Map<Integer, RegistryObject<MenuType<StorageChestMenu>>> STORAGE_CHEST_MENUS = new java.util.LinkedHashMap<>();
    static {
        for (int size : new int[]{54, 81, 108, 162})
            STORAGE_CHEST_MENUS.put(size, MENUS.register("storage_chest_" + size, () -> IForgeMenuType.create((id, inv, data) ->
                    new StorageChestMenu(id, inv, new net.minecraft.world.SimpleContainer(size)))));
    }
    public static MenuType<StorageChestMenu> storageChestMenu(int size) { return STORAGE_CHEST_MENUS.get(size).get(); }
    public static final RegistryObject<HungryChestBlock> HUNGRY_CHEST=BLOCKS.register("hungry_chest",HungryChestBlock::new);
    public static final RegistryObject<BlockEntityType<HungryChestBlockEntity>> HUNGRY_CHEST_ENTITY=ENTITIES.register("hungry_chest",()->BlockEntityType.Builder.of(HungryChestBlockEntity::new,HUNGRY_CHEST.get()).build(null));
    public static final RegistryObject<ArcaneBoreBlock> ARCANE_BORE_BASE=BLOCKS.register("arcane_bore_base",()->new ArcaneBoreBlock(true));
    public static final RegistryObject<ArcaneBoreBlock> ARCANE_BORE=BLOCKS.register("arcane_bore",()->new ArcaneBoreBlock(false));
    public static final RegistryObject<BlockEntityType<ArcaneBoreBaseBlockEntity>> ARCANE_BORE_BASE_ENTITY=ENTITIES.register("arcane_bore_base",()->BlockEntityType.Builder.of(ArcaneBoreBaseBlockEntity::new,ARCANE_BORE_BASE.get()).build(null));
    public static final RegistryObject<BlockEntityType<ArcaneBoreBlockEntity>> ARCANE_BORE_ENTITY=ENTITIES.register("arcane_bore",()->BlockEntityType.Builder.of(ArcaneBoreBlockEntity::new,ARCANE_BORE.get()).build(null));
    public static final RegistryObject<MenuType<ArcaneBoreMenu>> ARCANE_BORE_MENU=MENUS.register("arcane_bore",()->IForgeMenuType.create(ArcaneBoreMenu::new));
    public static final RegistryObject<Block> AMBER_BLOCK=BLOCKS.register("amber_block",()->new Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(1.5f,3).sound(net.minecraft.world.level.block.SoundType.STONE)));
    public static final RegistryObject<ArcaneLampBlock> ARCANE_LAMP=BLOCKS.register("arcane_lamp",()->new ArcaneLampBlock(ArcaneLampBlock.Kind.LIGHT));
    public static final RegistryObject<ArcaneLampBlock> GROWTH_LAMP=BLOCKS.register("growth_lamp",()->new ArcaneLampBlock(ArcaneLampBlock.Kind.GROWTH));
    public static final RegistryObject<ArcaneLampBlock> FERTILITY_LAMP=BLOCKS.register("fertility_lamp",()->new ArcaneLampBlock(ArcaneLampBlock.Kind.FERTILITY));
    public static final RegistryObject<LampAirBlock> LAMP_AIR=BLOCKS.register("lamp_air",LampAirBlock::new);
    public static final RegistryObject<BlockEntityType<ArcaneLampBlockEntity>> ARCANE_LAMP_ENTITY=ENTITIES.register("arcane_lamp",()->BlockEntityType.Builder.of(ArcaneLampBlockEntity::new,ARCANE_LAMP.get(),GROWTH_LAMP.get(),FERTILITY_LAMP.get()).build(null));
    public static final RegistryObject<LevitatorBlock> LEVITATOR=BLOCKS.register("levitator",LevitatorBlock::new);
    public static final RegistryObject<ArcaneEarBlock> ARCANE_EAR=BLOCKS.register("arcane_ear",ArcaneEarBlock::new);
    public static final RegistryObject<BlockEntityType<LevitatorBlockEntity>> LEVITATOR_ENTITY=ENTITIES.register("levitator",()->BlockEntityType.Builder.of(LevitatorBlockEntity::new,LEVITATOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<ArcaneEarBlockEntity>> ARCANE_EAR_ENTITY=ENTITIES.register("arcane_ear",()->BlockEntityType.Builder.of(ArcaneEarBlockEntity::new,ARCANE_EAR.get()).build(null));
    public static final RegistryObject<MenuType<PechMenu>> PECH_MENU=MENUS.register("pech",()->IForgeMenuType.create(PechMenu::new));
    public static final RegistryObject<MenuType<GolemMenu>> GOLEM_MENU=MENUS.register("golem",()->IForgeMenuType.create(GolemMenu::new));
    public static final RegistryObject<MenuType<TravelingTrunkMenu>> TRUNK_MENU=MENUS.register("traveling_trunk",()->IForgeMenuType.create(TravelingTrunkMenu::new));
    private static final java.util.List<RegistryObject<TallowCandleBlock>> CANDLES=new java.util.ArrayList<>();
    static { for (int i=0;i<16;i++) { final int color=i; CANDLES.add(BLOCKS.register("tallow_candle_"+i,()->new TallowCandleBlock(color))); } }
    public static TallowCandleBlock candle(int color) { return CANDLES.get(color).get(); }
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, "thaumcraft");
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "thaumcraft");
    private static final java.util.EnumMap<MagicalPlantBlock.Kind,RegistryObject<MagicalPlantBlock>> PLANTS=new java.util.EnumMap<>(MagicalPlantBlock.Kind.class);
    static { for (var kind:MagicalPlantBlock.Kind.values()) PLANTS.put(kind,BLOCKS.register(kind.path,()->new MagicalPlantBlock(kind))); }
    public static MagicalPlantBlock plant(MagicalPlantBlock.Kind kind) { return PLANTS.get(kind).get(); }
    private static final java.util.Map<String,RegistryObject<? extends Block>> WOODWORK=new java.util.LinkedHashMap<>();
    static {
        for (String wood:java.util.List.of("greatwood","silverwood")) {
            var planks=BLOCKS.register(wood+"_planks",MagicalPlanksBlock::new); WOODWORK.put(wood+"_planks",planks);
            WOODWORK.put(wood+"_slab",BLOCKS.register(wood+"_slab",()->new net.minecraft.world.level.block.SlabBlock(Block.Properties.of().strength(2,3).sound(net.minecraft.world.level.block.SoundType.WOOD))));
            WOODWORK.put(wood+"_stairs",BLOCKS.register(wood+"_stairs",()->new net.minecraft.world.level.block.StairBlock(()->planks.get().defaultBlockState(),Block.Properties.of().strength(2.5f,6).sound(net.minecraft.world.level.block.SoundType.WOOD))));
        }
    }
    public static Block woodwork(String name) { return WOODWORK.get(name).get(); }
    public static java.util.Set<String> woodworkNames() { return java.util.Collections.unmodifiableSet(WOODWORK.keySet()); }
    public static final RegistryObject<MagicalLogBlock> GREATWOOD_LOG=BLOCKS.register("greatwood_log",()->new MagicalLogBlock(false));
    public static final RegistryObject<MagicalSaplingBlock> GREATWOOD_SAPLING=BLOCKS.register("greatwood_sapling",()->new MagicalSaplingBlock(false));
    public static final RegistryObject<MagicalSaplingBlock> SILVERWOOD_SAPLING=BLOCKS.register("silverwood_sapling",()->new MagicalSaplingBlock(true));
    public static final RegistryObject<MagicalLogBlock> SILVERWOOD_LOG=BLOCKS.register("silverwood_log",()->new MagicalLogBlock(false));
    public static final RegistryObject<SilverwoodNodeLogBlock> SILVERWOOD_NODE_LOG=BLOCKS.register("silverwood_node_log",SilverwoodNodeLogBlock::new);
    public static final RegistryObject<ManaPodBlock> MANA_POD=BLOCKS.register("mana_pod",ManaPodBlock::new);
    public static final RegistryObject<BlockEntityType<ManaPodBlockEntity>> MANA_POD_ENTITY=ENTITIES.register("mana_pod",
            ()->BlockEntityType.Builder.of(ManaPodBlockEntity::new,MANA_POD.get()).build(null));
    public static final RegistryObject<MagicalLeavesBlock> GREATWOOD_LEAVES=BLOCKS.register("greatwood_leaves",()->new MagicalLeavesBlock(false));
    public static final RegistryObject<MagicalLeavesBlock> SILVERWOOD_LEAVES=BLOCKS.register("silverwood_leaves",()->new MagicalLeavesBlock(true));
    private static final java.util.EnumMap<ShardType,RegistryObject<CrystalClusterBlock>> CRYSTALS=new java.util.EnumMap<>(ShardType.class);
    static { for (var type:ShardType.values()) CRYSTALS.put(type,BLOCKS.register(CrystalClusterBlock.path(type),()->new CrystalClusterBlock(type))); }
    public static CrystalClusterBlock crystal(ShardType type) { return CRYSTALS.get(type).get(); }
    public static final RegistryObject<BlockEntityType<CrystalClusterBlockEntity>> CRYSTAL_CLUSTER_ENTITY=ENTITIES.register("crystal_cluster",
            ()->BlockEntityType.Builder.of(CrystalClusterBlockEntity::new,CRYSTALS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
    public static final RegistryObject<ArcaneWorkbenchBlock> BLOCK = BLOCKS.register("arcane_workbench", ArcaneWorkbenchBlock::new);
    public static final RegistryObject<ShockFieldBlock> SHOCK_FIELD = BLOCKS.register("shock_field", ShockFieldBlock::new);
    public static final RegistryObject<FluxGasBlock> FLUX_GAS = BLOCKS.register("flux_gas", FluxGasBlock::new);
    public static final RegistryObject<FluxGooBlock> FLUX_GOO = BLOCKS.register("flux_goo", FluxGooBlock::new);
    public static final RegistryObject<TaintFibreBlock> TAINT_FIBRES = BLOCKS.register("taint_fibres",TaintFibreBlock::new);
    public static final RegistryObject<TaintSolidBlock> TAINT_CRUST = BLOCKS.register("taint_crust",()->new TaintSolidBlock(TaintSolidBlock.Kind.CRUST));
    public static final RegistryObject<TaintSolidBlock> TAINT_SOIL = BLOCKS.register("taint_soil",()->new TaintSolidBlock(TaintSolidBlock.Kind.SOIL));
    public static final RegistryObject<TaintSolidBlock> FLESH_BLOCK = BLOCKS.register("flesh_block",()->new TaintSolidBlock(TaintSolidBlock.Kind.FLESH));
    public static final RegistryObject<Block> ADVANCED_ALCHEMICAL_CONSTRUCT=BLOCKS.register("advanced_alchemical_construct",()->new Block(Block.Properties.of().strength(3,17).sound(net.minecraft.world.level.block.SoundType.METAL)));
    public static final RegistryObject<AdvancedAlchemyBlock> ADVANCED_ALCHEMY_FURNACE=BLOCKS.register("advanced_alchemy_furnace",AdvancedAlchemyBlock::new);
    public static final RegistryObject<BlockEntityType<AdvancedAlchemyBlockEntity>> ADVANCED_ALCHEMY_ENTITY=ENTITIES.register("advanced_alchemy_furnace",()->BlockEntityType.Builder.of(AdvancedAlchemyBlockEntity::new,ADVANCED_ALCHEMY_FURNACE.get()).build(null));
    public static final RegistryObject<Block> ALCHEMICAL_CONSTRUCT=BLOCKS.register("alchemical_construct",()->new Block(Block.Properties.of().strength(3,17).sound(net.minecraft.world.level.block.SoundType.METAL)));
    public static final RegistryObject<MnemonicMatrixBlock> MNEMONIC_MATRIX=BLOCKS.register("mnemonic_matrix",MnemonicMatrixBlock::new);
    public static final RegistryObject<MnemonicMatrixBlock> CHAOS_MATRIX=BLOCKS.register("chaos_matrix",MnemonicMatrixBlock::new);
    public static final RegistryObject<ThaumatoriumBlock> THAUMATORIUM=BLOCKS.register("thaumatorium",ThaumatoriumBlock::new);
    public static final RegistryObject<BlockEntityType<ThaumatoriumBlockEntity>> THAUMATORIUM_ENTITY=ENTITIES.register("thaumatorium",()->BlockEntityType.Builder.of(ThaumatoriumBlockEntity::new,THAUMATORIUM.get()).build(null));
    public static final RegistryObject<MenuType<ThaumatoriumMenu>> THAUMATORIUM_MENU=MENUS.register("thaumatorium",()->IForgeMenuType.create(ThaumatoriumMenu::new));
    public static final RegistryObject<InfernalFurnaceBlock> INFERNAL_FURNACE=BLOCKS.register("infernal_furnace",InfernalFurnaceBlock::new);
    public static final RegistryObject<BlockEntityType<InfernalFurnaceBlockEntity>> INFERNAL_FURNACE_ENTITY=ENTITIES.register("infernal_furnace",
            ()->BlockEntityType.Builder.of(InfernalFurnaceBlockEntity::new,INFERNAL_FURNACE.get()).build(null));
    public static final RegistryObject<CrucibleBlock> CRUCIBLE = BLOCKS.register("crucible",CrucibleBlock::new);
    public static final RegistryObject<AlembicBlock> ALEMBIC = BLOCKS.register("alembic",AlembicBlock::new);
    public static final RegistryObject<LiquidDeathBlock> LIQUID_DEATH=BLOCKS.register("liquid_death",LiquidDeathBlock::new);
    public static final RegistryObject<ItemMirrorBlock> ITEM_MIRROR=BLOCKS.register("item_mirror",ItemMirrorBlock::new);
    public static final RegistryObject<BlockEntityType<ItemMirrorBlockEntity>> ITEM_MIRROR_ENTITY=ENTITIES.register("item_mirror",()->BlockEntityType.Builder.of(ItemMirrorBlockEntity::new,ITEM_MIRROR.get()).build(null));
    public static final RegistryObject<MenuType<HandMirrorMenu>> HAND_MIRROR_MENU=MENUS.register("hand_mirror",()->IForgeMenuType.create(HandMirrorMenu::new));
    public static final RegistryObject<EssentiaMirrorBlock> ESSENTIA_MIRROR = BLOCKS.register("essentia_mirror",EssentiaMirrorBlock::new);
    public static final RegistryObject<BlockEntityType<EssentiaMirrorBlockEntity>> ESSENTIA_MIRROR_ENTITY = ENTITIES.register("essentia_mirror",()->BlockEntityType.Builder.of(EssentiaMirrorBlockEntity::new,ESSENTIA_MIRROR.get()).build(null));
    public static final RegistryObject<EssentiaReservoirBlock> ESSENTIA_RESERVOIR = BLOCKS.register("essentia_reservoir",EssentiaReservoirBlock::new);
    public static final RegistryObject<BlockEntityType<EssentiaReservoirBlockEntity>> ESSENTIA_RESERVOIR_ENTITY = ENTITIES.register("essentia_reservoir",()->BlockEntityType.Builder.of(EssentiaReservoirBlockEntity::new,ESSENTIA_RESERVOIR.get()).build(null));
    public static final RegistryObject<EssentiaTubeBlock> ESSENTIA_TUBE = tube("essentia_tube",EssentiaTubeFlow.Kind.NORMAL);
    public static final RegistryObject<EssentiaTubeBlock> ESSENTIA_VALVE = tube("essentia_valve",EssentiaTubeFlow.Kind.VALVE);
    public static final RegistryObject<EssentiaTubeBlock> ESSENTIA_FILTER = tube("essentia_filter",EssentiaTubeFlow.Kind.FILTER);
    public static final RegistryObject<EssentiaTubeBlock> ESSENTIA_BUFFER = tube("essentia_buffer",EssentiaTubeFlow.Kind.BUFFER);
    public static final RegistryObject<EssentiaTubeBlock> ESSENTIA_RESTRICTOR = tube("essentia_restrictor",EssentiaTubeFlow.Kind.RESTRICTED);
    public static final RegistryObject<EssentiaTubeBlock> ESSENTIA_ONEWAY = tube("essentia_oneway",EssentiaTubeFlow.Kind.ONEWAY);
    public static final RegistryObject<BlockEntityType<EssentiaTubeBlockEntity>> ESSENTIA_TUBE_ENTITY = ENTITIES.register("essentia_tube",()->BlockEntityType.Builder.of(EssentiaTubeBlockEntity::new,essentiaTubes().toArray(Block[]::new)).build(null));
    private static RegistryObject<EssentiaTubeBlock> tube(String name,EssentiaTubeFlow.Kind kind) { return BLOCKS.register(name,()->new EssentiaTubeBlock(kind)); }
    public static java.util.List<EssentiaTubeBlock> essentiaTubes() { return java.util.List.of(ESSENTIA_TUBE.get(),ESSENTIA_VALVE.get(),ESSENTIA_FILTER.get(),ESSENTIA_BUFFER.get(),ESSENTIA_RESTRICTOR.get(),ESSENTIA_ONEWAY.get()); }
    public static final RegistryObject<AlchemyFurnaceBlock> ALCHEMY_FURNACE = BLOCKS.register("alchemy_furnace",AlchemyFurnaceBlock::new);
    public static final RegistryObject<BlockEntityType<AlembicBlockEntity>> ALEMBIC_ENTITY = ENTITIES.register("alembic",()->BlockEntityType.Builder.of(AlembicBlockEntity::new,ALEMBIC.get()).build(null));
    public static final RegistryObject<BlockEntityType<AlchemyFurnaceBlockEntity>> ALCHEMY_FURNACE_ENTITY = ENTITIES.register("alchemy_furnace",()->BlockEntityType.Builder.of(AlchemyFurnaceBlockEntity::new,ALCHEMY_FURNACE.get()).build(null));
    public static final RegistryObject<MenuType<AlchemyFurnaceMenu>> ALCHEMY_FURNACE_MENU = MENUS.register("alchemy_furnace",()->IForgeMenuType.create(AlchemyFurnaceMenu::new));
    public static final RegistryObject<BellowsBlock> BELLOWS = BLOCKS.register("bellows",BellowsBlock::new);
    public static final RegistryObject<ArcanePedestalBlock> ARCANE_PEDESTAL = BLOCKS.register("arcane_pedestal",ArcanePedestalBlock::new);
    public static final RegistryObject<InfusionMatrixBlock> INFUSION_MATRIX = BLOCKS.register("infusion_matrix",InfusionMatrixBlock::new);
    public static final RegistryObject<InfusionPillarBlock> INFUSION_PILLAR = BLOCKS.register("infusion_pillar",()->new InfusionPillarBlock(false));
    public static final RegistryObject<InfusionPillarBlock> INFUSION_PILLAR_TOP = BLOCKS.register("infusion_pillar_top",()->new InfusionPillarBlock(true));
    public static final RegistryObject<BlockEntityType<InfusionMatrixBlockEntity>> INFUSION_MATRIX_ENTITY = ENTITIES.register("infusion_matrix",
            ()->BlockEntityType.Builder.of(InfusionMatrixBlockEntity::new,INFUSION_MATRIX.get()).build(null));
    public static final RegistryObject<BlockEntityType<InfusionPillarBlockEntity>> INFUSION_PILLAR_ENTITY = ENTITIES.register("infusion_pillar",
            ()->BlockEntityType.Builder.of(InfusionPillarBlockEntity::new,INFUSION_PILLAR.get()).build(null));
    public static final RegistryObject<BlockEntityType<ArcanePedestalBlockEntity>> ARCANE_PEDESTAL_ENTITY = ENTITIES.register("arcane_pedestal",
            ()->BlockEntityType.Builder.of(ArcanePedestalBlockEntity::new,ARCANE_PEDESTAL.get()).build(null));
    public static final RegistryObject<BlockEntityType<BellowsBlockEntity>> BELLOWS_ENTITY = ENTITIES.register("bellows",
            () -> BlockEntityType.Builder.of(BellowsBlockEntity::new,BELLOWS.get()).build(null));
    public static final RegistryObject<BlockEntityType<CrucibleBlockEntity>> CRUCIBLE_ENTITY = ENTITIES.register("crucible",
            () -> BlockEntityType.Builder.of(CrucibleBlockEntity::new,CRUCIBLE.get()).build(null));
    public static final RegistryObject<PortableHoleBlock> PORTABLE_HOLE = BLOCKS.register("portable_hole", PortableHoleBlock::new);
    public static final RegistryObject<WardedGlassBlock> WARDED_GLASS=BLOCKS.register("warded_glass",WardedGlassBlock::new);
    public static final RegistryObject<ArcaneDoorBlock> ARCANE_DOOR=BLOCKS.register("arcane_door",ArcaneDoorBlock::new);
    public static final RegistryObject<ArcanePressurePlateBlock> ARCANE_PRESSURE_PLATE=BLOCKS.register("arcane_pressure_plate",ArcanePressurePlateBlock::new);
    public static final RegistryObject<BlockEntityType<OwnedDeviceBlockEntity>> OWNED_DEVICE_ENTITY=ENTITIES.register("owned_device",()->BlockEntityType.Builder.of(OwnedDeviceBlockEntity::new,WARDED_GLASS.get(),ARCANE_PRESSURE_PLATE.get(),ARCANE_DOOR.get()).build(null));
    public static final RegistryObject<WardedBlock> WARDED = BLOCKS.register("warded", WardedBlock::new);
    public static final RegistryObject<BlockEntityType<WardedBlockEntity>> WARDED_ENTITY = ENTITIES.register("warded",
            () -> BlockEntityType.Builder.of(WardedBlockEntity::new, WARDED.get()).build(null));
    public static final RegistryObject<BlockEntityType<PortableHoleBlockEntity>> PORTABLE_HOLE_ENTITY = ENTITIES.register("portable_hole",
            () -> BlockEntityType.Builder.of(PortableHoleBlockEntity::new, PORTABLE_HOLE.get()).build(null));
    public static final RegistryObject<EnergizedNodeBlock> ENERGIZED_NODE = BLOCKS.register("energized_node", EnergizedNodeBlock::new);
    public static final RegistryObject<AuraNodeBlock> AURA_NODE = BLOCKS.register("aura_node", AuraNodeBlock::new);
    public static final RegistryObject<NodeJarBlock> NODE_JAR = BLOCKS.register("node_jar",NodeJarBlock::new);
    public static final RegistryObject<BrainJarBlock> BRAIN_JAR=BLOCKS.register("brain_jar",BrainJarBlock::new);
    public static final RegistryObject<BlockEntityType<BrainJarBlockEntity>> BRAIN_JAR_ENTITY=ENTITIES.register("brain_jar",()->BlockEntityType.Builder.of(BrainJarBlockEntity::new,BRAIN_JAR.get()).build(null));
    public static final RegistryObject<EssentiaJarBlock> ESSENTIA_JAR = BLOCKS.register("essentia_jar",EssentiaJarBlock::new);
    public static final RegistryObject<EssentiaJarBlock> VOID_JAR = BLOCKS.register("void_jar",EssentiaJarBlock::new);
    public static final RegistryObject<BlockEntityType<EssentiaJarBlockEntity>> ESSENTIA_JAR_ENTITY = ENTITIES.register("essentia_jar",
            ()->BlockEntityType.Builder.of(EssentiaJarBlockEntity::new,ESSENTIA_JAR.get(),VOID_JAR.get()).build(null));
    public static final RegistryObject<BlockEntityType<NodeJarBlockEntity>> NODE_JAR_ENTITY = ENTITIES.register("node_jar",
            ()->BlockEntityType.Builder.of(NodeJarBlockEntity::new,NODE_JAR.get()).build(null));
    public static final RegistryObject<NitorBlock> NITOR = BLOCKS.register("nitor", NitorBlock::new);
    public static final RegistryObject<WandPedestalBlock> WAND_PEDESTAL = BLOCKS.register("wand_pedestal", WandPedestalBlock::new);
    public static final RegistryObject<WandPedestalFocusBlock> WAND_PEDESTAL_FOCUS = BLOCKS.register("wand_pedestal_focus", WandPedestalFocusBlock::new);
    public static final RegistryObject<BlockEntityType<WandPedestalBlockEntity>> WAND_PEDESTAL_ENTITY = ENTITIES.register("wand_pedestal",
            () -> BlockEntityType.Builder.of(WandPedestalBlockEntity::new, WAND_PEDESTAL.get()).build(null));
    public static final RegistryObject<BlockEntityType<NitorBlockEntity>> NITOR_ENTITY = ENTITIES.register("nitor",
            () -> BlockEntityType.Builder.of(NitorBlockEntity::new, NITOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<AuraNodeBlockEntity>> AURA_NODE_ENTITY = ENTITIES.register("aura_node",
            () -> BlockEntityType.Builder.of(AuraNodeBlockEntity::new, AURA_NODE.get(),SILVERWOOD_NODE_LOG.get(),OBSIDIAN_NODE_TOTEM.get()).build(null));
    public static final RegistryObject<NodeDeviceBlock> NODE_STABILIZER = BLOCKS.register("node_stabilizer", () -> new NodeDeviceBlock(false));
    public static final RegistryObject<NodeDeviceBlock> ADVANCED_NODE_STABILIZER = BLOCKS.register("advanced_node_stabilizer", () -> new NodeDeviceBlock(false));
    public static final RegistryObject<NodeDeviceBlock> NODE_CONVERTER = BLOCKS.register("node_converter", () -> new NodeDeviceBlock(true));
    public static final RegistryObject<BlockEntityType<NodeStabilizerBlockEntity>> NODE_STABILIZER_ENTITY = ENTITIES.register("node_stabilizer",
            () -> BlockEntityType.Builder.of(NodeStabilizerBlockEntity::new, NODE_STABILIZER.get(), ADVANCED_NODE_STABILIZER.get()).build(null));
    public static final RegistryObject<BlockEntityType<NodeConverterBlockEntity>> NODE_CONVERTER_ENTITY = ENTITIES.register("node_converter",
            () -> BlockEntityType.Builder.of(NodeConverterBlockEntity::new, NODE_CONVERTER.get()).build(null));
    public static final RegistryObject<EtherealBloomBlock> ETHEREAL_BLOOM=BLOCKS.register("ethereal_bloom",EtherealBloomBlock::new);
    public static final RegistryObject<BlockEntityType<EtherealBloomBlockEntity>> ETHEREAL_BLOOM_ENTITY=ENTITIES.register("ethereal_bloom",()->BlockEntityType.Builder.of(EtherealBloomBlockEntity::new,ETHEREAL_BLOOM.get()).build(null));
    public static final RegistryObject<AlchemyCentrifugeBlock> ALCHEMY_CENTRIFUGE=BLOCKS.register("alchemy_centrifuge",AlchemyCentrifugeBlock::new);
    public static final RegistryObject<BlockEntityType<AlchemyCentrifugeBlockEntity>> ALCHEMY_CENTRIFUGE_ENTITY=ENTITIES.register("alchemy_centrifuge",()->BlockEntityType.Builder.of(AlchemyCentrifugeBlockEntity::new,ALCHEMY_CENTRIFUGE.get()).build(null));
    public static final RegistryObject<EssentiaCrystallizerBlock> ESSENTIA_CRYSTALLIZER=BLOCKS.register("essentia_crystallizer",EssentiaCrystallizerBlock::new);
    public static final RegistryObject<BlockEntityType<EssentiaCrystallizerBlockEntity>> ESSENTIA_CRYSTALLIZER_ENTITY=ENTITIES.register("essentia_crystallizer",()->BlockEntityType.Builder.of(EssentiaCrystallizerBlockEntity::new,ESSENTIA_CRYSTALLIZER.get()).build(null));
    public static final RegistryObject<VisRelayBlock> VIS_RELAY = BLOCKS.register("vis_relay", () -> new VisRelayBlock(false));
    public static final RegistryObject<VisRelayBlock> WORKBENCH_CHARGER = BLOCKS.register("workbench_charger", () -> new VisRelayBlock(true));
    public static final RegistryObject<BlockEntityType<VisRelayBlockEntity>> VIS_RELAY_ENTITY = ENTITIES.register("vis_relay",
            () -> BlockEntityType.Builder.of(VisRelayBlockEntity::new, VIS_RELAY.get()).build(null));
    public static final RegistryObject<BlockEntityType<WorkbenchChargerBlockEntity>> WORKBENCH_CHARGER_ENTITY = ENTITIES.register("workbench_charger",
            () -> BlockEntityType.Builder.of(WorkbenchChargerBlockEntity::new, WORKBENCH_CHARGER.get()).build(null));
    public static final RegistryObject<BlockEntityType<EnergizedNodeBlockEntity>> ENERGIZED_NODE_ENTITY = ENTITIES.register("energized_node",
            () -> BlockEntityType.Builder.of(EnergizedNodeBlockEntity::new, ENERGIZED_NODE.get()).build(null));
    public static final RegistryObject<FocalManipulatorBlock> FOCAL_BLOCK = BLOCKS.register("focal_manipulator", FocalManipulatorBlock::new);
    public static final RegistryObject<Block> ARCANE_STONE = BLOCKS.register("arcane_stone", () -> new Block(Block.Properties.of().strength(4, 20).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> ARCANE_STONE_BRICKS = BLOCKS.register("arcane_stone_bricks", () -> new Block(Block.Properties.of().strength(4, 20).requiresCorrectToolForDrops()));
    public static final RegistryObject<net.minecraft.world.level.block.SlabBlock> ARCANE_STONE_SLAB = BLOCKS.register("arcane_stone_slab",
            () -> new net.minecraft.world.level.block.SlabBlock(Block.Properties.of().strength(2, 6).requiresCorrectToolForDrops()));
    public static final RegistryObject<net.minecraft.world.level.block.StairBlock> ARCANE_STONE_STAIRS = BLOCKS.register("arcane_stone_stairs",
            () -> new net.minecraft.world.level.block.StairBlock(() -> ARCANE_STONE_BRICKS.get().defaultBlockState(), Block.Properties.of().strength(2, 20).requiresCorrectToolForDrops()));
    public static final RegistryObject<BlockEntityType<FocalManipulatorBlockEntity>> FOCAL_ENTITY = ENTITIES.register("focal_manipulator",
            () -> BlockEntityType.Builder.of(FocalManipulatorBlockEntity::new, FOCAL_BLOCK.get()).build(null));
    public static final RegistryObject<MenuType<FocalManipulatorMenu>> FOCAL_MENU = MENUS.register("focal_manipulator", () -> IForgeMenuType.create(FocalManipulatorMenu::new));
    public static final RegistryObject<BlockEntityType<ArcaneWorkbenchBlockEntity>> ENTITY = ENTITIES.register("arcane_workbench",
            () -> BlockEntityType.Builder.of(ArcaneWorkbenchBlockEntity::new, BLOCK.get()).build(null));
    public static final RegistryObject<MenuType<ArcaneWorkbenchMenu>> MENU = MENUS.register("arcane_workbench", () -> IForgeMenuType.create(ArcaneWorkbenchMenu::new));
    public static final RegistryObject<MenuType<FocusPouchMenu>> FOCUS_POUCH_MENU = MENUS.register("focus_pouch", () -> IForgeMenuType.create(FocusPouchMenu::new));
    public static final RegistryObject<MenuType<HoverHarnessMenu>> HOVER_HARNESS_MENU = MENUS.register("hover_harness", () -> IForgeMenuType.create(HoverHarnessMenu::new));
    public static final RegistryObject<RecipeType<ArcaneRecipe>> RECIPE_TYPE = TYPES.register("arcane", () -> new RecipeType<>() {
        @Override public String toString() { return "thaumcraft:arcane"; }
    });
    public static final RegistryObject<RecipeSerializer<ArcaneRecipe>> RECIPE_SERIALIZER = SERIALIZERS.register("arcane", ArcaneRecipe.Serializer::new);
    public static final RegistryObject<RecipeType<CrucibleRecipe>> CRUCIBLE_RECIPE_TYPE = TYPES.register("crucible", () -> new RecipeType<>() {
        @Override public String toString() { return "thaumcraft:crucible"; }
    });
    public static final RegistryObject<RecipeSerializer<CrucibleRecipe>> CRUCIBLE_RECIPE_SERIALIZER = SERIALIZERS.register("crucible", CrucibleRecipe.Serializer::new);
    public static final RegistryObject<RecipeType<InfusionRecipe>> INFUSION_RECIPE_TYPE = TYPES.register("infusion",()->new RecipeType<>() {
        @Override public String toString() { return "thaumcraft:infusion"; }
    });
    public static final RegistryObject<RecipeSerializer<InfusionRecipe>> INFUSION_RECIPE_SERIALIZER = SERIALIZERS.register("infusion",InfusionRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<RobeDyeRecipe>> ROBE_DYE_SERIALIZER = SERIALIZERS.register("armor_dye",
            () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(RobeDyeRecipe::new));
    public static final RegistryObject<RecipeSerializer<JarLabelRecipe>> JAR_LABEL_SERIALIZER = SERIALIZERS.register("jar_label",
            JarLabelRecipe.Serializer::new);
    public static final RegistryObject<RecipeType<InfernalBonusRecipe>> INFERNAL_BONUS_TYPE=TYPES.register("infernal_bonus",()->new RecipeType<>() {
        @Override public String toString() { return "thaumcraft:infernal_bonus"; }
    });
    public static final RegistryObject<RecipeSerializer<InfernalBonusRecipe>> INFERNAL_BONUS_SERIALIZER=SERIALIZERS.register("infernal_bonus",InfernalBonusRecipe.Serializer::new);
    private TCArcane() {}
    public static void register(IEventBus bus) { BLOCKS.register(bus); ENTITIES.register(bus); MENUS.register(bus); TYPES.register(bus); SERIALIZERS.register(bus); }
}
