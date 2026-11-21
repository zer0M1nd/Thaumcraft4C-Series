package thaumicomniverse.expansion;
public final class AncientForestRegistration {
    private static final net.minecraftforge.registries.DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES=net.minecraftforge.registries.DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.FEATURES,"thaumicomniverse");
    public static final net.minecraftforge.registries.RegistryObject<AncientForestFeature> ANCIENT_FOREST_FEATURE=FEATURES.register("ancient_forest",AncientForestFeature::new);
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus){FEATURES.register(bus);}
}
