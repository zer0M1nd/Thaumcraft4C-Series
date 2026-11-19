package arcanearteries;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ArcaneArteries.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ArteriesClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ArcaneArteries.CONTENT.values().forEach(ref -> {
            if (ref.get() instanceof SelfSacrificeKnife)
                ItemProperties.register(ref.get(), ArcaneArteries.id("orb"), (stack, level, entity, seed) -> ArteriesConfig.ORBS.get() ? 1 : 0);
        }));
    }
    @Mod.EventBusSubscriber(modid = ArcaneArteries.ID, value = Dist.CLIENT)
    public static final class Tooltips {
        @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) { ConfigSync.clear(); }
    }
}
