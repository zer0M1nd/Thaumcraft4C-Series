package automagy.client;

import automagy.content.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

public final class RemoteComparatorRenderer implements BlockEntityRenderer<RemoteComparatorBlockEntity> {
    public RemoteComparatorRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(RemoteComparatorBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(90-tile.getBlockState().getValue(RemoteComparatorBlock.FACING).toYRot()));
        draw(pose,buffers,light,overlay,!tile.stored(0).isEmpty(),tile.visuallyPowered());pose.popPose();
        if (!tile.stored(0).isEmpty()) {
            float time=(tile.getLevel()==null?0:tile.getLevel().getGameTime())+partial;
            pose.pushPose();pose.translate(.5,.35+Math.sin(time*.05)*.04,.5);pose.scale(.35f,.35f,.35f);pose.mulPose(Axis.YP.rotationDegrees(time*2));
            Minecraft.getInstance().getItemRenderer().renderStatic(tile.stored(0),ItemDisplayContext.GROUND,light,overlay,pose,buffers,tile.getLevel(),0);pose.popPose();
        }
    }
    public static void draw(PoseStack pose,MultiBufferSource buffers,int light,int overlay,boolean reading,boolean powered) {
        pose.scale(.5f,.5f,.5f);
        var texture=new ResourceLocation("automagy","textures/models/comparator"+(reading?(powered?"signaling":"reading"):"")+".png");
        ExternalObj.COMPARATOR.drawAll(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),light,overlay);
    }
}
