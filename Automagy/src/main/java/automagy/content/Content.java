package automagy.content;

import automagy.Automagy;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class Content {
    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES=DeferredRegister.create(ForgeRegistries.FEATURES,Automagy.ID);
    public static final RegistryObject<NetherSpireFeature> NETHER_SPIRE=FEATURES.register("nether_spire",NetherSpireFeature::new);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,Automagy.ID);
    public static final RegistryObject<EntropicRefiningRecipe.Serializer> REFINING_RECIPE=RECIPE_SERIALIZERS.register("entropic_refining",EntropicRefiningRecipe.Serializer::new);
    public static final RegistryObject<net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<AdvancedGolemRecipe>> ADVANCED_GOLEM_RECIPE=RECIPE_SERIALIZERS.register("advanced_golem",()->new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(AdvancedGolemRecipe::new));
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Automagy.ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Automagy.ID);
    public static final RegistryObject<CraftingFocusItem> CRAFTING_FOCUS=ITEMS.register("crafting_focus",CraftingFocusItem::new);
    public static final RegistryObject<ThinkingCapItem> THINKING_CAP=ITEMS.register("thinking_cap",ThinkingCapItem::new);
    public static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Automagy.ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Automagy.ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Automagy.ID);
    public static final RegistryObject<ThaumostaticBlock> THAUMOSTATIC_PYLON=block("thaumostatic_pylon",()->new ThaumostaticBlock(false));
    public static final RegistryObject<BlockEntityType<ThaumostaticPylonBlockEntity>> THAUMOSTATIC_PYLON_TILE=TILES.register("thaumostatic_pylon",()->BlockEntityType.Builder.of(ThaumostaticPylonBlockEntity::new,THAUMOSTATIC_PYLON.get()).build(null));
    public static final RegistryObject<ThaumostaticBlock> THAUMOSTATIC_CONTROLLER=block("thaumostatic_controller",()->new ThaumostaticBlock(true));
    public static final RegistryObject<BlockEntityType<ThaumostaticControllerBlockEntity>> THAUMOSTATIC_CONTROLLER_TILE=TILES.register("thaumostatic_controller",()->BlockEntityType.Builder.of(ThaumostaticControllerBlockEntity::new,THAUMOSTATIC_CONTROLLER.get()).build(null));
    public static final RegistryObject<ThaumostaticSpireBlock> THAUMOSTATIC_SPIRE=block("thaumostatic_spire",ThaumostaticSpireBlock::new);
    public static final RegistryObject<BlockEntityType<ThaumostaticSpireBlockEntity>> THAUMOSTATIC_SPIRE_TILE=TILES.register("thaumostatic_spire",()->BlockEntityType.Builder.of(ThaumostaticSpireBlockEntity::new,THAUMOSTATIC_SPIRE.get()).build(null));
    public static final RegistryObject<EnderDisjunctionRingItem> ENDER_DISJUNCTION_RING=ITEMS.register("ender_disjunction_ring",()->new EnderDisjunctionRingItem(false));
    public static final RegistryObject<EnderDisjunctionRingItem> LESSER_ENDER_DISJUNCTION_RING=ITEMS.register("lesser_ender_disjunction_ring",()->new EnderDisjunctionRingItem(true));
    public static final RegistryObject<InfinityJarBlock> INFINITY_JAR=block("infinity_jar",InfinityJarBlock::new);
    public static final RegistryObject<BlockEntityType<InfinityJarBlockEntity>> INFINITY_JAR_TILE=TILES.register("infinity_jar",()->BlockEntityType.Builder.of(InfinityJarBlockEntity::new,INFINITY_JAR.get()).build(null));
    public static final RegistryObject<MobLureBlock> MOB_LURE=block("mob_lure",MobLureBlock::new);
    public static final RegistryObject<BlockEntityType<MobLureBlockEntity>> MOB_LURE_TILE=TILES.register("mob_lure",()->BlockEntityType.Builder.of(MobLureBlockEntity::new,MOB_LURE.get()).build(null));
    public static final RegistryObject<NetherHeartItem> NETHER_HEART=ITEMS.register("nether_heart",NetherHeartItem::new);
    public static final DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> LOOT_MODIFIERS=DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,Automagy.ID);
    public static final RegistryObject<com.mojang.serialization.Codec<NetherHeartLootModifier>> HEART_LOOT=LOOT_MODIFIERS.register("nether_heart",()->NetherHeartLootModifier.CODEC);
    public static final RegistryObject<NethermindBlock> NETHERMIND=block("nethermind",NethermindBlock::new);
    public static final RegistryObject<BlockEntityType<NethermindBlockEntity>> NETHERMIND_TILE=TILES.register("nethermind",()->BlockEntityType.Builder.of(NethermindBlockEntity::new,NETHERMIND.get()).build(null));
    public static final RegistryObject<DimensionLureBlock> DIMENSION_LURE=block("dimension_lure",DimensionLureBlock::new);
    public static final RegistryObject<BlockEntityType<DimensionLureBlockEntity>> DIMENSION_LURE_TILE=TILES.register("dimension_lure",()->BlockEntityType.Builder.of(DimensionLureBlockEntity::new,DIMENSION_LURE.get()).build(null));
    public static final RegistryObject<RunedObsidianBlock> RUNED_OBSIDIAN=block("runed_obsidian",RunedObsidianBlock::new);
    public static final java.util.Map<String,RegistryObject<NetherRuneBlock>> NETHER_RUNES=netherRunes();
    private static java.util.Map<String,RegistryObject<NetherRuneBlock>> netherRunes(){var map=new java.util.LinkedHashMap<String,RegistryObject<NetherRuneBlock>>();automagy.logic.NetherRuneCatalog.BUILTINS.forEach((key,definition)->map.put(key,block("nether_rune_"+key,()->new NetherRuneBlock(definition))));return java.util.Collections.unmodifiableMap(map);}
    public static final RegistryObject<ExperiencePhialItem> EXPERIENCE_PHIAL=ITEMS.register("experience_phial",ExperiencePhialItem::new);
    public static final RegistryObject<ExperienceJarBlock> EXPERIENCE_JAR=block("experience_jar",ExperienceJarBlock::new);
    public static final RegistryObject<BlockEntityType<ExperienceJarBlockEntity>> EXPERIENCE_JAR_TILE=TILES.register("experience_jar",()->BlockEntityType.Builder.of(ExperienceJarBlockEntity::new,EXPERIENCE_JAR.get()).build(null));
    public static final RegistryObject<ExperienceStoneBlock> EXPERIENCE_STONE=block("experience_stone",ExperienceStoneBlock::new);
    public static final RegistryObject<BlockEntityType<ExperienceStoneBlockEntity>> EXPERIENCE_STONE_TILE=TILES.register("experience_stone",()->BlockEntityType.Builder.of(ExperienceStoneBlockEntity::new,EXPERIENCE_STONE.get()).build(null));
    public static final RegistryObject<AmnesiacStoneBlock> AMNESIAC_STONE=block("amnesiac_stone",AmnesiacStoneBlock::new);
    public static final RegistryObject<BlockEntityType<AmnesiacStoneBlockEntity>> AMNESIAC_STONE_TILE=TILES.register("amnesiac_stone",()->BlockEntityType.Builder.of(AmnesiacStoneBlockEntity::new,AMNESIAC_STONE.get()).build(null));
    public static final java.util.Map<Integer,RegistryObject<Item>> WORKBENCH_UPGRADES=workbenchUpgrades();
    private static java.util.Map<Integer,RegistryObject<Item>> workbenchUpgrades(){var map=new java.util.LinkedHashMap<Integer,RegistryObject<Item>>();for(int id:new int[]{0,1,3,5})map.put(id,ITEMS.register("workbench_upgrade_"+id,()->new WorkbenchUpgradeItem(id)));return java.util.Collections.unmodifiableMap(map);}
    public static final RegistryObject<MenuType<PortableCraftingMenu>> PORTABLE_CRAFTING_MENU=MENUS.register("portable_crafting",()->IForgeMenuType.create((id,inv,buf)->new PortableCraftingMenu(id,inv,buf.readVarInt())));
    public static final RegistryObject<MenuType<GolemWorkbenchMenu>> GOLEM_WORKBENCH_MENU=MENUS.register("golem_workbench",()->IForgeMenuType.create((id,inv,buf)->new GolemWorkbenchMenu(id,inv,buf.readBlockPos())));
    public static final RegistryObject<GolemWorkbenchBlock> GOLEM_WORKBENCH=block("golem_workbench",GolemWorkbenchBlock::new);
    public static final RegistryObject<BlockEntityType<GolemWorkbenchBlockEntity>> GOLEM_WORKBENCH_TILE=TILES.register("golem_workbench",()->BlockEntityType.Builder.of(GolemWorkbenchBlockEntity::new,GOLEM_WORKBENCH.get()).build(null));
    public static final RegistryObject<Item> AQUA_INK=ITEMS.register("aqua_ink",()->new Item(new Item.Properties()));
    public static final RegistryObject<ThirstyTankBlock> THIRSTY_TANK=block("thirsty_tank",ThirstyTankBlock::new);
    public static final RegistryObject<BlockEntityType<ThirstyTankBlockEntity>> THIRSTY_TANK_TILE=TILES.register("thirsty_tank",()->BlockEntityType.Builder.of(ThirstyTankBlockEntity::new,THIRSTY_TANK.get()).build(null));
    public static final java.util.Map<Integer,RegistryObject<Item>> TANK_GLYPHS=tankGlyphs();
    private static java.util.Map<Integer,RegistryObject<Item>> tankGlyphs(){var map=new java.util.LinkedHashMap<Integer,RegistryObject<Item>>();for(int i=1;i<=9;i++){int id=i;map.put(id,ITEMS.register("tank_glyph_"+id,()->new Item(new Item.Properties())));}return java.util.Collections.unmodifiableMap(map);}
    public static final RegistryObject<Item> VISHROOM_STEW=ITEMS.register("vishroom_stew",VishroomStewItem::new);
    public static final RegistryObject<BoilerBlock> BOILER=block("boiler",BoilerBlock::new);
    public static final RegistryObject<BlockEntityType<BoilerBlockEntity>> BOILER_TILE=TILES.register("boiler",()->BlockEntityType.Builder.of(BoilerBlockEntity::new,BOILER.get()).build(null));
    public static final RegistryObject<MenuType<BoilerMenu>> BOILER_MENU=MENUS.register("boiler",()->IForgeMenuType.create((id,inv,buf)->new BoilerMenu(id,inv,buf.readBlockPos())));
    public static final RegistryObject<LocusAggregatorBlock> LOCUS_AGGREGATOR=block("locus_aggregator",LocusAggregatorBlock::new);
    public static final RegistryObject<EssentiaLocusBlock> ESSENTIA_LOCUS=block("essentia_locus",EssentiaLocusBlock::new);
    public static final RegistryObject<BlockEntityType<EssentiaLocusBlockEntity>> ESSENTIA_LOCUS_TILE=TILES.register("essentia_locus",()->BlockEntityType.Builder.of(EssentiaLocusBlockEntity::new,ESSENTIA_LOCUS.get()).build(null));
    public static final RegistryObject<Item> AUTO_HAND_MIRROR=ITEMS.register("auto_hand_mirror",AutoHandMirrorItem::new);
    public static final RegistryObject<MenuType<AutoHandMirrorMenu>> AUTO_HAND_MIRROR_MENU=MENUS.register("auto_hand_mirror",()->IForgeMenuType.create((id,inventory,buf)->new AutoHandMirrorMenu(id,inventory,buf.readVarInt())));
    public static final RegistryObject<GolemInhibitorBlock> INHIBITOR=block("golem_inhibitor",GolemInhibitorBlock::new);
    public static final RegistryObject<BlockEntityType<GolemInhibitorBlockEntity>> INHIBITOR_TILE=TILES.register("golem_inhibitor",()->BlockEntityType.Builder.of(GolemInhibitorBlockEntity::new,INHIBITOR.get()).build(null));
    public static final RegistryObject<GolemTaskmasterBlock> TASKMASTER=block("golem_taskmaster",GolemTaskmasterBlock::new);
    public static final RegistryObject<BlockEntityType<GolemTaskmasterBlockEntity>> TASKMASTER_TILE=TILES.register("golem_taskmaster",()->BlockEntityType.Builder.of(GolemTaskmasterBlockEntity::new,TASKMASTER.get()).build(null));
    public static final RegistryObject<Item> GOLEM_LINKER=ITEMS.register("golem_linker",GolemLinkerItem::new);
    public static final RegistryObject<Item> CONFLUX_DISC=ITEMS.register("conflux_disc",()->new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<DeliveryMirrorBlock> MELDING_MIRROR=block("melding_mirror",()->new DeliveryMirrorBlock(false));
    public static final RegistryObject<DeliveryMirrorBlock> MULTI_DEST_MIRROR=block("multi_dest_mirror",()->new DeliveryMirrorBlock(true));
    public static final RegistryObject<BlockEntityType<DeliveryMirrorBlockEntity>> DELIVERY_MIRROR_TILE=TILES.register("delivery_mirror",()->BlockEntityType.Builder.of(DeliveryMirrorBlockEntity::new,MELDING_MIRROR.get(),MULTI_DEST_MIRROR.get()).build(null));
    public static final RegistryObject<RequisitionTomeBlock> REQUISITION_TOME=block("requisition_tome",RequisitionTomeBlock::new);
    public static final RegistryObject<BlockEntityType<RequisitionTomeBlockEntity>> REQUISITION_TOME_TILE=TILES.register("requisition_tome",()->BlockEntityType.Builder.of(RequisitionTomeBlockEntity::new,REQUISITION_TOME.get()).build(null));
    public static final RegistryObject<GlowingAmberBlock> GLOWING_AMBER=block("glowing_amber",GlowingAmberBlock::new);
    public static final RegistryObject<GlowingAmberBlock> GLOWING_AMBER_BRICKS=block("glowing_amber_bricks",GlowingAmberBlock::new);
    public static final RegistryObject<AmberMemoryBlock> AMBER_MEMORY=block("amber_memory",AmberMemoryBlock::new);
    public static final RegistryObject<EnchantedBookshelfBlock> GREATWOOD_BOOKSHELF=block("greatwood_bookshelf",()->new EnchantedBookshelfBlock(216,4));
    public static final RegistryObject<EnchantedBookshelfBlock> SILVERWOOD_BOOKSHELF=block("silverwood_bookshelf",()->new EnchantedBookshelfBlock(486,8));
    public static final RegistryObject<MenuType<InventariumMenu>> INVENTARIUM_MENU=MENUS.register("inventarium",()->IForgeMenuType.create((id,inv,buf)->new InventariumMenu(id,inv,buf.readBlockPos())));
    public static final RegistryObject<ScribePointerBlock> SCRIBE_POINTER=block("scribe_pointer",ScribePointerBlock::new);
    public static final RegistryObject<BlockEntityType<ScribePointerBlockEntity>> SCRIBE_POINTER_TILE=TILES.register("scribe_pointer",()->BlockEntityType.Builder.of(ScribePointerBlockEntity::new,SCRIBE_POINTER.get()).build(null));
    public static final RegistryObject<UnseenScribeBlock> SCRIBE=block("unseen_scribe",UnseenScribeBlock::new);
    public static final RegistryObject<BlockEntityType<UnseenScribeBlockEntity>> SCRIBE_TILE=TILES.register("unseen_scribe",()->BlockEntityType.Builder.of(UnseenScribeBlockEntity::new,SCRIBE.get()).build(null));
    public static final RegistryObject<InventariumBlock> INVENTARIUM=block("inventarium",InventariumBlock::new);
    public static final RegistryObject<BlockEntityType<InventariumBlockEntity>> INVENTARIUM_TILE=TILES.register("inventarium",()->BlockEntityType.Builder.of(InventariumBlockEntity::new,INVENTARIUM.get()).build(null));
    public static final RegistryObject<MawProcessBlock> MAW_PROCESS=BLOCKS.register("maw_process",MawProcessBlock::new);
    public static final RegistryObject<BlockEntityType<MawProcessBlockEntity>> MAW_PROCESS_TILE=TILES.register("maw_process",()->BlockEntityType.Builder.of(MawProcessBlockEntity::new,MAW_PROCESS.get()).build(null));
    public static final RegistryObject<MawBlock> HUNGRY_MAW=block("hungry_maw",()->new MawBlock(false));
    public static final RegistryObject<MawBlock> FINICAL_MAW=block("finical_maw",()->new MawBlock(true));
    public static final RegistryObject<BlockEntityType<MawBlockEntity>> MAW_TILE=TILES.register("maw",()->BlockEntityType.Builder.of(MawBlockEntity::new,HUNGRY_MAW.get(),FINICAL_MAW.get()).build(null));
    public static final RegistryObject<Item> SHATTERED_PEARL = ITEMS.register("shattered_avarice_pearl",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> MERCATUS_AMALGAM = ITEMS.register("mercatus_amalgam",MercatusAmalgamItem::new);
    public static final RegistryObject<EagerChestBlock> EAGER_CHEST = block("eager_chest",EagerChestBlock::new);
    public static final RegistryObject<BlockEntityType<EagerChestBlockEntity>> EAGER_TILE = TILES.register("eager_chest",()->BlockEntityType.Builder.of(EagerChestBlockEntity::new,EAGER_CHEST.get()).build(null));
    public static final RegistryObject<MenuType<EagerChestMenu>> EAGER_MENU = MENUS.register("eager_chest",()->IForgeMenuType.create((id,inv,buf)->new EagerChestMenu(id,inv,buf.readBlockPos(),buf.readBoolean())));
    public static final RegistryObject<Item> AVARICE_PEARL = ITEMS.register("avarice_pearl",()->new AvaricePearlItem(false));
    public static final RegistryObject<Item> MISERS_PEARL = ITEMS.register("misers_pearl",()->new AvaricePearlItem(true));
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,Automagy.ID);
    public static final RegistryObject<net.minecraft.world.entity.EntityType<HoveringItemEntity>> HOVERING_ITEM=ENTITIES.register("hovering_item",()->net.minecraft.world.entity.EntityType.Builder.<HoveringItemEntity>of(HoveringItemEntity::new,net.minecraft.world.entity.MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(6).updateInterval(3).build("automagy:hovering_item"));
    public static final RegistryObject<net.minecraft.world.entity.EntityType<NetherWispEntity>> NETHER_WISP=ENTITIES.register("nether_wisp",()->net.minecraft.world.entity.EntityType.Builder.<NetherWispEntity>of(NetherWispEntity::new,net.minecraft.world.entity.MobCategory.MONSTER).sized(.9f,.9f).fireImmune().clientTrackingRange(4).updateInterval(3).build("automagy:nether_wisp"));
    public static final RegistryObject<net.minecraft.world.entity.EntityType<SuppressedExperienceOrb>> SUPPRESSED_ORB=ENTITIES.register("suppressed_experience_orb",()->net.minecraft.world.entity.EntityType.Builder.<SuppressedExperienceOrb>of(SuppressedExperienceOrb::new,net.minecraft.world.entity.MobCategory.MISC).sized(.5f,.5f).clientTrackingRange(6).updateInterval(20).build("automagy:suppressed_experience_orb"));
    public static final RegistryObject<net.minecraft.world.entity.EntityType<AvaricePearlEntity>> PEARL_ENTITY=ENTITIES.register("avarice_pearl",()->net.minecraft.world.entity.EntityType.Builder.<AvaricePearlEntity>of(AvaricePearlEntity::new,net.minecraft.world.entity.MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(10).build("automagy:avarice_pearl"));
    public static final RegistryObject<GreedyChestBlock> GREEDY_CHEST = block("greedy_chest",GreedyChestBlock::new);
    public static final RegistryObject<BlockEntityType<GreedyChestBlockEntity>> GREEDY_TILE = TILES.register("greedy_chest",()->BlockEntityType.Builder.of(GreedyChestBlockEntity::new,GREEDY_CHEST.get()).build(null));
    public static final RegistryObject<MenuType<GreedyChestMenu>> GREEDY_MENU = MENUS.register("greedy_chest",()->IForgeMenuType.create((id,inv,buf)->new GreedyChestMenu(id,inv,buf.readBlockPos(),buf.readBoolean())));
    public static final RegistryObject<TenaciousChestBlock> TENACIOUS_CHEST = block("tenacious_chest", TenaciousChestBlock::new);
    public static final RegistryObject<BlockEntityType<TenaciousChestBlockEntity>> TENACIOUS_TILE = TILES.register("tenacious_chest",
            () -> BlockEntityType.Builder.of(TenaciousChestBlockEntity::new, TENACIOUS_CHEST.get()).build(null));
    public static final java.util.Map<String, RegistryObject<TallyBlock>> TALLIES = tallies();
    private static java.util.Map<String, RegistryObject<TallyBlock>> tallies() {
        var result = new java.util.LinkedHashMap<String, RegistryObject<TallyBlock>>();
        for (String kind : new String[]{"inventory", "world", "drops"}) result.put(kind, block("tally_" + kind, () -> new TallyBlock(kind)));
        return java.util.Collections.unmodifiableMap(result);
    }
    public static final java.util.Map<String, RegistryObject<Item>> TALLY_LENSES = lenses();
    private static java.util.Map<String, RegistryObject<Item>> lenses() {
        var result = new java.util.LinkedHashMap<String, RegistryObject<Item>>();
        for (String kind : new String[]{"blank", "world", "drops"}) result.put(kind, ITEMS.register("tally_lens_" + kind, () -> new TallyLensItem(kind)));
        return java.util.Collections.unmodifiableMap(result);
    }
    public static final RegistryObject<BlockEntityType<TallyBlockEntity>> TALLY_TILE = TILES.register("tally",
            () -> BlockEntityType.Builder.of(TallyBlockEntity::new, TALLIES.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
    public static final RegistryObject<MenuType<MawMenu>> MAW_MENU=MENUS.register("finical_maw",()->IForgeMenuType.create((id,inv,buf)->new MawMenu(id,inv,buf.readBlockPos())));
    public static final RegistryObject<MenuType<MirrorSettingsMenu>> MIRROR_SETTINGS_MENU=MENUS.register("mirror_settings",()->IForgeMenuType.create((id,inv,buf)->new MirrorSettingsMenu(id,inv,buf.readBlockPos())));
    public static final RegistryObject<MenuType<TallyMenu>> TALLY_MENU = MENUS.register("tally",
            () -> IForgeMenuType.create((id, inventory, buf) -> new TallyMenu(id, inventory, buf.readBlockPos())));
    public static final RegistryObject<Item> ENCHANTED_PAPER = ITEMS.register("enchanted_paper", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ENCHANTED_TOME = ITEMS.register("enchanted_tome", () -> new Item(new Item.Properties()));
    public static final RegistryObject<FilterItem> WHITE_FILTER = ITEMS.register("white_filter", () -> new FilterItem(false));
    public static final RegistryObject<FilterItem> BLACK_FILTER = ITEMS.register("black_filter", () -> new FilterItem(true));
    public static final RegistryObject<MenuType<FilterMenu>> FILTER_MENU = MENUS.register("filter", () -> IForgeMenuType.create((id, inv, buf) -> {
        FilterMenu menu = new FilterMenu(id, inv, buf.readVarInt()); menu.initialName(buf.readUtf(ItemFilter.MAX_NAME)); return menu;
    }));
    public static final RegistryObject<HourglassBlock> HOURGLASS = block("hourglass", () -> new HourglassBlock(false));
    public static final RegistryObject<HourglassBlock> MAGIC_HOURGLASS = block("magic_hourglass", () -> new HourglassBlock(true));
    public static final RegistryObject<InversionTorchBlock> INVERSION_TORCH = block("inversion_torch", InversionTorchBlock::new);
    public static final RegistryObject<RemoteComparatorBlock> REMOTE_COMPARATOR = block("remote_comparator",RemoteComparatorBlock::new);
    public static final RegistryObject<VisReaderBlock> VIS_READER = block("vis_reader",VisReaderBlock::new);
    public static final RegistryObject<EntitySensorBlock> ENTITY_SENSOR = block("entity_sensor",EntitySensorBlock::new);
    public static final RegistryObject<Item> ENTITY_EYE = ITEMS.register("entity_eye",EntityEyeItem::new);
    public static final java.util.Map<String,RegistryObject<Item>> CRYSTAL_BRAINS = brains();
    private static java.util.Map<String,RegistryObject<Item>> brains(){
        var result=new java.util.LinkedHashMap<String,RegistryObject<Item>>();
        for(String primal:new String[]{"air","fire","water","earth","order","entropy"})
            result.put(primal,ITEMS.register(primal+"_crystal_brain",()->new Item(new Item.Properties())));
        return java.util.Collections.unmodifiableMap(result);
    }
    public static final RegistryObject<BlockEntityType<EntitySensorBlockEntity>> ENTITY_SENSOR_TILE = TILES.register("entity_sensor",
            ()->BlockEntityType.Builder.of(EntitySensorBlockEntity::new,ENTITY_SENSOR.get()).build(null));
    public static final RegistryObject<MenuType<EntitySensorMenu>> ENTITY_SENSOR_MENU = MENUS.register("entity_sensor",
            ()->IForgeMenuType.create((id,inventory,buf)->new EntitySensorMenu(id,inventory,buf.readBlockPos())));
    public static final RegistryObject<BlockEntityType<VisReaderBlockEntity>> VIS_READER_TILE = TILES.register("vis_reader",
            ()->BlockEntityType.Builder.of(VisReaderBlockEntity::new,VIS_READER.get()).build(null));
    public static final RegistryObject<MenuType<VisReaderMenu>> VIS_READER_MENU = MENUS.register("vis_reader",
            ()->IForgeMenuType.create((id,inventory,buf)->new VisReaderMenu(id,inventory,buf.readBlockPos())));
    public static final RegistryObject<Item> CRYSTAL_EYE = ITEMS.register("crystal_eye",CrystalEyeItem::new);
    public static final RegistryObject<BlockEntityType<RemoteComparatorBlockEntity>> REMOTE_COMPARATOR_TILE = TILES.register("remote_comparator",
            ()->BlockEntityType.Builder.of(RemoteComparatorBlockEntity::new,REMOTE_COMPARATOR.get()).build(null));
    public static final java.util.Map<String, RegistryObject<RedcrystalBlock>> REDCRYSTALS = redcrystals();
    private static java.util.Map<String, RegistryObject<RedcrystalBlock>> redcrystals() {
        var blocks = new java.util.LinkedHashMap<String, RegistryObject<RedcrystalBlock>>();
        for (String kind : new String[]{"standard", "amp", "dim", "dense", "res", "merc"})
            blocks.put(kind, block(kind.equals("standard") ? "redcrystal" : "redcrystal_"+kind, () -> new RedcrystalBlock(kind)));
        return java.util.Collections.unmodifiableMap(blocks);
    }
    public static final RegistryObject<BlockEntityType<RedcrystalBlockEntity>> REDCRYSTAL_TILE = TILES.register("redcrystal",
            () -> BlockEntityType.Builder.of(RedcrystalBlockEntity::new, REDCRYSTALS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
    public static final java.util.Map<String, RegistryObject<Item>> SLIVERS = slivers();
    private static java.util.Map<String, RegistryObject<Item>> slivers() {
        var items = new java.util.LinkedHashMap<String, RegistryObject<Item>>();
        for (String primal : new String[]{"air", "fire", "water", "earth", "order", "entropy"})
            items.put(primal, ITEMS.register(primal+"_sliver", () -> new Item(new Item.Properties())));
        return java.util.Collections.unmodifiableMap(items);
    }
    public static final RegistryObject<BlockEntityType<InversionTorchBlockEntity>> INVERSION_TILE = TILES.register("inversion_torch",
            () -> BlockEntityType.Builder.of(InversionTorchBlockEntity::new, INVERSION_TORCH.get()).build(null));
    public static final RegistryObject<BlockEntityType<HourglassBlockEntity>> HOURGLASS_TILE = TILES.register("hourglass",
            () -> BlockEntityType.Builder.of(HourglassBlockEntity::new, HOURGLASS.get(), MAGIC_HOURGLASS.get()).build(null));
    public static final RegistryObject<MenuType<HourglassMenu>> HOURGLASS_MENU = MENUS.register("hourglass",
            () -> IForgeMenuType.create((id, inventory, buf) -> new HourglassMenu(id, inventory, buf.readBlockPos())));
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.automagy")).icon(() -> new ItemStack(MAGIC_HOURGLASS.get()))
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> {
                if(item.get()==INFINITY_JAR.get().asItem()&&!automagy.CreativeToolsConfig.LIST_JAR.get()||item.get()==THINKING_CAP.get()&&!automagy.CreativeToolsConfig.LIST_CAP.get())return;
                output.accept(item.get()==EXPERIENCE_JAR.get().asItem()?ExperienceJarItem.full():new ItemStack(item.get()));
            })).build());
    public static <T extends Block> RegistryObject<T> block(String name, java.util.function.Supplier<T> factory) {
        var result = BLOCKS.register(name, factory);
        ITEMS.register(name, () -> result.get() instanceof HourglassBlock ? new HourglassItem(result.get())
                : result.get() instanceof GolemTaskmasterBlock || result.get() instanceof GolemInhibitorBlock || result.get() instanceof RequisitionTomeBlock || result.get() instanceof UnseenScribeBlock ? new PedestalDeviceItem(result.get())
                : result.get() instanceof InventariumBlock || result.get() instanceof EssentiaLocusBlock ? new InventariumItem(result.get())
                : result.get() instanceof TenaciousChestBlock ? new TenaciousChestItem(result.get())
                : result.get() instanceof GreedyChestBlock ? new GreedyChestItem(result.get())
                : result.get() instanceof EagerChestBlock ? new EagerChestItem(result.get())
                : result.get() instanceof RedcrystalBlock ? new RedcrystalItem(result.get())
                : result.get() instanceof InfinityJarBlock ? new InfinityJarItem(result.get()) : result.get() instanceof ExperienceJarBlock ? new ExperienceJarItem(result.get()) : result.get() instanceof ThirstyTankBlock ? new ThirstyTankItem(result.get()) : result.get() instanceof BoilerBlock ? new BoilerItem(result.get()) : result.get() instanceof RemoteComparatorBlock ? new RemoteComparatorItem(result.get()) : result.get() instanceof MobLureBlock || result.get() instanceof DimensionLureBlock ? new LureBlockItem(result.get()) : new BlockItem(result.get(), new Item.Properties()));
        return result;
    }
    public static void register(IEventBus bus) {
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(NETHER_WISP.get(),NetherWispEntity.attributes().build()));
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(InventariumDeliveryGoal::register));
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(MawCrafting::register));
        net.minecraftforge.common.ForgeMod.enableMilkFluid();
        SoupFluids.register(bus);
        RECIPE_SERIALIZERS.register(bus); FEATURES.register(bus); LOOT_MODIFIERS.register(bus); BLOCKS.register(bus); ITEMS.register(bus); TILES.register(bus); MENUS.register(bus); TABS.register(bus); ENTITIES.register(bus);
    }
    private Content() {}
}
