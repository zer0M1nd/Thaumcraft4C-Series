package conciliummodern;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.*;

public final class ConciliumItems {
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ThaumicConcilium.ID);
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,ThaumicConcilium.ID);
    public static final java.util.Map<String,RegistryObject<Item>> ENTITY_ICONS=new java.util.LinkedHashMap<>();
    static {for(String entity:java.util.List.of("thaumaturge","mad_thaumaturge","golem_bydlo","red_powered_mind","quicksilver_elemental","squid","dope_squid"))ENTITY_ICONS.put(entity,ITEMS.register("entity_icon_"+entity,()->new Item(new Item.Properties()){
        @Override public Component getName(ItemStack stack){return Component.translatable("entity."+(entity.equals("squid")?"minecraft":"thaumicconcilium")+"."+entity);}
    }));}
    public static final RegistryObject<Item> PRIMORDIAL_LIFE=ITEMS.register("primordial_life",PrimordialLife::new);
    public static final RegistryObject<Item> VOID_SLAG=ITEMS.register("void_slag",()->new Item(new Item.Properties()));
    public static final RegistryObject<PolishmentDevice> POLISHMENT_DEVICE=ITEMS.register("polishment_device",PolishmentDevice::new);
    public static final RegistryObject<Protobody> PROTOBODY=ITEMS.register("protobody",Protobody::new);
    public static final RegistryObject<DrainageSyringe> DRAINAGE_SYRINGE=ITEMS.register("drainage_syringe",DrainageSyringe::new);
    public static final java.util.Map<String,RegistryObject<CastingBracelet>> BRACELETS=new java.util.LinkedHashMap<>();
    static {for(var definition:CastingBracelet.definitions())BRACELETS.put(definition.name(),ITEMS.register(definition.name()+"_bracelet",()->new CastingBracelet(definition)));}
    public static final RegistryObject<Item> QUICKSILVER_EGG=ITEMS.register("quicksilver_elemental_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(ConciliumEntities.QUICKSILVER_ELEMENTAL,0x00ffff,0x00008b,new Item.Properties()));
    public static final RegistryObject<Item> RED_MIND_EGG=ITEMS.register("red_powered_mind_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(ConciliumEntities.RED_MIND,0x00ffff,0x00008b,new Item.Properties()));
    public static final RegistryObject<AstralMonitor> ASTRAL_MONITOR=ITEMS.register("astral_monitor",AstralMonitor::new);
    public static final RegistryObject<CoagulatingEyeDrops> EYE_DROPS=ITEMS.register("coagulating_eye_drops",CoagulatingEyeDrops::new);
    // The old custom dropped-item entity only made this material fireproof.
    public static final RegistryObject<Item> NETHER_MEMBRANE=ITEMS.register("nether_membrane",()->new Item(new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<CoolingFocus> COOLING_FOCUS=ITEMS.register("cooling_focus",CoolingFocus::new);
    public static final RegistryObject<EtherealManipulatorFocus> ETHEREAL_MANIPULATOR=ITEMS.register("ethereal_manipulator_focus",EtherealManipulatorFocus::new);
    public static final RegistryObject<SpotlightFocus> SPOTLIGHT=ITEMS.register("spotlight_focus",SpotlightFocus::new);
    public static final RegistryObject<WarpExtractionFocus> WARP_EXTRACTION=ITEMS.register("warp_extraction_focus",WarpExtractionFocus::new);
    public static final RegistryObject<WrathAttractionFocus> WRATH_ATTRACTION=ITEMS.register("wrath_attraction_focus",WrathAttractionFocus::new);
    public static final RegistryObject<TaintAnimationFocus> TAINT_ANIMATION=ITEMS.register("taint_animation_focus",TaintAnimationFocus::new);
    public static final RegistryObject<ScribingFocus> SCRIBING=ITEMS.register("scribing_focus",ScribingFocus::new);
    public static final RegistryObject<AlchemistSpreeFocus> ALCHEMIST_SPREE=ITEMS.register("alchemist_spree_focus",AlchemistSpreeFocus::new);
    public static final RegistryObject<ReflectionFocus> REFLECTION_FOCUS=ITEMS.register("reflection_focus",ReflectionFocus::new);
    public static final RegistryObject<SignalFocus> SIGNAL_FOCUS=ITEMS.register("signal_focus",SignalFocus::new);
    public static final RegistryObject<ImpulseFocus> IMPULSE_FOCUS=ITEMS.register("impulse_focus",ImpulseFocus::new);
    public static final RegistryObject<PositiveBurstFocus> POSITIVE_FOCUS=ITEMS.register("positive_burst_focus",PositiveBurstFocus::new);
    public static final RegistryObject<Item> CLEAR_WATER=ITEMS.register("clear_water_bottle",ClearWaterBottle::new);
    public static final RegistryObject<Item> TIGHT_BELT=ITEMS.register("tight_belt",TightBelt::new);
    public static final RegistryObject<Item> BURDENING_AMULET=ITEMS.register("burdening_amulet",BurdeningAmulet::new);
    public static final RegistryObject<Item> BLUSTERING_RING=ITEMS.register("ring_of_blustering_light",BlusteringLightRing::new);
    public static final RegistryObject<Item> THAUMATURGE_DRUM=ITEMS.register("thaumaturge_drum",()->new ThaumaturgeDrum(false));
    public static final RegistryObject<Item> WARPED_DRUM=ITEMS.register("warped_drum",()->new ThaumaturgeDrum(true));
    public static final RegistryObject<ShardMill> SHARD_MILL=ITEMS.register("shard_mill",()->new ShardMill(false));
    public static final RegistryObject<ShardMill> SILVERWOOD_SHARD_MILL=ITEMS.register("silverwood_shard_mill",()->new ShardMill(true));
    public static final RegistryObject<DumpJackboots> DUMP_JACKBOOTS=ITEMS.register("dump_jackboots",DumpJackboots::new);
    public static final RegistryObject<ElementalSurferShorts> SURFER_SHORTS=ITEMS.register("elemental_surfer_shorts",ElementalSurferShorts::new);
    public static final RegistryObject<Item> BRIGHTEST_EGG=ITEMS.register("brightest_one_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(ConciliumEntities.BRIGHTEST,0x00ffff,0x111111,new Item.Properties()));
    public static final RegistryObject<Item> THICK_TAINT=ITEMS.register("thick_taint_bottle",ThickTaintBottle::new);
    public static final RegistryObject<VisConductorFocus> VIS_CONDUCTOR=ITEMS.register("vis_conductor_focus",VisConductorFocus::new);
    public static final RegistryObject<Item> RUNIC_BODY=ITEMS.register("runic_body_windings",()->new RunicWindings(ArmorItem.Type.CHESTPLATE));
    public static final RegistryObject<Item> RUNIC_LEGS=ITEMS.register("runic_legs_windings",()->new RunicWindings(ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> MAD_EGG=ITEMS.register("mad_thaumaturge_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(ConciliumEntities.MAD_THAUMATURGE,0x00ffff,0x111111,new Item.Properties()));
    public static final RegistryObject<Item> EXPLORER_EGG=ITEMS.register("nether_explorer_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(ConciliumEntities.NETHER_EXPLORER,0x00ffff,0x00008b,new Item.Properties()));
    public static final RegistryObject<Item> THAUMATURGE_EGG=ITEMS.register("thaumaturge_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(ConciliumEntities.THAUMATURGE,0x00ffff,0x00008b,new Item.Properties()));
    public static final RegistryObject<CreativeModeTab> TAB=TABS.register("main",()->CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.thaumicconcilium"))
            .icon(()->new ItemStack(ENTITY_ICONS.get("thaumaturge").get()))
            .displayItems((parameters,output)->{ITEMS.getEntries().forEach(entry->{var item=entry.get();String id=entry.getId().getPath();
                if(id.equals("void_reality")||id.equals("hex_of_predictability")||id.startsWith("entity_icon_"))return;
                var mods=net.minecraftforge.fml.ModList.get();
                if((id.equals("flesh_crucible")||id.equals("solid_void"))&&!mods.isLoaded("taintedmagic"))return;
                if(id.equals("protobody")&&!(mods.isLoaded("taintedmagic")&&mods.isLoaded("thaumichorizons")))return;
                if((id.equals("red_powered_mind")||id.equals("red_powered_mind_spawn_egg"))&&!(mods.isLoaded("automagy")&&mods.isLoaded("thaumichorizons")))return;
                if(item instanceof DimensionalGem&&!ConciliumGems.available())return;
                if((id.equals("crimson_archer_spawn_egg")||id.equals("crimson_ranger_spawn_egg"))&&!CrimsonContent.dyes())return;
                if((id.startsWith("pontifex_")||id.equals("crimson_annales")||id.equals("crimson_paladin_spawn_egg")||id.equals("crimson_pontifex_spawn_egg")||id.equals("lesser_portal_spawn_egg"))&&!CrimsonContent.available())return;
                if(item instanceof CastingBracelet bracelet){if(bracelet.available()){var stack=new ItemStack(item);bracelet.initializeParts(stack);output.accept(stack);}}
                else if(!(item instanceof DrainageSyringe)||DrainageSyringe.available())output.accept(item);
            });ConciliumGolems.creative(output);})
            .build());
    private ConciliumItems() {}
}
