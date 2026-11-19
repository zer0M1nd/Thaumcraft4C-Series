package thaumicfutures.compat.exdeorum;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import thedarkcolour.exdeorum.client.ter.BarrelRenderer;

final class ExDeorumClient {
    static void register(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(ExDeorumCompat.BARREL_ENTITY.get(), BarrelRenderer::new));
    }
}
