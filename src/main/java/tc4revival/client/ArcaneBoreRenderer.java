package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.*;

public  class ArcaneBoreRenderer implements BlockEntityRenderer<ArcaneBoreBlockEntity> {
    private final ModelPart carriage=ArcaneBoreModels.carriage(),barrel=ArcaneBoreModels.barrel(),emitter=ArcaneBoreModels.emitter(),jar;
    public ArcaneBoreRenderer(BlockEntityRendererProvider.Context context) {
        var mesh=new MeshDefinition(); mesh.getRoot().addOrReplaceChild("core",CubeListBuilder.create().texOffs(0,0).addBox(-5,-12,-5,10,12,10),PartPose.ZERO);
        jar=LayerDefinition.create(mesh,64,32).bakeRoot();
    }
    @Override public void render(ArcaneBoreBlockEntity bore,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var motion=bore.visual; var out=TCRenderTypes.buffer(buffers,RenderType.entityCutoutNoCull(ArcaneBoreBaseRenderer.TEXTURE));
        pose.pushPose(); pose.translate(.5,.5,.5); pose.mulPose(Axis.YP.rotationDegrees(motion.rotation.x()-motion.aimX+partial*motion.rotation.speedX()));
        pose.pushPose(); if (bore.getBlockState().getValue(ArcaneBoreBlock.MOUNT)==Direction.DOWN) pose.mulPose(Axis.ZP.rotationDegrees(180));
        pose.translate(0,-.5,0); carriage.render(pose,out,light,overlay); pose.popPose();
        pose.mulPose(Axis.ZP.rotationDegrees(motion.rotation.z()-motion.aimZ+partial*motion.rotation.speedZ()));
        pose.pushPose(); pose.mulPose(Axis.ZP.rotationDegrees(90)); pose.translate(0,-.5,0); barrel.render(pose,out,light,overlay); pose.popPose();
        pose.pushPose(); pose.mulPose(Axis.YP.rotationDegrees(motion.topRotation)); pose.translate(0,.5,0);
        emitter.getChild("focus").visible=bore.focus().getItem() instanceof ExcavationFocusItem; emitter.render(pose,out,light,overlay); pose.popPose();
        var camera=net.minecraft.client.Minecraft.getInstance().getCameraEntity(); float rotation=((camera==null?0:camera.tickCount)%45+partial)*8;
        for (int i=0;i<3;i++) {
            pose.pushPose(); pose.translate(0,-.17-.04*i,0); pose.mulPose(Axis.XP.rotationDegrees(-90));
            pose.mulPose(Axis.ZP.rotationDegrees(i==1?rotation:-rotation)); pose.mulPose(Axis.YP.rotationDegrees(i==2?-10:10));
            vortex(pose,buffers,.4f-.1f*i,i==0?1:.8f,overlay); pose.popPose();
        }
        pose.pushPose(); pose.mulPose(Axis.ZP.rotationDegrees(180)); pose.translate(0,.3,0); pose.scale(.6f,.6f,.6f);
        jar.render(pose,TCRenderTypes.buffer(buffers,TCRenderTypes.ghostModel(ResourceLocation.parse("thaumcraft:textures/models/jar.png"))),light,overlay);
        pose.popPose(); pose.popPose();
        ArcaneBoreBeam.render(bore,partial,pose,buffers);
    }
    private static void vortex(PoseStack pose,MultiBufferSource buffers,float size,float alpha,int overlay) {
        var out=TCRenderTypes.buffer(buffers,RenderType.entityTranslucent(ResourceLocation.parse("thaumcraft:textures/misc/vortex.png"))); var frame=pose.last();
        float half=size/2;
        for (float[] vertex:new float[][]{{-half,half,0,1},{half,half,1,1},{half,-half,1,0},{-half,-half,0,0}})
            out.vertex(frame.pose(),vertex[0],vertex[1],0).color(1f,1f,1f,alpha).uv(vertex[2],vertex[3]).overlayCoords(overlay).uv2(200).normal(frame.normal(),0,0,1).endVertex();
    }
    @Override public boolean shouldRenderOffScreen(ArcaneBoreBlockEntity bore) { return true; }
}
