package arcane_engineering.client;

import arcane_engineering.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;

public class DestabilizerRenderer implements BlockEntityRenderer<DestabilizerBlockEntity> {
    private final ModelPart model=DestabilizerModel.createBodyLayer().bakeRoot();
    public DestabilizerRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(DestabilizerBlockEntity block,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        draw(model,block.open,block.angle+partial*block.speed,pose,buffers,light,overlay);
    }
    public static void draw(ModelPart model,boolean open,float angle,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose(); pose.translate(.5,1.5,.5); pose.scale(1,-1,-1);
        model.getChild("root").getChild("arms").yRot=(float)Math.toRadians(angle);
        var texture=ArcaneEngineering.id("textures/models/destabilizer"+(open?"_open":"")+".png");
        model.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),light,overlay);
        pose.popPose();
    }
}
