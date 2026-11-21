package thmodern.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RenderLivingEvent;
import tc4revival.content.GolemEntity;
import thmodern.Horizons;
import thmodern.golem.AnimatedGolems;

@Mod.EventBusSubscriber(modid=Horizons.ID,value=Dist.CLIENT)
public final class AnimatedGolemRendering {
    static BlockGolemRenderer renderer;
    @SubscribeEvent public static void render(RenderLivingEvent.Pre<?,?> event){
        if(renderer==null||event.getRenderer()==renderer||!(event.getEntity() instanceof GolemEntity golem)||!AnimatedGolems.isAnimated(golem))return;
        event.setCanceled(true);
        renderer.render(golem,golem.getYRot(),event.getPartialTick(),event.getPoseStack(),event.getMultiBufferSource(),event.getPackedLight());
    }
}
