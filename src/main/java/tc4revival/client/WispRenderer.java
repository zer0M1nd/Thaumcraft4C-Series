package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.WispEntity;
import tc4revival.aspect.TCAspects;

public  class WispRenderer extends EntityRenderer<WispEntity> {
    private static final ResourceLocation SKIN=ResourceLocation.parse("thaumcraft:textures/misc/wisp.png"),PARTICLES=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    public WispRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @Override public ResourceLocation getTextureLocation(WispEntity entity){return SKIN;}
    @Override public void render(WispEntity wisp,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        if(wisp.getHealth()<=0)return;
        int color=TCAspects.catalog().find(wisp.aspectTag()).map(a->a.color()).orElse(0),frame=wisp.tickCount%16;
        float red=(color>>16&255)/255f,green=(color>>8&255)/255f,blue=(color&255)/255f;
        if(wisp.hurtTime>0){red=1;green=(color>>8&255)/300f;blue=(color&255)/300f;}
        pose.pushPose();pose.translate(0,.45,0);pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float u=frame%4/4f,v=frame/4/4f;
        ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,TCRenderTypes.softParticles(SKIN)),pose.last().pose(),1,u+.25f-.0000390625f,v,u,v+.25f-.0000390625f,red,green,blue,1,240);
        ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,TCRenderTypes.softParticles(PARTICLES)),pose.last().pose(),.4f+(float)Math.sin((wisp.tickCount+partial)/10)*.1f,(frame+1)/16f,5/16f,frame/16f,6/16f,1,1,1,1,240);
        pose.popPose();super.render(wisp,yaw,partial,pose,buffers,light);
    }
    public static void effects(WispEntity wisp){
        if(wisp.tickCount<=1)ClientFocusEffects.wispBirth(wisp.level(),wisp.position().add(0,.45,0));
        if(wisp.level().random.nextBoolean())TCAspects.catalog().find(wisp.aspectTag()).ifPresent(aspect->{
            var r=wisp.level().random;ClientFocusEffects.wispMote(wisp.level(),wisp.position().add((r.nextFloat()-r.nextFloat())*.7,.45+(r.nextFloat()-r.nextFloat())*.7,(r.nextFloat()-r.nextFloat())*.7),aspect.color());
        });
    }
}
