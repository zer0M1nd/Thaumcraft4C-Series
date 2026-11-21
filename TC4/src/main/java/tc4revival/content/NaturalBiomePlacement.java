package tc4revival.content;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import tc4revival.config.TCConfig;
import terrablender.api.Regions;

/** TerraBlender owns placement, biome discovery and feature sorting. */
public class NaturalBiomePlacement {
    private static final DeferredRegister<Codec<? extends BiomeSource>> SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, "thaumcraft");
    static {
        // Old saves decode to their original source. New saves never reintroduce the overlay.
        SOURCES.register("legacy_natural", () -> BiomeSource.CODEC.fieldOf("delegate").codec());
    }
    private static final ThaumcraftBiomeRegion FOREST = new ThaumcraftBiomeRegion(TCBiomes.MAGICAL_FOREST, 5);
    private static final ThaumcraftBiomeRegion TAINT = new ThaumcraftBiomeRegion(TCBiomes.TAINTED_LAND, 2);

    private NaturalBiomePlacement() {}

    public static void register(IEventBus bus) {
        SOURCES.register(bus);
        bus.addListener(NaturalBiomePlacement::setup);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Regions.register(FOREST);
            Regions.register(TAINT);
        });
    }

    public static void starting(ServerAboutToStartEvent event) {
        // SERVER configs are loaded now. TerraBlender builds its sampler at LOWEST
        // priority, after this listener. Snapshot until the next world start.
        FOREST.configureWeight(TCConfig.SERVER.magicalForestBiomeWeight.get());
        TAINT.configureWeight(TCConfig.SERVER.taintBiomeWeight.get());
        com.mojang.logging.LogUtils.getLogger().info(
                "TC4 TerraBlender forest-family regions: forestWeight={}, taintWeight={}",
                FOREST.getWeight(), TAINT.getWeight());
    }
}
