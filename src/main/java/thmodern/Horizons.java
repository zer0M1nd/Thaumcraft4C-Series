package thmodern;

import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import thmodern.node.*;

@Mod(Horizons.ID)
public final class Horizons {
 public static final String ID="thaumichorizons";
 public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,ID);
 public static final RegistryObject<net.minecraft.sounds.SoundEvent> VORTEX_SOUND=SOUNDS.register("vortex",()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.parse(ID+":vortex")));
 public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,ID);
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ID);
 public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,ID);
 public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,ID);
 public static final RegistryObject<thmodern.node.TransductionAmplifierBlock> TRANSDUCTION_AMPLIFIER=BLOCKS.register("transduction_amplifier",thmodern.node.TransductionAmplifierBlock::new);
 public static final RegistryObject<BlockEntityType<thmodern.node.TransductionAmplifierEntity>> TRANSDUCTION_AMPLIFIER_ENTITY=ENTITIES.register("transduction_amplifier",()->BlockEntityType.Builder.of(thmodern.node.TransductionAmplifierEntity::new,TRANSDUCTION_AMPLIFIER.get()).build(null));
 static {ITEMS.register("transduction_amplifier",()->new DeviceBlockItem(TRANSDUCTION_AMPLIFIER.get()));}
 public static final RegistryObject<Item> CRYSTAL_WAND=ITEMS.register("crystal_wand",()->new Item(new Item.Properties().stacksTo(1)));
 public static final RegistryObject<thmodern.node.DisposableWandItem> DISPOSABLE_WAND=ITEMS.register("disposable_wand",thmodern.node.DisposableWandItem::new);
 public static final RegistryObject<Block> VAT=BLOCKS.register("vat",thmodern.vat.VatBlock::new);
 public static final RegistryObject<Block> VORTEX=BLOCKS.register("vortex",thmodern.plane.VortexBlock::new);
 public static final RegistryObject<BlockEntityType<thmodern.plane.VortexEntity>> VORTEX_ENTITY=ENTITIES.register("vortex",()->BlockEntityType.Builder.of(thmodern.plane.VortexEntity::new,VORTEX.get()).build(null));
 public static final RegistryObject<Item> VORTEX_ITEM=ITEMS.register("vortex",()->new BlockItem(VORTEX.get(),new Item.Properties())),STABLE_VORTEX_ITEM=ITEMS.register("vortex_stable",()->new BlockItem(VORTEX.get(),new Item.Properties()){@Override public String getDescriptionId(){return "item.thaumichorizons.vortex_stable";}});
 public static final RegistryObject<Block> VORTEX_STABILIZER=BLOCKS.register("vortex_stabilizer",thmodern.plane.VortexStabilizerBlock::new);
 public static final RegistryObject<BlockEntityType<thmodern.plane.VortexStabilizerEntity>> VORTEX_STABILIZER_ENTITY=ENTITIES.register("vortex_stabilizer",()->BlockEntityType.Builder.of(thmodern.plane.VortexStabilizerEntity::new,VORTEX_STABILIZER.get()).build(null));
 static {ITEMS.register("vortex_stabilizer",()->new BlockItem(VORTEX_STABILIZER.get(),new Item.Properties()));}
 public static final RegistryObject<Block> ALCHEMITE=BLOCKS.register("alchemite",thmodern.alchemy.AlchemiteBlock::new);
 static {ITEMS.register("alchemite",()->new BlockItem(ALCHEMITE.get(),new Item.Properties()));}
 public static final RegistryObject<Block> LIGHT=BLOCKS.register("light",()->new thmodern.focus.LightBlock(false)),SOLAR_LIGHT=BLOCKS.register("solar_light",()->new thmodern.focus.LightBlock(true));
 public static final RegistryObject<Item> GREATWOOD_BOAT_ITEM=ITEMS.register("boat_greatwood",()->new thmodern.boat.HorizonsBoatItem(false)),THAUMIUM_BOAT_ITEM=ITEMS.register("boat_thaumium",()->new thmodern.boat.HorizonsBoatItem(true));
 public static final RegistryObject<Item> KEYSTONE=ITEMS.register("keystone",thmodern.plane.KeystoneItem::new);
 public static final RegistryObject<Item> VOID_PUTTY=ITEMS.register("void_putty",()->new Item(new Item.Properties().stacksTo(16)));
 public static final RegistryObject<Item> DISINTEGRATION_FOCUS=ITEMS.register("focus_disintegration",thmodern.focus.DisintegrationFocusItem::new);
 public static final RegistryObject<Item> LIQUEFACTION_FOCUS=ITEMS.register("focus_liquefaction",thmodern.focus.LiquefactionFocusItem::new);
 public static final java.util.List<RegistryObject<Item>> ILLUMINATION_FOCI=registerIllumination();
 private static java.util.List<RegistryObject<Item>> registerIllumination(){var result=new java.util.ArrayList<RegistryObject<Item>>();for(int i=0;i<16;i++){int shade=i;result.add(ITEMS.register("focus_illumination_"+thmodern.focus.IlluminationFocusItem.COLORS.get(i),()->new thmodern.focus.IlluminationFocusItem(shade)));}return java.util.List.copyOf(result);}
 public static final RegistryObject<Block> SOULFORGE=BLOCKS.register("soulforge",thmodern.soul.SoulforgeBlock::new);
 public static final RegistryObject<Block> SOUL_BEACON=BLOCKS.register("soul_beacon",thmodern.soul.SoulBeaconBlock::new);
 public static final RegistryObject<BlockEntityType<thmodern.soul.SoulBeaconEntity>> SOUL_BEACON_ENTITY=ENTITIES.register("soul_beacon",()->BlockEntityType.Builder.of(thmodern.soul.SoulBeaconEntity::new,SOUL_BEACON.get()).build(null));
 static {ITEMS.register("soul_beacon",()->new BlockItem(SOUL_BEACON.get(),new Item.Properties()));}
 public static final RegistryObject<Block> INSPIRATRON=BLOCKS.register("inspiratron",thmodern.soul.InspiratronBlock::new);
 public static final RegistryObject<BlockEntityType<thmodern.soul.InspiratronEntity>> INSPIRATRON_ENTITY=ENTITIES.register("inspiratron",()->BlockEntityType.Builder.of(thmodern.soul.InspiratronEntity::new,INSPIRATRON.get()).build(null));
 static {ITEMS.register("inspiratron",()->new BlockItem(INSPIRATRON.get(),new Item.Properties()));}
 public static final RegistryObject<Block> SOUL_EXTRACTOR=BLOCKS.register("soul_extractor",thmodern.soul.SoulExtractorBlock::new);
 public static final RegistryObject<BlockEntityType<thmodern.soul.SoulExtractorEntity>> SOUL_EXTRACTOR_ENTITY=ENTITIES.register("soul_extractor",()->BlockEntityType.Builder.of(thmodern.soul.SoulExtractorEntity::new,SOUL_EXTRACTOR.get()).build(null));
 static {ITEMS.register("soul_extractor",()->new BlockItem(SOUL_EXTRACTOR.get(),new Item.Properties()));}
 public static final RegistryObject<BlockEntityType<thmodern.soul.SoulforgeEntity>> SOULFORGE_ENTITY=ENTITIES.register("soulforge",()->BlockEntityType.Builder.of(thmodern.soul.SoulforgeEntity::new,SOULFORGE.get()).build(null));
 static {ITEMS.register("soulforge",()->new BlockItem(SOULFORGE.get(),new Item.Properties()));}
 public static final RegistryObject<BlockEntityType<thmodern.vat.VatEntity>> VAT_ENTITY=ENTITIES.register("vat",()->BlockEntityType.Builder.of(thmodern.vat.VatEntity::new,VAT.get()).build(null));
 public static final RegistryObject<BlockEntityType<thmodern.vat.VatPartEntity>> VAT_PART_ENTITY=ENTITIES.register("vat_part",()->BlockEntityType.Builder.of(thmodern.vat.VatPartEntity::new,VAT.get()).build(null));
 public static final RegistryObject<Item> NUTRIENTS=ITEMS.register("nutrients",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> CORPSE_EFFIGY=ITEMS.register("corpse_effigy",thmodern.vat.CorpseEffigyItem::new);
 public static final RegistryObject<Item> MIRROR_AMULET=ITEMS.register("mirror_amulet",thmodern.soul.MirrorAmuletItem::new);
 public static final RegistryObject<Item> LENS_CASE=ITEMS.register("lens_case",thmodern.lens.LensCaseItem::new);
 public static final java.util.Map<String,RegistryObject<Item>> LENSES=registerLenses();
 private static java.util.Map<String,RegistryObject<Item>> registerLenses(){var result=new java.util.LinkedHashMap<String,RegistryObject<Item>>();for(String key:java.util.List.of("fire","water","air","earth","order_entropy"))result.put(key,ITEMS.register("lens_"+key,()->new thmodern.lens.LensItem(key)));return java.util.Collections.unmodifiableMap(result);}
 public static final RegistryObject<Item> CONTAINMENT_FOCUS=ITEMS.register("focus_containment",thmodern.containment.ContainmentFocusItem::new);
 static {for(var key:thmodern.vat.CreatureInfusions.TYPES)ITEMS.register("infusion_"+key,()->new thmodern.vat.InfusionIconItem(key));}
 static {for(var key:thmodern.vat.SelfInfusions.TYPES)ITEMS.register("self_infusion_"+key,()->new thmodern.vat.SelfInfusionIconItem(key));}
 public static final RegistryObject<Block> CREATURE_JAR=BLOCKS.register("creature_jar",thmodern.containment.CreatureJarBlock::new);
 public static final RegistryObject<Item> CREATURE_JAR_ITEM=ITEMS.register("creature_jar",thmodern.containment.CreatureJarItem::new);
 public static final RegistryObject<BlockEntityType<thmodern.containment.CreatureJarEntity>> CREATURE_JAR_ENTITY=ENTITIES.register("creature_jar",()->BlockEntityType.Builder.of(thmodern.containment.CreatureJarEntity::new,CREATURE_JAR.get()).build(null));
 public static final RegistryObject<Block> VAT_MATRIX=BLOCKS.register("modified_matrix",thmodern.vat.VatMatrixBlock::new);
 public static final RegistryObject<BlockEntityType<thmodern.vat.VatMatrixEntity>> VAT_MATRIX_ENTITY=ENTITIES.register("modified_matrix",()->BlockEntityType.Builder.of(thmodern.vat.VatMatrixEntity::new,VAT_MATRIX.get()).build(null));
 static {ITEMS.register("modified_matrix",()->new thmodern.vat.VatMatrixItem(VAT_MATRIX.get()));}
 public static final RegistryObject<NodeMonitorBlock> NODE_MONITOR=BLOCKS.register("node_monitor",NodeMonitorBlock::new);
 public static final RegistryObject<Item> PLANAR_CONDUIT=ITEMS.register("planar_conduit",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> GOLEM_POWDER=ITEMS.register("golem_powder",thmodern.golem.GolemPowderItem::new);
 public static final RegistryObject<Item> ANIMATION_FOCUS=ITEMS.register("focus_animation",thmodern.golem.AnimationFocusItem::new);
 public static final RegistryObject<Item> GOLEM_PLACER=ITEMS.register("golem_placer",thmodern.golem.AnimatedGolemPlacerItem::new);
 public static final RegistryObject<Item> GOLEM_BELL=ITEMS.register("golem_bell",thmodern.golem.HorizonsBellItem::new);
 public static final RegistryObject<Item> EMPTY_SYRINGE=ITEMS.register("empty_syringe",thmodern.blood.EmptySyringeItem::new);
 public static final RegistryObject<Item> INCUBATED_EGG=ITEMS.register("incubated_egg",thmodern.blood.IncubatedEggItem::new);
 public static final RegistryObject<Item> INJECTOR=ITEMS.register("injector",thmodern.blood.InjectorItem::new);
 public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> MOBS=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,ID);
 public static final RegistryObject<net.minecraft.world.entity.EntityType<thmodern.boat.HorizonsBoat>> GREATWOOD_BOAT=MOBS.register("boat_greatwood",()->net.minecraft.world.entity.EntityType.Builder.<thmodern.boat.HorizonsBoat>of((type,level)->new thmodern.boat.HorizonsBoat(type,level,false),net.minecraft.world.entity.MobCategory.MISC).sized(1.375f,.5625f).clientTrackingRange(10).updateInterval(3).build("thaumichorizons:boat_greatwood")),THAUMIUM_BOAT=MOBS.register("boat_thaumium",()->net.minecraft.world.entity.EntityType.Builder.<thmodern.boat.HorizonsBoat>of((type,level)->new thmodern.boat.HorizonsBoat(type,level,true),net.minecraft.world.entity.MobCategory.MISC).sized(1.375f,.5625f).fireImmune().clientTrackingRange(10).updateInterval(3).build("thaumichorizons:boat_thaumium"));
 public static final RegistryObject<net.minecraft.world.entity.EntityType<thmodern.alchemy.AlchemiteEntity>> ALCHEMITE_ENTITY=MOBS.register("alchemite_primed",()->net.minecraft.world.entity.EntityType.Builder.<thmodern.alchemy.AlchemiteEntity>of(thmodern.alchemy.AlchemiteEntity::new,net.minecraft.world.entity.MobCategory.MISC).sized(.98f,.98f).clientTrackingRange(10).updateInterval(10).build("thaumichorizons:alchemite_primed"));
 public static final RegistryObject<net.minecraft.world.entity.EntityType<thmodern.containment.SoulEntity>> SOUL=MOBS.register("soul",()->net.minecraft.world.entity.EntityType.Builder.<thmodern.containment.SoulEntity>of(thmodern.containment.SoulEntity::new,net.minecraft.world.entity.MobCategory.MISC).sized(.9f,.9f).clientTrackingRange(10).build(ID+":soul"));
 public static final RegistryObject<net.minecraft.world.entity.EntityType<thmodern.creature.InfusedCat>> GUARDIAN_PANTHER=infusedCat("guardian_panther",1.2f,1.6f),GRAVEKEEPER=infusedCat("gravekeeper",.6f,.7f),FAMILIAR=infusedCat("familiar",.6f,.7f);
 private static RegistryObject<net.minecraft.world.entity.EntityType<thmodern.creature.InfusedCat>> infusedCat(String key,float width,float height){return MOBS.register(key,()->net.minecraft.world.entity.EntityType.Builder.<thmodern.creature.InfusedCat>of(thmodern.creature.InfusedCat::new,net.minecraft.world.entity.MobCategory.CREATURE).sized(width,height).clientTrackingRange(10).build(ID+":"+key));}
 public static final RegistryObject<net.minecraft.world.entity.EntityType<thmodern.creature.NightmareHorse>> NIGHTMARE=MOBS.register("nightmare",()->net.minecraft.world.entity.EntityType.Builder.<thmodern.creature.NightmareHorse>of(thmodern.creature.NightmareHorse::new,net.minecraft.world.entity.MobCategory.CREATURE).sized(1.3964844f,1.6f).fireImmune().clientTrackingRange(10).build(ID+":nightmare"));
 public static final RegistryObject<net.minecraft.world.entity.EntityType<thmodern.creature.InfusedHorse>> ENDERSTEED=infusedHorse("endersteed"),UNDEAD_HORSE=infusedHorse("undead_horse"),SKELETAL_HORSE=infusedHorse("skeletal_horse");
 private static RegistryObject<net.minecraft.world.entity.EntityType<thmodern.creature.InfusedHorse>> infusedHorse(String key){return MOBS.register(key,()->net.minecraft.world.entity.EntityType.Builder.<thmodern.creature.InfusedHorse>of(thmodern.creature.InfusedHorse::new,net.minecraft.world.entity.MobCategory.CREATURE).sized(1.3964844f,1.6f).clientTrackingRange(10).build(ID+":"+key));}
 public static final RegistryObject<net.minecraft.world.entity.EntityType<thmodern.creature.InfusedWolf>> SEAWOLF=infusedWolf("seawolf"),LUNAR_WOLF=infusedWolf("lunar_wolf"),NETHER_HOUND=infusedWolf("nether_hound");
 private static RegistryObject<net.minecraft.world.entity.EntityType<thmodern.creature.InfusedWolf>> infusedWolf(String key){return MOBS.register(key,()->net.minecraft.world.entity.EntityType.Builder.<thmodern.creature.InfusedWolf>of(thmodern.creature.InfusedWolf::new,net.minecraft.world.entity.MobCategory.CREATURE).sized(.6f,.85f).clientTrackingRange(10).build(ID+":"+key));}
 public static final RegistryObject<net.minecraft.world.entity.EntityType<thmodern.creature.Sheeder>> SHEEDER=MOBS.register("sheeder",()->net.minecraft.world.entity.EntityType.Builder.<thmodern.creature.Sheeder>of(thmodern.creature.Sheeder::new,net.minecraft.world.entity.MobCategory.CREATURE).sized(1.4f,.9f).clientTrackingRange(10).build(ID+":sheeder"));
 public static final RegistryObject<net.minecraft.world.entity.EntityType<thmodern.creature.TaintEaterPig>> TAINT_EATER_PIG=MOBS.register("taint_eater_pig",()->net.minecraft.world.entity.EntityType.Builder.<thmodern.creature.TaintEaterPig>of(thmodern.creature.TaintEaterPig::new,net.minecraft.world.entity.MobCategory.CREATURE).sized(.9f,.9f).clientTrackingRange(10).build(ID+":taint_eater_pig"));
 public static final RegistryObject<net.minecraft.world.entity.EntityType<thmodern.creature.OrePig>> ORE_PIG=MOBS.register("ore_pig",()->net.minecraft.world.entity.EntityType.Builder.<thmodern.creature.OrePig>of(thmodern.creature.OrePig::new,net.minecraft.world.entity.MobCategory.CREATURE).sized(.9f,.9f).clientTrackingRange(10).build(ID+":ore_pig"));
 public static final RegistryObject<net.minecraft.world.entity.EntityType<thmodern.creature.ChocolateCow>> CHOCOLATE_COW=MOBS.register("chocolate_cow",()->net.minecraft.world.entity.EntityType.Builder.<thmodern.creature.ChocolateCow>of(thmodern.creature.ChocolateCow::new,net.minecraft.world.entity.MobCategory.CREATURE).sized(.9f,1.4f).clientTrackingRange(10).build(ID+":chocolate_cow"));
 public static final RegistryObject<net.minecraft.world.entity.EntityType<thmodern.creature.InfusedSheep>> CHROMATIC_SHEEP=infusedSheep("chromatic_sheep"),SELF_SHEARING_SHEEP=infusedSheep("self_shearing_sheep");
 private static RegistryObject<net.minecraft.world.entity.EntityType<thmodern.creature.InfusedSheep>> infusedSheep(String key){return MOBS.register(key,()->net.minecraft.world.entity.EntityType.Builder.<thmodern.creature.InfusedSheep>of(thmodern.creature.InfusedSheep::new,net.minecraft.world.entity.MobCategory.CREATURE).sized(.9f,1.3f).clientTrackingRange(10).build(ID+":"+key));}
 public static final RegistryObject<Item> CHOCOLATE_MILK=ITEMS.register("chocolate_milk",thmodern.creature.ChocolateMilkItem::new);
 public static final RegistryObject<Item> CHOCOLATE_BAR=ITEMS.register("chocolate_bar",()->new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(1).saturationMod(2).build())));
 public static final RegistryObject<Item> ICE_CREAM=ITEMS.register("ice_cream",()->new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationMod(4).build())));
 public static final RegistryObject<Block> CHOCOLATE_BLOCK=BLOCKS.register("chocolate",()->new Block(Block.Properties.of().strength(.5f,.5f).sound(SoundType.WOOD)));
 static {ITEMS.register("chocolate",()->new BlockItem(CHOCOLATE_BLOCK.get(),new Item.Properties()));}
 public static final RegistryObject<net.minecraft.world.entity.EntityType<thmodern.creature.InfusedChicken>> SCHOLAR_CHICKEN=infusedChicken("scholar_chicken"),GOLD_CHICKEN=infusedChicken("gold_chicken");
 private static RegistryObject<net.minecraft.world.entity.EntityType<thmodern.creature.InfusedChicken>> infusedChicken(String key){return MOBS.register(key,()->net.minecraft.world.entity.EntityType.Builder.<thmodern.creature.InfusedChicken>of(thmodern.creature.InfusedChicken::new,net.minecraft.world.entity.MobCategory.CREATURE).sized(.4f,.7f).clientTrackingRange(10).build(ID+":"+key));}
 public static final RegistryObject<Item> INK_EGG=ITEMS.register("ink_egg",()->new Item(new Item.Properties().stacksTo(16))),GOLD_EGG=ITEMS.register("gold_egg",()->new Item(new Item.Properties().stacksTo(16)));
 public static final RegistryObject<net.minecraft.world.entity.EntityType<thmodern.blood.SyringeProjectile>> SYRINGE_PROJECTILE=MOBS.register("syringe",()->net.minecraft.world.entity.EntityType.Builder.<thmodern.blood.SyringeProjectile>of(thmodern.blood.SyringeProjectile::new,net.minecraft.world.entity.MobCategory.MISC).sized(.5f,.5f).clientTrackingRange(10).updateInterval(1).build("thaumichorizons:syringe"));
 public static final RegistryObject<Item> HUMAN_BLOOD=ITEMS.register("human_blood",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> BLOOD_PHIAL=ITEMS.register("blood_phial",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> BLOOD_SAMPLE=ITEMS.register("blood_sample",thmodern.blood.BloodSampleItem::new);
 public static final RegistryObject<Item> INJECTION=ITEMS.register("injection",()->new thmodern.blood.InjectionItem(false));
 public static final RegistryObject<Item> INJECTION_PHIAL=ITEMS.register("injection_phial",()->new thmodern.blood.InjectionItem(true));
 public static final RegistryObject<Block> BLOOD_INFUSER=BLOCKS.register("blood_infuser",thmodern.blood.BloodInfuserBlock::new);
 public static final RegistryObject<BlockEntityType<thmodern.blood.BloodInfuserEntity>> BLOOD_INFUSER_ENTITY=ENTITIES.register("blood_infuser",()->BlockEntityType.Builder.of(thmodern.blood.BloodInfuserEntity::new,BLOOD_INFUSER.get()).build(null));
 public static final RegistryObject<Item> NODE_MONITOR_ITEM=ITEMS.register("node_monitor",()->new DeviceBlockItem(NODE_MONITOR.get()));
 public static final RegistryObject<BlockEntityType<NodeMonitorEntity>> NODE_MONITOR_ENTITY=ENTITIES.register("node_monitor",()->BlockEntityType.Builder.of(NodeMonitorEntity::new,NODE_MONITOR.get()).build(null));
 public static final RegistryObject<SyntheticNodeBlock> SYNTHETIC_NODE=BLOCKS.register("synthetic_node",SyntheticNodeBlock::new);
 public static final RegistryObject<BlockEntityType<SyntheticNodeEntity>> SYNTHETIC_NODE_ENTITY=ENTITIES.register("synthetic_node",()->BlockEntityType.Builder.of(SyntheticNodeEntity::new,SYNTHETIC_NODE.get()).build(null));
 public static final RegistryObject<VisDynamoBlock> VIS_DYNAMO=BLOCKS.register("vis_dynamo",VisDynamoBlock::new);
 public static final RegistryObject<BlockEntityType<VisDynamoEntity>> VIS_DYNAMO_ENTITY=ENTITIES.register("vis_dynamo",()->BlockEntityType.Builder.of(VisDynamoEntity::new,VIS_DYNAMO.get()).build(null));
 public static final RegistryObject<Block> ESSENTIA_DYNAMO=BLOCKS.register("essentia_dynamo",()->new NodeDeviceBlock<>(Block.Properties.of().strength(.7f,1).sound(SoundType.GLASS).lightLevel(s->7),()->Horizons.ESSENTIA_DYNAMO_ENTITY.get(),EssentiaDynamoEntity::tick));
 public static final RegistryObject<BlockEntityType<EssentiaDynamoEntity>> ESSENTIA_DYNAMO_ENTITY=ENTITIES.register("essentia_dynamo",()->BlockEntityType.Builder.of(EssentiaDynamoEntity::new,ESSENTIA_DYNAMO.get()).build(null));
 public static final RegistryObject<Block> RECOMBINATOR=BLOCKS.register("recombinator",()->new NodeDeviceBlock<>(Block.Properties.of().strength(0),()->Horizons.RECOMBINATOR_ENTITY.get(),RecombinatorEntity::tick));
 public static final RegistryObject<BlockEntityType<RecombinatorEntity>> RECOMBINATOR_ENTITY=ENTITIES.register("recombinator",()->BlockEntityType.Builder.of(RecombinatorEntity::new,RECOMBINATOR.get()).build(null));
 public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,ID);
 public static final RegistryObject<net.minecraft.world.inventory.MenuType<thmodern.lens.LensCaseMenu>> LENS_CASE_MENU=MENUS.register("lens_case",()->net.minecraftforge.common.extensions.IForgeMenuType.create(thmodern.lens.LensCaseMenu::new));
 public static final RegistryObject<net.minecraft.world.inventory.MenuType<thmodern.soul.SoulDeviceMenu>> SOUL_DEVICE_MENU=MENUS.register("soul_device",()->net.minecraftforge.common.extensions.IForgeMenuType.create(thmodern.soul.SoulDeviceMenu::new));
 public static final RegistryObject<net.minecraft.world.inventory.MenuType<thmodern.vat.MorphicFingersMenu>> FINGERS_MENU=MENUS.register("morphic_fingers",()->net.minecraftforge.common.extensions.IForgeMenuType.create(thmodern.vat.MorphicFingersMenu::new));
 public static final RegistryObject<net.minecraft.world.inventory.MenuType<thmodern.vat.VatMenu>> VAT_MENU=MENUS.register("vat",()->net.minecraftforge.common.extensions.IForgeMenuType.create(thmodern.vat.VatMenu::new));
 public static final RegistryObject<net.minecraft.world.inventory.MenuType<VisDynamoMenu>> VIS_DYNAMO_MENU=MENUS.register("vis_dynamo",()->net.minecraftforge.common.extensions.IForgeMenuType.create(VisDynamoMenu::new));
 public static final RegistryObject<net.minecraft.world.inventory.MenuType<thmodern.blood.BloodInfuserMenu>> BLOOD_INFUSER_MENU=MENUS.register("blood_infuser",()->net.minecraftforge.common.extensions.IForgeMenuType.create(thmodern.blood.BloodInfuserMenu::new));
 public static final RegistryObject<net.minecraft.world.inventory.MenuType<thmodern.blood.InjectorMenu>> INJECTOR_MENU=MENUS.register("injector",()->net.minecraftforge.common.extensions.IForgeMenuType.create(thmodern.blood.InjectorMenu::new));
 static {ITEMS.register("blood_infuser",()->new BlockItem(BLOOD_INFUSER.get(),new Item.Properties()));}
 static { for(var block:java.util.List.of(SYNTHETIC_NODE,VIS_DYNAMO,ESSENTIA_DYNAMO,RECOMBINATOR))ITEMS.register(block.getId().getPath(),()->new DeviceBlockItem(block.get())); }
 public static final RegistryObject<CreativeModeTab> TAB=TABS.register("main",()->CreativeModeTab.builder().title(net.minecraft.network.chat.Component.literal("Thaumic Horizons")).icon(()->new ItemStack(NODE_MONITOR_ITEM.get())).displayItems((p,o)->ITEMS.getEntries().forEach(i->o.accept(i.get()==DISPOSABLE_WAND.get()?DISPOSABLE_WAND.get().full():new ItemStack(i.get())))).build());
 public Horizons(FMLJavaModLoadingContext context){
        RevivalFeatures.register();LegacyConfig.register();thmodern.plane.PlanarBlocks.register();thmodern.plane.PlanarDecorations.register();thmodern.plane.CloudBlocks.register();thmodern.plane.GatewayBlocks.register();thmodern.creature.PlanarSlimes.register();thmodern.creature.HorizonsSpawnEggs.register();thmodern.misc.CreativeTools.register();thmodern.vat.BodyNetwork.register();thmodern.lens.LensNetwork.register();var bus=context.getModEventBus();thmodern.focus.FocusRecipes.SERIALIZERS.register(bus);SOUNDS.register(bus);BLOCKS.register(bus);ITEMS.register(bus);MOBS.register(bus);ENTITIES.register(bus);TABS.register(bus);MENUS.register(bus);thmodern.blood.BloodEffects.REGISTRY.register(bus);bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(thmodern.golem.GolemProfiles::register));}
}





