package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.ArcaneBoreBaseBlockEntity;

public  class ArcaneBoreBaseRenderer implements BlockEntityRenderer<ArcaneBoreBaseBlockEntity> {
    static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/bore.png");
    private final ModelPart base=ArcaneBoreModels.base(),connector=ArcaneBoreModels.connector();
    public ArcaneBoreBaseRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(ArcaneBoreBaseBlockEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose(); pose.translate(.5,0,.5);
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)); base.render(pose,out,light,overlay);
        rotateOutput(pose,entity.output()); connector.render(pose,out,light,overlay); pose.popPose();
    }
    static void rotateOutput(PoseStack pose,Direction side) {
        // Vertical wand-selected outputs retain the original default east-facing nozzle.
        float angle=switch(side) { case NORTH->90; case SOUTH->270; case WEST->180; default->0; };
        pose.mulPose(Axis.YP.rotationDegrees(angle));
    }
}
