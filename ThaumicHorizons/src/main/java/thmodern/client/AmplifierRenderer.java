package thmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import thmodern.node.*;

public final class AmplifierRenderer implements BlockEntityRenderer<TransductionAmplifierEntity> {
    private static tc4revival.client.GroupedObjMesh mesh;
    public AmplifierRenderer(BlockEntityRendererProvider.Context context){}
    public static void reload(){mesh=null;}
    @Override public void render(TransductionAmplifierEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(mesh==null)try(var reader=Minecraft.getInstance().getResourceManager().getResourceOrThrow(ResourceLocation.parse("thaumcraft:models/node_stabilizer.obj")).openAsReader()){
            mesh=tc4revival.client.GroupedObjMesh.read(reader);
        }catch(java.io.IOException failure){throw new IllegalStateException("Cannot load amplifier model",failure);}
        pose.pushPose();pose.translate(.5,.5,.5);
        var facing=entity.getBlockState().getValue(TransductionAmplifierBlock.FACING);
        pose.mulPose(Axis.YP.rotationDegrees(switch(facing){case SOUTH->180;case WEST->90;case EAST->270;default->0;}));pose.translate(0,0,-.5);
        draw("lock",false,pose,buffers,light,overlay);
        if(entity.active())draw("lock",true,pose,buffers,LightTexture.FULL_BRIGHT,overlay);
        for(int i=0;i<4;i++){pose.pushPose();pose.mulPose(Axis.ZP.rotationDegrees(i*90));pose.mulPose(Axis.YP.rotationDegrees(45));pose.translate(0,0,entity.extension()/137f);draw("piston",false,pose,buffers,light,overlay);if(entity.active())draw("piston",true,pose,buffers,LightTexture.FULL_BRIGHT,overlay);pose.popPose();}
        pose.popPose();
    }
    private static void draw(String part,boolean glow,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var vertices=mesh.groups().get(part);if(vertices==null)return;
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse("thaumcraft:textures/models/node_converter"+(glow?"_over":"")+".png")));
        for(var v:vertices)out.vertex(pose.last().pose(),v.x(),v.y(),v.z()).color(glow?204:255,glow?204:255,glow?0:255,255).uv(v.u(),v.v()).overlayCoords(overlay).uv2(light).normal(pose.last().normal(),v.nx(),v.ny(),v.nz()).endVertex();
    }
}
