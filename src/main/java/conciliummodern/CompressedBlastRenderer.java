package conciliummodern;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.client.TCRenderTypes;

/** Original fourth-frame atlas animation, in addition to the projectile's existing trail. */
public final class CompressedBlastRenderer extends EntityRenderer<CompressedBlast> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/misc/particles2.png");
    public CompressedBlastRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public ResourceLocation getTextureLocation(CompressedBlast entity){return TEXTURE;}
    @Override public void render(CompressedBlast entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.mulPose(entityRenderDispatcher.cameraOrientation());pose.scale(.3f,.3f,.3f);
        float u=(entity.tickCount%4)/16f,v=u+.0625f;var out=buffers.getBuffer(TCRenderTypes.softParticles(TEXTURE));var matrix=pose.last().pose();
        out.vertex(matrix,-.5f,-.5f,0).color(1f,1f,1f,.8f).uv(u,.875f).uv2(550).endVertex();
        out.vertex(matrix,.5f,-.5f,0).color(1f,1f,1f,.8f).uv(v,.875f).uv2(550).endVertex();
        out.vertex(matrix,.5f,.5f,0).color(1f,1f,1f,.8f).uv(v,.8125f).uv2(550).endVertex();
        out.vertex(matrix,-.5f,.5f,0).color(1f,1f,1f,.8f).uv(u,.8125f).uv2(550).endVertex();
        pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
    }
}
