package emtmodern.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="emt",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public class PortableNodeRendering {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(emtmodern.ElectroMagicTools.PORTABLE_NODE_BE.get(),tc4revival.client.AuraNodeRenderer::new);}
}
