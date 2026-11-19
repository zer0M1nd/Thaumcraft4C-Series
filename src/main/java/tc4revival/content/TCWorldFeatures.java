package tc4revival.content;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.levelgen.feature.Feature;

public  class TCWorldFeatures {
    private static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(ForgeRegistries.FEATURES,"thaumcraft");
    public static final RegistryObject<ForestDecorationFeature> FOREST_DECORATION=FEATURES.register("forest_decoration",ForestDecorationFeature::new);
    public static final RegistryObject<ForestOresFeature> FOREST_ORES=FEATURES.register("forest_ores",ForestOresFeature::new);
    public static final RegistryObject<LegacyVegetationFeature> VEGETATION=FEATURES.register("legacy_vegetation",LegacyVegetationFeature::new);
    public static final RegistryObject<ForestVegetationFeature> FOREST_VEGETATION=FEATURES.register("forest_vegetation",ForestVegetationFeature::new);
    public static final RegistryObject<ForestSedimentsFeature> FOREST_SEDIMENTS=FEATURES.register("forest_sediments",ForestSedimentsFeature::new);
    public static final RegistryObject<ForestSpringsFeature> FOREST_SPRINGS=FEATURES.register("forest_springs",ForestSpringsFeature::new);
    public static final RegistryObject<ManaPodFeature> MANA_POD=FEATURES.register("mana_pod",ManaPodFeature::new);
    public static final RegistryObject<ForestUnderstoryFeature> FOREST_UNDERSTORY=FEATURES.register("forest_understory",ForestUnderstoryFeature::new);
    public static final RegistryObject<ForestMossFeature> FOREST_MOSS=FEATURES.register("forest_moss",ForestMossFeature::new);
    public static final RegistryObject<ForestMushroomFeature> FOREST_MUSHROOM=FEATURES.register("forest_mushroom",ForestMushroomFeature::new);
    public static final RegistryObject<ForestFlowersFeature> FOREST_FLOWERS=FEATURES.register("forest_flowers",ForestFlowersFeature::new);
    public static final RegistryObject<ForestLilyFeature> FOREST_LILY=FEATURES.register("forest_lily",ForestLilyFeature::new);
    public static final RegistryObject<ForestReedsFeature> FOREST_REEDS=FEATURES.register("forest_reeds",ForestReedsFeature::new);
    public static final RegistryObject<ForestPumpkinFeature> FOREST_PUMPKIN=FEATURES.register("forest_pumpkin",ForestPumpkinFeature::new);
    public static final RegistryObject<ForestSmallMushroomsFeature> FOREST_SMALL_MUSHROOMS=FEATURES.register("forest_small_mushrooms",ForestSmallMushroomsFeature::new);
    public static final RegistryObject<MagicalTreeFeature> FOREST_TREE=FEATURES.register("forest_tree",()->new MagicalTreeFeature(MagicalTreeFeature.Kind.FOREST_TREE));
    public static final RegistryObject<MagicalTreeFeature> FOREST_OAK=FEATURES.register("forest_oak",()->new MagicalTreeFeature(MagicalTreeFeature.Kind.FOREST_OAK));
    public static final RegistryObject<MagicalTreeFeature> GREATWOOD=FEATURES.register("greatwood_plain",()->new MagicalTreeFeature(MagicalTreeFeature.Kind.GREATWOOD));
    public static final RegistryObject<MagicalTreeFeature> NATURAL_GREATWOOD=FEATURES.register("greatwood",()->new MagicalTreeFeature(MagicalTreeFeature.Kind.NATURAL_GREATWOOD));
    public static final RegistryObject<MagicalTreeFeature> SPIDER_GREATWOOD=FEATURES.register("greatwood_spider",()->new MagicalTreeFeature(MagicalTreeFeature.Kind.SPIDER_GREATWOOD));
    public static final RegistryObject<MagicalTreeFeature> SILVERWOOD=FEATURES.register("silverwood",()->new MagicalTreeFeature(MagicalTreeFeature.Kind.SILVERWOOD));
    public static final RegistryObject<MagicalTreeFeature> FOREST_SILVERWOOD=FEATURES.register("forest_silverwood",()->new MagicalTreeFeature(MagicalTreeFeature.Kind.FOREST_SILVERWOOD));
    public static final RegistryObject<EldritchRingFeature> ELDRITCH_RING=FEATURES.register("eldritch_ring",EldritchRingFeature::new);
    public static final RegistryObject<ObsidianTotemFeature> OBSIDIAN_TOTEM=FEATURES.register("obsidian_totem",ObsidianTotemFeature::new);
    public static final RegistryObject<NaturalAuraFeature> NATURAL_AURA=FEATURES.register("natural_aura",NaturalAuraFeature::new);
    public static final RegistryObject<TaintDecorationFeature> TAINT_DECORATION=FEATURES.register("taint_decoration",TaintDecorationFeature::new);
    private TCWorldFeatures() {}
    public static void register(IEventBus bus) { FEATURES.register(bus); }
}
