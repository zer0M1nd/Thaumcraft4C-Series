package wgmodern;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid="witchinggadgets",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public class MultipartClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent e){if(net.minecraftforge.fml.ModList.get().isLoaded("cb_multipart"))e.enqueueWork(Available::setup);}
    public static class Available {
        public static void setup(){var renderer=new tc4revival.client.EssentiaTubeRenderer(null);for(var type:MultipartCompat.PARTS.values())codechicken.multipart.api.MultipartClientRegistry.register(type.get(),new codechicken.multipart.api.part.render.PartRenderer<MultipartCompat.Tube>(){@Override public void renderDynamic(MultipartCompat.Tube part,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay,float partial){renderer.render(part.view(),partial,pose,buffers,light,overlay);}});}
    }
}
