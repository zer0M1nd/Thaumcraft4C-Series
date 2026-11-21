package thaumicfutures.devices.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumicfutures.devices.*;

@Mod.EventBusSubscriber(modid="thaumicfutures",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class DeviceClient {
    @SubscribeEvent public static void models(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {
        event.register(PatternCrafterRenderer.GEAR_MODEL);
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Devices.DIOPTRA_TILE.get(),DioptraRenderer::new);
        event.registerBlockEntityRenderer(Devices.CRAFTER_TILE.get(),PatternCrafterRenderer::new);
        event.registerEntityRenderer(Devices.CROSSBOW.get(),CrossbowRenderer::new);
    }
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) { event.registerLayerDefinition(CrossbowModel.LAYER,CrossbowModel::layer); }
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) { event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(Devices.CROSSBOW_MENU.get(),CrossbowScreen::new)); }
    public static void openDioptra(Dioptra tile) { Minecraft.getInstance().setScreen(new DioptraScreen(tile)); }
    private DeviceClient() {}
}
