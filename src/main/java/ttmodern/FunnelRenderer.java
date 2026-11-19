package ttmodern;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;

@Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class FunnelRenderer implements BlockEntityRenderer<FunnelEntity> {
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(Tinkerer.FUNNEL_ENTITY.get(),context->new FunnelRenderer());}
    @Override public void render(FunnelEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var stack=entity.inventory.getStackInSlot(0);if(stack.isEmpty())return;
        pose.pushPose();pose.translate(.5,.625,.5);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.NONE,light,overlay,pose,buffers,entity.getLevel(),0);pose.popPose();
    }
}
