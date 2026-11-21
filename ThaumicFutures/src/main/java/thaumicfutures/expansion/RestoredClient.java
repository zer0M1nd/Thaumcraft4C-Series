package thaumicfutures.expansion;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumicfutures",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class RestoredClient {
    @SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(RestoredContent.CRYSTAL_ENTITY.get(),PlantedCrystalRenderer::new);
        event.registerBlockEntityRenderer(RestoredContent.CREATIVE_TANK_ENTITY.get(),tc4revival.client.EssentiaJarRenderer::new);
        event.registerBlockEntityRenderer(RestoredInfusion.MATRIX_ENTITY.get(),context->{
            var renderer=new RestoredAltarRenderer.Matrix(context);
            return (tile,partial,pose,buffers,light,overlay)->renderer.render(tile,partial,pose,buffers,light,overlay);
        });
        event.registerBlockEntityRenderer(RestoredInfusion.PEDESTAL_ENTITY.get(),context->{
            var renderer=new tc4revival.client.ArcanePedestalRenderer(context);
            return (tile,partial,pose,buffers,light,overlay)->renderer.render(tile,partial,pose,buffers,light,overlay);
        });
        event.registerBlockEntityRenderer(RestoredInfusion.PILLAR_ENTITY.get(),RestoredAltarRenderer.Pillar::new);
        event.registerEntityRenderer(RestoredContent.PARADOX_ENTITY.get(),net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    }
    @SubscribeEvent public static void reload(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event){
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)resources->{PlantedCrystalRenderer.reload();RestoredAltarRenderer.reload();});
    }
    @SubscribeEvent public static void colors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event){
        RestoredContent.NITORS.forEach((color,block)->event.register((stack,tint)->color.getMapColor().col,block.get().asItem()));
    }
    private RestoredClient(){}
}
