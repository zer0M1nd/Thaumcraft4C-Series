package ttmodern;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;

@Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class PlatformRenderer implements BlockEntityRenderer<PlatformEntity> {
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) { event.registerBlockEntityRenderer(Tinkerer.PLATFORM_ENTITY.get(),context->new PlatformRenderer()); }
    @Override public void render(PlatformEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var cover=entity.cover();if(cover==null||cover.isAir()||cover.getBlock() instanceof PlatformBlock||cover.hasBlockEntity())return;
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(cover,pose,buffers,light,overlay);
    }
}
