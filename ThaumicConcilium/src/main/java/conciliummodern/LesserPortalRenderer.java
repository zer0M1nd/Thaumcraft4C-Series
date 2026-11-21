package conciliummodern;
import tc4revival.client.TCRenderTypes;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;


public  class LesserPortalRenderer extends EntityRenderer<LesserPortal> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/misc/cultist_portal.png");
    public LesserPortalRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius=.1f; shadowStrength=.5f; }
    @Override public ResourceLocation getTextureLocation(LesserPortal entity) { return TEXTURE; }
    @Override public void render(LesserPortal portal,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        float height=1.5f; int growth=(int)Math.min(50,portal.tickCount+partial);
        if (portal.hurtTime>0) { double wave=Math.sin(portal.hurtTime*Math.PI/2.5); height-=wave/4; growth=(int)(growth+6*wave); }
        if (portal.pulse()>0) { double wave=Math.sin(portal.pulse()*Math.PI/5); height+=wave/4; growth=(int)(growth+12*wave); }
        float width=growth/50f*1.3f,loss=(1-portal.getHealth()/portal.getMaxHealth())/3;
        float pulse=(float)Math.sin(portal.tickCount/(5-12*loss))*loss+loss;
        float pulseWidth=(float)Math.sin(portal.tickCount/(6-15*loss))*loss+loss;
        height-=pulse/4; width-=pulseWidth/3; float alpha=1-pulse;
        int frame=15-(int)((System.nanoTime()/50000000L)%16); float u0=frame/16f,u1=u0+.0625f;
        var out=buffers.getBuffer(TCRenderTypes.worldTranslucent(TEXTURE)); var matrix=pose.last().pose();
        float[][] corners={{-1,-1,u1,0},{-1,1,u1,1},{1,1,u0,1},{1,-1,u0,0}};
        for (var corner:corners) {
            var point=new Vector3f(corner[0],corner[1],0).rotate(entityRenderDispatcher.cameraOrientation());
            out.vertex(matrix,point.x*width,portal.getBbHeight()/2+point.y*height,point.z*width).color(1,1,1,alpha).uv(corner[2],corner[3]).uv2(220).endVertex();
        }
    }
}
