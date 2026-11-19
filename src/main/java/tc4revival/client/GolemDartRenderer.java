package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tc4revival.content.GolemDartEntity;

/** Original arrow geometry, shortened and tinted as the TC4 dart. */
public  class GolemDartRenderer extends EntityRenderer<GolemDartEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("minecraft:textures/entity/projectiles/arrow.png");
    public GolemDartRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(GolemDartEntity entity) { return TEXTURE; }
    @Override public void render(GolemDartEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial,entity.yRotO,entity.getYRot())-90));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial,entity.xRotO,entity.getXRot())));
        float shake=entity.shakeTime-partial;
        if (shake>0) pose.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(shake*3)*shake));
        pose.mulPose(Axis.XP.rotationDegrees(45));
        pose.scale(.025625f*.75f,.025625f,.025625f); pose.translate(-4,0,0);
        var vertices=buffers.getBuffer(RenderType.entityCutout(TEXTURE));
        vertex(vertices,pose,-7,-2,-2,0,5/32f,1,0,0,light);
        vertex(vertices,pose,-7,-2,2,.15625f,5/32f,1,0,0,light);
        vertex(vertices,pose,-7,2,2,.15625f,10/32f,1,0,0,light);
        vertex(vertices,pose,-7,2,-2,0,10/32f,1,0,0,light);
        vertex(vertices,pose,-7,2,-2,0,5/32f,-1,0,0,light);
        vertex(vertices,pose,-7,2,2,.15625f,5/32f,-1,0,0,light);
        vertex(vertices,pose,-7,-2,2,.15625f,10/32f,-1,0,0,light);
        vertex(vertices,pose,-7,-2,-2,0,10/32f,-1,0,0,light);
        for (int fin=0;fin<4;fin++) {
            pose.mulPose(Axis.XP.rotationDegrees(90));
            vertex(vertices,pose,-8,-2,0,0,0,0,0,1,light);
            vertex(vertices,pose,8,-2,0,.5f,0,0,0,1,light);
            vertex(vertices,pose,8,2,0,.5f,5/32f,0,0,1,light);
            vertex(vertices,pose,-8,2,0,0,5/32f,0,0,1,light);
        }
        pose.popPose();
        super.render(entity,yaw,partial,pose,buffers,light);
    }
    private static void vertex(VertexConsumer vertices,PoseStack pose,float x,float y,float z,float u,float v,float nx,float ny,float nz,int light) {
        vertices.vertex(pose.last().pose(),x,y,z).color(.5f,.5f,.6f,1).uv(u,v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),nx,ny,nz).endVertex();
    }
}
