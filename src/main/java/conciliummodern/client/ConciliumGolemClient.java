package conciliummodern.client;

import conciliummodern.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ConciliumGolemClient {
    @SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(ConciliumGolems.BYDLO.get(),GolemBydloRenderer::new);event.registerEntityRenderer(ConciliumGolems.VENGEFUL.get(),VengefulGolemRenderer::new);}
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(ConciliumGolems.MENU.get(),ConciliumGolemScreen::new));}
}
