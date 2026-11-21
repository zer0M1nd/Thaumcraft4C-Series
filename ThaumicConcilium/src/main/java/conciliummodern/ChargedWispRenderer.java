package conciliummodern;
import tc4revival.client.TCRenderTypes;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

import tc4revival.aspect.TCAspects;

public  class ChargedWispRenderer extends EntityRenderer<ChargedWisp> {
    private static final ResourceLocation SKIN=ResourceLocation.parse("thaumcraft:textures/misc/wisp.png"),PARTICLES=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    public ChargedWispRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @Override public ResourceLocation getTextureLocation(ChargedWisp entity){return SKIN;}
    @Override public void render(ChargedWisp wisp,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        int color=TCAspects.catalog().find(wisp.aspectTag()).map(a->a.color()).orElse(0),frame=wisp.tickCount%16;
        float red=(color>>16&255)/255f,green=(color>>8&255)/255f,blue=(color&255)/255f;
        pose.pushPose();pose.translate(0,.45,0);pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float u=frame%4/4f,v=frame/4/4f;
        quad(buffers.getBuffer(TCRenderTypes.softParticles(SKIN)),pose.last().pose(),1,u+.25f-.0000390625f,v,u,v+.25f-.0000390625f,red,green,blue,1,240);
        quad(buffers.getBuffer(TCRenderTypes.softParticles(PARTICLES)),pose.last().pose(),.4f+(float)Math.sin((wisp.tickCount+partial)/10)*.1f,(frame+1)/16f,5/16f,frame/16f,6/16f,1,1,1,1,240);
        pose.popPose();super.render(wisp,yaw,partial,pose,buffers,light);
    }
    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer out,org.joml.Matrix4f pose,float half,float u1,float v1,float u2,float v2,float red,float green,float blue,float alpha,int light) {
        out.vertex(pose,-half,-half,0).color(red,green,blue,alpha).uv(u2,v2).uv2(light).endVertex();
        out.vertex(pose,-half,half,0).color(red,green,blue,alpha).uv(u2,v1).uv2(light).endVertex();
        out.vertex(pose,half,half,0).color(red,green,blue,alpha).uv(u1,v1).uv2(light).endVertex();
        out.vertex(pose,half,-half,0).color(red,green,blue,alpha).uv(u1,v2).uv2(light).endVertex();
    }
}
