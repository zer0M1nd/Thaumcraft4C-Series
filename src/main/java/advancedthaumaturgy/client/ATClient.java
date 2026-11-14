package advancedthaumaturgy.client;
import advancedthaumaturgy.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.*;
@Mod.EventBusSubscriber(modid=AdvancedThaumaturgy.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public class ATClient {
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{net.minecraft.client.gui.screens.MenuScreens.register(AdvancedThaumaturgy.NODE_MENU.get(),NodeModifierScreen::new);net.minecraft.client.gui.screens.MenuScreens.register(AdvancedThaumaturgy.BENCH_MENU.get(),WandbenchScreen::new);});}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){
        e.registerBlockEntityRenderer(AdvancedThaumaturgy.MACHINE_ENTITY.get(),ATBlockRenderer::new);
        e.registerBlockEntityRenderer(AdvancedThaumaturgy.NODE_ENTITY.get(),ATBlockRenderer::new);
        e.registerBlockEntityRenderer(AdvancedThaumaturgy.BENCH_ENTITY.get(),ATBlockRenderer::new);
        e.registerBlockEntityRenderer(AdvancedThaumaturgy.CREATIVE_ENTITY.get(),ATBlockRenderer::new);
        e.registerBlockEntityRenderer(AdvancedThaumaturgy.JAR_ENTITY.get(),ATBlockRenderer.JarRenderer::new);
        e.registerEntityRenderer(AdvancedThaumaturgy.SENTRY_FIREBALL.get(),net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item e){for(var crystal:AdvancedThaumaturgy.CRYSTALS.values())e.register((s,tint)->crystal.get().color,crystal.get());}
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent e){e.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->OriginalObj.clear());}
}
