package automagy.client;

import automagy.content.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;

/** Vanilla portal shader; no copied shader or bundled legacy bitmap. */
public final class DeliveryMirrorRenderer implements BlockEntityRenderer<DeliveryMirrorBlockEntity> {
    public DeliveryMirrorRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(DeliveryMirrorBlockEntity mirror,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(!mirror.getBlockState().getValue(DeliveryMirrorBlock.SOCKETED))return;
        pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(mirror.getBlockState().getValue(DeliveryMirrorBlock.FACING).getRotation());pose.translate(-.5,-.5,-.5);
        var matrix=pose.last().pose();var vertices=buffers.getBuffer(RenderType.endPortal());
        vertices.vertex(matrix,.1875f,.095f,.1875f).endVertex();
        vertices.vertex(matrix,.1875f,.095f,.8125f).endVertex();
        vertices.vertex(matrix,.8125f,.095f,.8125f).endVertex();
        vertices.vertex(matrix,.8125f,.095f,.1875f).endVertex();
        pose.popPose();
    }
}
