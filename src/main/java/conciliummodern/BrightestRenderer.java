package conciliummodern;
import tc4revival.client.TCRenderTypes;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

import tc4revival.aspect.TCAspects;

public  class BrightestRenderer extends EntityRenderer<BrightestOne> {
    private static final ResourceLocation SKIN=ResourceLocation.parse("thaumcraft:textures/misc/wisp.png"),PARTICLES=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    public BrightestRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @Override public ResourceLocation getTextureLocation(BrightestOne entity){return SKIN;}
    public static void effects(BrightestOne spirit){
        if(spirit.tickCount<=1)tc4revival.client.ClientFocusEffects.wispBirth(spirit.level(),spirit.position().add(0,.45,0));
        var random=spirit.level().random;if(random.nextBoolean())TCAspects.catalog().find(spirit.aspectTag()).ifPresent(aspect->
                tc4revival.client.ClientFocusEffects.wispMote(spirit.level(),spirit.position().add((random.nextFloat()-random.nextFloat())*.7,.45+(random.nextFloat()-random.nextFloat())*.7,(random.nextFloat()-random.nextFloat())*.7),aspect.color()));
    }
    @Override public void render(BrightestOne wisp,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        if(wisp.getHealth()<=0)return;
        int color=TCAspects.catalog().find(wisp.aspectTag()).map(a->a.color()).orElse(0),frame=wisp.tickCount%16;
        float red=(color>>16&255)/255f,green=(color>>8&255)/255f,blue=(color&255)/255f;
        pose.pushPose();pose.translate(0,1.5,0);pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float u=frame%4/4f,v=frame/4/4f;
        quad(buffers.getBuffer(TCRenderTypes.softParticles(SKIN)),pose.last().pose(),3.5f,u+.25f-.0000390625f,v,u,v+.25f-.0000390625f,red,green,blue,1,240);
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
