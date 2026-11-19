package thaumicfutures.autocaster.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumicfutures.autocaster.Autocasters;

@Mod.EventBusSubscriber(modid="thaumicfutures",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class AutocasterClient {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) { event.registerEntityRenderer(Autocasters.ENTITY.get(),AutocasterRenderer::new); }
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(Autocasters.MENU.get(),AutocasterScreen::new));
    }
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->AutocasterRenderer.reload());
    }
    private AutocasterClient() {}
}
