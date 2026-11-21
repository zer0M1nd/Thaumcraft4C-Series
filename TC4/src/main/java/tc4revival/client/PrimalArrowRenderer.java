package tc4revival.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tc4revival.content.*;

public  class PrimalArrowRenderer extends EntityRenderer<PrimalArrowEntity> {
    private static final ResourceLocation ARROW=ResourceLocation.parse("minecraft:textures/entity/projectiles/arrow.png"),GLOW=ResourceLocation.parse("thaumcraft:textures/misc/wisp.png");
    public PrimalArrowRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(PrimalArrowEntity entity) { return ARROW; }
    @Override public void render(PrimalArrowEntity arrow,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        float alpha=1-net.minecraft.util.Mth.clamp((arrow.groundTicks()-1100)/100f,0,1);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial,arrow.yRotO,arrow.getYRot())-90));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial,arrow.xRotO,arrow.getXRot())));
        float shake=arrow.shakeTime-partial; if (shake>0) pose.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(shake*3)*shake));
        pose.mulPose(Axis.XP.rotationDegrees(45)); pose.scale(.05625f,.05625f,.05625f); pose.translate(-4,0,0);
        var vertices=TCRenderTypes.buffer(buffers,TCRenderTypes.lens(ARROW));
        for (int side=0;side<2;side++) for (int corner=0;corner<4;corner++) {
            int index=side==0?corner:3-corner; float y=index<2?-2:2,z=index==0||index==3?-2:2;
            vertex(vertices,pose,-7,y,z,corner==0||corner==3?0:.15625f,corner<2?5/32f:10/32f,alpha,light);
        }
        for (int fin=0;fin<4;fin++) {
            pose.mulPose(Axis.XP.rotationDegrees(90));
            for (int corner=0;corner<4;corner++) vertex(vertices,pose,corner==0||corner==3?-8:8,corner<2?-2:2,0,corner==0||corner==3?0:.5f,corner<2?0:5/32f,alpha,light);
        }
        pose.popPose(); pose.pushPose(); pose.mulPose(entityRenderDispatcher.cameraOrientation());
        int rgb=ShardType.values()[arrow.element()].tint(),frame=arrow.tickCount%16;
        float u=frame%4/4f,v=frame/4/4f;
        ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,arrow.element()==5?TCRenderTypes.softParticle(GLOW):TCRenderTypes.runes(GLOW)),pose.last().pose(),.5f,u,v,u+.249375f,v+.249375f,(rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f,alpha,240);
        pose.popPose(); super.render(arrow,yaw,partial,pose,buffers,light);
    }
    private static void vertex(VertexConsumer out,PoseStack pose,float x,float y,float z,float u,float v,float alpha,int light) {
        out.vertex(pose.last().pose(),x,y,z).color(1,1,1,alpha).uv(u,v).uv2(light).endVertex();
    }
}
