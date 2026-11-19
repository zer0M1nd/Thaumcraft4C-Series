package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.ArcaneLampBlockEntity;

/** Original Bore nozzle geometry and facing transforms shared by all three lamps. */
public  class ArcaneLampRenderer implements BlockEntityRenderer<ArcaneLampBlockEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/bore.png");
    private final ModelPart nozzle;
    public ArcaneLampRenderer(BlockEntityRendererProvider.Context context) {
        var mesh=new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("nozzle",CubeListBuilder.create()
                .texOffs(106,42).addBox(2.5f,-2,-2,5,4,4)
                .texOffs(106,51).addBox(7,-2.5f,-2.5f,1,5,5),PartPose.offset(0,8,0));
        nozzle=LayerDefinition.create(mesh,128,64).bakeRoot();
    }
    @Override public void render(ArcaneLampBlockEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if (entity.getLevel()!=null && entity.getLevel().getBlockEntity(entity.getBlockPos().relative(entity.facing())) instanceof tc4revival.content.ArcaneBoreBaseBlockEntity) {
            pose.pushPose(); var facing=entity.facing(); pose.translate(facing.getStepX(),facing.getStepY(),facing.getStepZ());
            renderConnector(facing.getOpposite(),pose,buffers,light,overlay); pose.popPose();
        }
        renderConnector(entity.facing(),pose,buffers,light,overlay);
    }
    private void renderConnector(net.minecraft.core.Direction facing,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose(); pose.translate(.5,0,.5);
        switch (facing) {
            case DOWN->{ pose.translate(-.5,.5,0); pose.mulPose(Axis.ZP.rotationDegrees(-90)); }
            case UP->{ pose.translate(.5,.5,0); pose.mulPose(Axis.ZP.rotationDegrees(90)); }
            case NORTH->pose.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH->pose.mulPose(Axis.YP.rotationDegrees(270));
            case WEST->pose.mulPose(Axis.YP.rotationDegrees(180));
            case EAST->{}
        }
        nozzle.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),light,overlay);
        pose.popPose();
    }
}
