package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import tc4revival.content.*;

public  class EldritchLockRenderer implements BlockEntityRenderer<EldritchLockBlockEntity> {
    private final ModelPart cube;
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/eldritch_cube.png");
    public EldritchLockRenderer(BlockEntityRendererProvider.Context context) {
        var mesh=new MeshDefinition(); mesh.getRoot().addOrReplaceChild("cube",CubeListBuilder.create().texOffs(0,0).addBox(-8,-8,-8,16,16,16),PartPose.ZERO);
        cube=LayerDefinition.create(mesh,64,64).bakeRoot();
    }
    @Override public int getViewDistance() { return 96; }
    @Override public void render(EldritchLockBlockEntity lock,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        Direction side=lock.getBlockState().getValue(EldritchLockBlock.FACING); var pos=lock.getBlockPos();
        var viewer=Minecraft.getInstance().getCameraEntity(); float time=viewer==null ? 0 : viewer.tickCount+partial;
        pose.pushPose(); pose.translate(.5,.5,.5);
        for (int arm=0;arm<4;arm++) {
            pose.pushPose(); pose.mulPose(new Quaternionf().rotationAxis((float)(Math.PI*arm/2),side.getStepX(),0,side.getStepZ()));
            for (int piece=1;piece<5-(lock.count()+arm*5)/20;piece++) {
                pose.pushPose(); pose.translate(0,.25+.5*piece,0);
                float wobble=(float)Math.sin((time+piece*10+arm*20)/20)*.1f;
                if (piece==1 || piece==4) wobble=wobble/2+.2f;
                pose.scale(.5f+wobble,.5f,.5f+wobble); cube.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),light,overlay); pose.popPose();
            }
            pose.popPose();
        }
        pose.popPose();
        if (lock.count()>=0) {
            pose.pushPose(); pose.translate(.5+side.getStepX()*.525,.285,.5+side.getStepZ()*.525);
            pose.mulPose(Axis.YP.rotationDegrees(switch(side) { case EAST->90; case WEST->270; case NORTH->180; default->0; }));
            EldritchMonumentRenderer.framedItem(TCEldritch.RUNED_TABLET.get().getDefaultInstance(),pose,buffers,light,overlay); pose.popPose();
        }
        boolean near=viewer!=null && viewer.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<512;
        EldritchPlanes.draw(pos,side,.5f,-2,3,-2,3,near,partial,pose,buffers);
    }
}
