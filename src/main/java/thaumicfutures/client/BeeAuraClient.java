package thaumicfutures.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import thaumicfutures.compat.bees.BeeAuraContent;

@Mod.EventBusSubscriber(modid="thaumicfutures", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BeeAuraClient {
    private static boolean enabled() { return ModList.get().isLoaded("forestry") && ModList.get().isLoaded("mobees"); }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        if (enabled()) event.enqueueWork(() -> MenuScreens.register(BeeAuraContent.APIARY_MENU.get(), MagicApiaryScreen::new));
    }
    @SubscribeEvent public static void models(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {
        if (enabled()) event.register(VisAuraRenderer.CENTRE);
    }
    @SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        if (enabled()) event.registerBlockEntityRenderer(BeeAuraContent.PROVIDER_TILE.get(), VisAuraRenderer::new);
    }
}
