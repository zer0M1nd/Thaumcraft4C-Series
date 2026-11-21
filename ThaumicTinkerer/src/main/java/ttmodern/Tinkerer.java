package ttmodern;

import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Tinkerer.ID)
public final class Tinkerer {
    public static final String ID="thaumic_tinkerer";
    private static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPES=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,ID);
    private static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES=DeferredRegister.create(ForgeRegistries.FEATURES,ID);
    public static final RegistryObject<BedrockOreFeature> BEDROCK_ORES=FEATURES.register("bedrock_ores",BedrockOreFeature::new);
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,ID);
    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,ID);
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,ID);
    public static final RegistryObject<BedrockPortalBlock> BEDROCK_PORTAL=BLOCKS.register("bedrock_portal",BedrockPortalBlock::new);
    public static final RegistryObject<GolemConnectorBlock> GOLEM_CONNECTOR=block("golem_connector",GolemConnectorBlock::new);
    public static final RegistryObject<BlockEntityType<GolemConnectorEntity>> GOLEM_CONNECTOR_ENTITY=ENTITIES.register("golem_connector",()->BlockEntityType.Builder.of(GolemConnectorEntity::new,GOLEM_CONNECTOR.get()).build(null));
    public static final RegistryObject<AspectAnalyzerBlock> ASPECT_ANALYZER=block("aspect_analyzer",AspectAnalyzerBlock::new);
    public static final RegistryObject<BlockEntityType<AspectAnalyzerEntity>> ASPECT_ANALYZER_ENTITY=ENTITIES.register("aspect_analyzer",()->BlockEntityType.Builder.of(AspectAnalyzerEntity::new,ASPECT_ANALYZER.get()).build(null));
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<AspectAnalyzerMenu>> ASPECT_ANALYZER_MENU=MENUS.register("aspect_analyzer",()->net.minecraftforge.common.extensions.IForgeMenuType.create(AspectAnalyzerMenu::new));
    public static final RegistryObject<GasBlock> LIGHT_GAS=BLOCKS.register("gaseous_light",()->new GasBlock(false,false));
    public static final RegistryObject<GasBlock> SHADOW_GAS=BLOCKS.register("gaseous_shadow",()->new GasBlock(true,false));
    public static final RegistryObject<GasBlock> NITOR_GAS=BLOCKS.register("nitor_gas",()->new GasBlock(false,true));
    public static final RegistryObject<Item> GASEOUS_LIGHT=ITEMS.register("gaseous_light",()->new GasItems.Bottle(LIGHT_GAS));
    public static final RegistryObject<Item> GASEOUS_SHADOW=ITEMS.register("gaseous_shadow",()->new GasItems.Bottle(SHADOW_GAS));
    public static final RegistryObject<Item> GAS_REMOVER=ITEMS.register("gas_remover",GasItems.Remover::new);
    public static final RegistryObject<Item> BRIGHT_NITOR=ITEMS.register("bright_nitor",GasItems.BrightNitor::new);
    public static final RegistryObject<Item> DARK_QUARTZ=ITEMS.register("dark_quartz",()->new Item(new Item.Properties()));
    private static BlockBehaviour.Properties quartz(){return BlockBehaviour.Properties.copy(Blocks.QUARTZ_BLOCK).strength(.8f,10f);}
    public static final RegistryObject<Block> QUARTZ_BLOCK=block("dark_quartz_block",()->new Block(quartz()));
    public static final RegistryObject<Block> CHISELED=block("chiseled_dark_quartz",()->new Block(quartz()));
    public static final RegistryObject<RotatedPillarBlock> PILLAR=block("dark_quartz_pillar",()->new RotatedPillarBlock(quartz()));
    public static final RegistryObject<SlabBlock> SLAB=block("dark_quartz_slab",()->new SlabBlock(quartz()));
    public static final RegistryObject<StairBlock> STAIRS=block("dark_quartz_stairs",()->new StairBlock(()->QUARTZ_BLOCK.get().defaultBlockState(),quartz()));
    public static final RegistryObject<FunnelBlock> FUNNEL=block("essentia_funnel",FunnelBlock::new);
    public static final RegistryObject<BlockEntityType<FunnelEntity>> FUNNEL_ENTITY=ENTITIES.register("essentia_funnel",()->BlockEntityType.Builder.of(FunnelEntity::new,FUNNEL.get()).build(null));
    public static final RegistryObject<RestorerBlock> RESTORER=block("restorer",RestorerBlock::new);
    public static final RegistryObject<BlockEntityType<RestorerEntity>> RESTORER_ENTITY=ENTITIES.register("restorer",()->BlockEntityType.Builder.of(RestorerEntity::new,RESTORER.get()).build(null));
    public static final RegistryObject<InterfaceBlock> INTERFACE=block("transvector_interface",InterfaceBlock::new);
    public static final RegistryObject<BlockEntityType<InterfaceEntity>> INTERFACE_ENTITY=ENTITIES.register("transvector_interface",()->BlockEntityType.Builder.of(InterfaceEntity::new,INTERFACE.get()).build(null));
    public static final RegistryObject<Item> CONNECTOR=ITEMS.register("connector",ConnectorItem::new);
    public static final RegistryObject<DislocatorBlock> DISLOCATOR=block("transvector_dislocator",DislocatorBlock::new);
    public static final RegistryObject<BlockEntityType<DislocatorEntity>> DISLOCATOR_ENTITY=ENTITIES.register("transvector_dislocator",()->BlockEntityType.Builder.of(DislocatorEntity::new,DISLOCATOR.get()).build(null));
    public static final RegistryObject<SmeltingFocus> SMELTING_FOCUS=ITEMS.register("focus_smelting",SmeltingFocus::new);
    public static final RegistryObject<TinkererFocus.Flight> FLIGHT_FOCUS=ITEMS.register("focus_flight",TinkererFocus.Flight::new);
    public static final RegistryObject<TinkererFocus.Telekinesis> TELEKINESIS_FOCUS=ITEMS.register("focus_telekinesis",TinkererFocus.Telekinesis::new);
    public static final RegistryObject<MagnetBlock> ITEM_MAGNET=block("item_magnet",()->new MagnetBlock(false));
    public static final RegistryObject<MagnetBlock> MOB_MAGNET=block("mob_magnet",()->new MagnetBlock(true));
    public static final RegistryObject<BlockEntityType<MagnetEntity>> MAGNET_ENTITY=ENTITIES.register("magnet",()->BlockEntityType.Builder.of(MagnetEntity::new,ITEM_MAGNET.get(),MOB_MAGNET.get()).build(null));
    public static final RegistryObject<SoulMouldItem> SOUL_MOULD=ITEMS.register("soul_mould",SoulMouldItem::new);
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<MagnetMenu>> MAGNET_MENU=MENUS.register("mob_magnet",()->net.minecraftforge.common.extensions.IForgeMenuType.create(MagnetMenu::new));
    public static final RegistryObject<TabletBlock> TABLET=block("dynamism_tablet",TabletBlock::new);
    public static final RegistryObject<BlockEntityType<TabletEntity>> TABLET_ENTITY=ENTITIES.register("dynamism_tablet",()->BlockEntityType.Builder.of(TabletEntity::new,TABLET.get()).build(null));
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<TabletMenu>> TABLET_MENU=MENUS.register("dynamism_tablet",()->net.minecraftforge.common.extensions.IForgeMenuType.create(TabletMenu::new));
    public static final RegistryObject<DeflectionFocus> DEFLECTION_FOCUS=ITEMS.register("focus_deflection",DeflectionFocus::new);
    public static final RegistryObject<EnderChestFocus> ENDER_CHEST_FOCUS=ITEMS.register("focus_ender_chest",EnderChestFocus::new);
    public static final RegistryObject<DislocationFocus> DISLOCATION_FOCUS=ITEMS.register("focus_dislocation",DislocationFocus::new);
    public static final RegistryObject<HealingFocus> HEALING_FOCUS=ITEMS.register("focus_healing",HealingFocus::new);
    public static final RegistryObject<XpDrainFocus> XP_DRAIN_FOCUS=ITEMS.register("focus_xp_drain",XpDrainFocus::new);
    public static final RegistryObject<RecallFocus> RECALL_FOCUS=ITEMS.register("focus_recall",RecallFocus::new);
    public static final RegistryObject<ShadowbeamFocus> SHADOWBEAM_FOCUS=ITEMS.register("focus_shadowbeam",ShadowbeamFocus::new);
    public static final RegistryObject<EnchanterBlock> ENCHANTER=block("osmotic_enchanter",EnchanterBlock::new);
    public static final RegistryObject<BlockEntityType<EnchanterEntity>> ENCHANTER_ENTITY=ENTITIES.register("osmotic_enchanter",()->BlockEntityType.Builder.of(EnchanterEntity::new,ENCHANTER.get()).build(null));
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<EnchanterMenu>> ENCHANTER_MENU=MENUS.register("osmotic_enchanter",()->net.minecraftforge.common.extensions.IForgeMenuType.create(EnchanterMenu::new));
    public static final RegistryObject<Item> SPELL_CLOTH=ITEMS.register("spell_cloth",()->new Item(new Item.Properties().durability(35).rarity(Rarity.UNCOMMON)){@Override public boolean isValidRepairItem(ItemStack stack,ItemStack material){return false;}@Override public boolean isEnchantable(ItemStack stack){return false;}@Override public boolean isFoil(ItemStack stack){return false;}});
    public static final RegistryObject<net.minecraft.world.item.crafting.RecipeSerializer<SpellClothRecipe>> SPELL_CLOTH_RECIPE=RECIPES.register("spell_cloth",()->new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(SpellClothRecipe::new));
    public static final RegistryObject<XpTalismanItem> XP_TALISMAN=ITEMS.register("xp_talisman",XpTalismanItem::new);
    public static final RegistryObject<CleansingTalismanItem> CLEANSING_TALISMAN=ITEMS.register("cleansing_talisman",CleansingTalismanItem::new);
    public static final RegistryObject<SummoningTabletBlock> SUMMONING_TABLET=block("summoning_tablet",SummoningTabletBlock::new);
    public static final RegistryObject<BlockEntityType<SummoningTabletEntity>> SUMMONING_TABLET_ENTITY=ENTITIES.register("summoning_tablet",()->BlockEntityType.Builder.of(SummoningTabletEntity::new,SUMMONING_TABLET.get()).build(null));
    public static final RegistryObject<BloodSwordItem> BLOOD_SWORD=ITEMS.register("blood_sword",BloodSwordItem::new);
    public static final java.util.Map<String,RegistryObject<MobEssenceItem>> ESSENCES=essences();
    private static java.util.Map<String,RegistryObject<MobEssenceItem>> essences(){var result=new java.util.LinkedHashMap<String,RegistryObject<MobEssenceItem>>();for(String aspect:MobEssenceItem.ASPECTS)for(int i=0;i<3;i++){int grade=i;String prefix=i==0?"soul_":i==1?"condensed_soul_":"infused_soul_";result.put(aspect+"/"+i,ITEMS.register(prefix+aspect,()->new MobEssenceItem(aspect,grade)));}return java.util.Collections.unmodifiableMap(result);}
    public static final RegistryObject<InfusedInkwellItem> INFUSED_INKWELL=ITEMS.register("infused_inkwell",InfusedInkwellItem::new);
    public static final RegistryObject<RevealingHelmItem> REVEALING_HELM=ITEMS.register("revealing_helm",RevealingHelmItem::new);
    public static final RegistryObject<ShareBookItem> SHARE_BOOK=ITEMS.register("share_book",ShareBookItem::new);
    public static final RegistryObject<net.minecraft.world.item.crafting.RecipeSerializer<ShareBookRecipe>> SHARE_BOOK_RECIPE=RECIPES.register("share_book",()->new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(ShareBookRecipe::new));
    public static final RegistryObject<TravelBlocks.TravelSlab> TRAVEL_SLAB=block("travel_slab",TravelBlocks.TravelSlab::new);
    public static final RegistryObject<TravelBlocks.TravelStairs> TRAVEL_STAIRS=block("travel_stairs",TravelBlocks.TravelStairs::new);
    public static final RegistryObject<TravelBlocks.WardSlab> WARD_SLAB=block("ward_slab",TravelBlocks.WardSlab::new);
    public static final RegistryObject<TravelBlocks.WardBarrier> WARD_BARRIER=BLOCKS.register("ward_barrier",TravelBlocks.WardBarrier::new);
    public static final RegistryObject<PlatformBlock> PLATFORM=block("platform",PlatformBlock::new);
    public static final RegistryObject<BlockEntityType<PlatformEntity>> PLATFORM_ENTITY=ENTITIES.register("platform",()->BlockEntityType.Builder.of(PlatformEntity::new,PLATFORM.get()).build(null));
    public static final java.util.Map<String,RegistryObject<ImbuedFireBlock>> FIRES=fires();
    private static java.util.Map<String,RegistryObject<ImbuedFireBlock>> fires(){var result=new java.util.LinkedHashMap<String,RegistryObject<ImbuedFireBlock>>();for(String aspect:java.util.List.of("aer","ignis","aqua","terra","ordo","perditio"))result.put(aspect,block("fire_"+aspect,()->new ImbuedFireBlock(aspect)));return java.util.Collections.unmodifiableMap(result);}
    public static final RegistryObject<InfusedSeedItem> INFUSED_SEEDS=ITEMS.register("infused_seeds",InfusedSeedItem::new);
    public static final RegistryObject<InfusedCropBlock> INFUSED_CROP=BLOCKS.register("infused_crop",InfusedCropBlock::new);
    public static final RegistryObject<InfusedFarmlandBlock> INFUSED_FARMLAND=BLOCKS.register("infused_farmland",InfusedFarmlandBlock::new);
    public static final RegistryObject<BlockEntityType<InfusedCropEntity>> INFUSED_CROP_ENTITY=ENTITIES.register("infused_crop",()->BlockEntityType.Builder.of(InfusedCropEntity::new,INFUSED_CROP.get()).build(null));
    public static final RegistryObject<BlockEntityType<InfusedFarmlandEntity>> INFUSED_FARMLAND_ENTITY=ENTITIES.register("infused_farmland",()->BlockEntityType.Builder.of(InfusedFarmlandEntity::new,INFUSED_FARMLAND.get()).build(null));
    public static final RegistryObject<ForcefieldBlock> FORCEFIELD=BLOCKS.register("forcefield",ForcefieldBlock::new);
    public static final java.util.Map<String,RegistryObject<InfusedGrainItem>> INFUSED_GRAIN=grains();
    public static final java.util.Map<String,RegistryObject<InfusedPotionItem>> INFUSED_POTIONS=potions();
    private static java.util.Map<String,RegistryObject<InfusedGrainItem>> grains(){var result=new java.util.LinkedHashMap<String,RegistryObject<InfusedGrainItem>>();for(String aspect:java.util.List.of("aer","ignis","terra","aqua"))result.put(aspect,ITEMS.register("infused_grain_"+aspect,()->new InfusedGrainItem(aspect)));return java.util.Collections.unmodifiableMap(result);}
    private static java.util.Map<String,RegistryObject<InfusedPotionItem>> potions(){var result=new java.util.LinkedHashMap<String,RegistryObject<InfusedPotionItem>>();for(String aspect:java.util.List.of("aer","ignis","terra","aqua"))result.put(aspect,ITEMS.register("infused_potion_"+aspect,()->new InfusedPotionItem(aspect)));return java.util.Collections.unmodifiableMap(result);}
    public static final RegistryObject<MobilizerBlock> MOBILIZER=block("mobilizer",()->new MobilizerBlock(false));
    public static final RegistryObject<MobilizerBlock> MOBILIZER_RELAY=block("mobilizer_relay",()->new MobilizerBlock(true));
    public static final RegistryObject<BlockEntityType<MobilizerEntity>> MOBILIZER_ENTITY=ENTITIES.register("mobilizer",()->BlockEntityType.Builder.of(MobilizerEntity::new,MOBILIZER.get()).build(null));
    public static final RegistryObject<BlockEntityType<MobilizerRelayEntity>> MOBILIZER_RELAY_ENTITY=ENTITIES.register("mobilizer_relay",()->BlockEntityType.Builder.of(MobilizerRelayEntity::new,MOBILIZER_RELAY.get()).build(null));
    public static final RegistryObject<SkyPearlItem> SKY_PEARL=ITEMS.register("sky_pearl",SkyPearlItem::new);
    public static final RegistryObject<PlacementMirrorItem> PLACEMENT_MIRROR=ITEMS.register("placement_mirror",PlacementMirrorItem::new);
    public static final RegistryObject<WarpGateBlock> WARP_GATE=block("warp_gate",WarpGateBlock::new);
    public static final RegistryObject<BlockEntityType<WarpGateEntity>> WARP_GATE_ENTITY=ENTITIES.register("warp_gate",()->BlockEntityType.Builder.of(WarpGateEntity::new,WARP_GATE.get()).build(null));
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<WarpGateMenu>> WARP_GATE_MENU=MENUS.register("warp_gate",()->net.minecraftforge.common.extensions.IForgeMenuType.create(WarpGateMenu::new));
    public static final RegistryObject<BlockTalismanItem> BLOCK_TALISMAN=ITEMS.register("block_talisman",BlockTalismanItem::new);
    public static final RegistryObject<IchorPouchItem> ICHOR_POUCH=ITEMS.register("ichor_pouch",IchorPouchItem::new);
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<IchorPouchMenu>> ICHOR_POUCH_MENU=MENUS.register("ichor_pouch",()->net.minecraftforge.common.extensions.IForgeMenuType.create(IchorPouchMenu::new));
    public static final RegistryObject<ProtoclayItem> PROTOCLAY=ITEMS.register("protoclay",ProtoclayItem::new);
    public static final RegistryObject<CatAmuletItem> CAT_AMULET=ITEMS.register("cat_amulet",CatAmuletItem::new);
    public static final RegistryObject<AwakenedMiningItem> AWAKENED_PICK=ITEMS.register("awakened_ichor_pick",()->new AwakenedMiningItem("pick"));
    public static final RegistryObject<AwakenedMiningItem> AWAKENED_SHOVEL=ITEMS.register("awakened_ichor_shovel",()->new AwakenedMiningItem("shovel"));
    public static final RegistryObject<AwakenedAxeItem> AWAKENED_AXE=ITEMS.register("awakened_ichor_axe",AwakenedAxeItem::new);
    public static final RegistryObject<AwakenedSwordItem> AWAKENED_SWORD=ITEMS.register("awakened_ichor_sword",AwakenedSwordItem::new);
    public static final RegistryObject<AwakenedArmorItem> AWAKENED_HELM=ITEMS.register("awakened_ichorcloth_helm",()->new AwakenedArmorItem(ArmorItem.Type.HELMET));
    public static final RegistryObject<AwakenedArmorItem> AWAKENED_CHEST=ITEMS.register("awakened_ichorcloth_chest",()->new AwakenedArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<AwakenedArmorItem> AWAKENED_LEGS=ITEMS.register("awakened_ichorcloth_legs",()->new AwakenedArmorItem(ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<AwakenedArmorItem> AWAKENED_BOOTS=ITEMS.register("awakened_ichorcloth_boots",()->new AwakenedArmorItem(ArmorItem.Type.BOOTS));
    public static final RegistryObject<IchorArmorItem> ICHOR_HELM=ITEMS.register("ichorcloth_helm",()->new IchorArmorItem(ArmorItem.Type.HELMET));
    public static final RegistryObject<IchorArmorItem> ICHOR_CHEST=ITEMS.register("ichorcloth_chest",()->new IchorArmorItem(ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<IchorArmorItem> ICHOR_LEGS=ITEMS.register("ichorcloth_legs",()->new IchorArmorItem(ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<IchorArmorItem> ICHOR_BOOTS=ITEMS.register("ichorcloth_boots",()->new IchorArmorItem(ArmorItem.Type.BOOTS));
    public static final RegistryObject<Item> ICHOR_PICK=ITEMS.register("ichor_pick",IchorTools::pick);
    public static final RegistryObject<Item> ICHOR_SHOVEL=ITEMS.register("ichor_shovel",IchorTools::shovel);
    public static final RegistryObject<Item> ICHOR_AXE=ITEMS.register("ichor_axe",IchorTools::axe);
    public static final RegistryObject<Item> ICHOR_SWORD=ITEMS.register("ichor_sword",IchorTools::sword);
    public static final java.util.Map<String,RegistryObject<Item>> KAMI_MATERIALS=kamiMaterials();
    private static java.util.Map<String,RegistryObject<Item>> kamiMaterials(){var result=new java.util.LinkedHashMap<String,RegistryObject<Item>>();for(String name:java.util.List.of("ichor","ichor_cloth","ichorium","ichor_nugget","ichor_cap","ichorcloth_rod","nether_shard","ender_shard"))result.put(name,ITEMS.register(name,()->new Item(new Item.Properties().rarity(name.endsWith("_shard")?Rarity.COMMON:Rarity.EPIC))));return java.util.Collections.unmodifiableMap(result);}
    public static final RegistryObject<CreativeModeTab> TAB=TABS.register("main",()->CreativeModeTab.builder().title(net.minecraft.network.chat.Component.translatable("itemGroup.thaumic_tinkerer"))
            .icon(Tinkerer::creativeIcon).displayItems((parameters,output)->{ITEMS.getEntries().stream().filter(item->KamiContent.visible(item.get())).filter(item->item.get()!=INFUSED_SEEDS.get()).filter(item->(item.get()!=ASPECT_ANALYZER.get().asItem()&&item.get()!=GOLEM_CONNECTOR.get().asItem())||net.minecraftforge.fml.ModList.get().isLoaded("computercraft")).forEach(item->output.accept(item.get()));CropTraits.PRIMALS.forEach(aspect->output.accept(InfusedSeedItem.seed(aspect,java.util.Map.of())));}).build());
    private static ItemStack creativeIcon() {
        var rod=tc4revival.content.WandParts.Rod.SILVERWOOD;var cap=tc4revival.content.WandParts.Cap.THAUMIUM;
        if(TinkererConfig.KAMI.get()) {rod=tc4revival.content.WandParts.Rod.require(ID+":ichorcloth");cap=tc4revival.content.WandParts.Cap.require(ID+":ichor");}
        return tc4revival.content.TCItems.CASTING_WAND.get().create(rod,cap,false,true);
    }
    private static <T extends Block> RegistryObject<T> block(String name,java.util.function.Supplier<T> factory) {
        var value=BLOCKS.register(name,factory);ITEMS.register(name,()->new BlockItem(value.get(),name.equals("warp_gate")?new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC):new Item.Properties()));return value;
    }
    public Tinkerer(FMLJavaModLoadingContext context) {
        RevivalFeatures.register();
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT,ClientConfig.SPEC);
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,TinkererConfig.SPEC);
        var bus=context.getModEventBus();if(net.minecraftforge.fml.ModList.get().isLoaded("computercraft"))FumeTurtleUpgrade.register(bus);TinkererEnchantments.REGISTRY.register(bus);ElementalEffects.REGISTRY.register(bus);RECIPES.register(bus);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->{KamiContent.setup(); AddonApiBindings.setup();SoulHearts.setup();for(String key:java.util.List.of("PERIPHERALS","ASPECT_ANALYZER","GOLEM_CONNECTOR"))thaumcraft.api.research.ResearchCategories.registerEnablement(ID+":"+key,()->net.minecraftforge.fml.ModList.get().isLoaded("computercraft"));if(net.minecraftforge.fml.ModList.get().isLoaded("computercraft"))ComputerIntegration.setup();EmptyFocusPouchIngredient.register();EmptyBlockTalismanIngredient.register();EmptyXpTalismanIngredient.register();for(String aspect:FIRES.keySet())thaumcraft.api.research.ResearchCategories.registerEnablement(ID+":FIRE_"+aspect.toUpperCase(java.util.Locale.ROOT),()->TinkererConfig.FIRE.get());for(String key:java.util.List.of("DISLOCATOR","FOCUS_DEFLECT","FOCUS_ENDER_CHEST","FOCUS_DISLOCATION","FOCUS_HEAL"))thaumcraft.api.research.ResearchCategories.registerEnablement(ID+":"+key,()->tc4revival.config.TCConfig.SERVER.allowMirrors.get());}));FEATURES.register(bus);BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);TABS.register(bus);MENUS.register(bus);
    }
}
