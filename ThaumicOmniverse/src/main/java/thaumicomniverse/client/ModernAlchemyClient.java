package thaumicomniverse.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumicomniverse",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ModernAlchemyClient {
    @SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(thaumicomniverse.content.ModernAlchemy.CONVENIENT_TUBE_ENTITY.get(),ConvenientTubeRenderer::new);
    }
    private ModernAlchemyClient() {}
}
