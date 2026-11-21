package conciliummodern;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.client.TCRenderTypes;

/** Original particle-atlas row, animated billboard and age-dependent size. */
public final class FirePowderRenderer<T extends ShardPowder> extends EntityRenderer<T> {
    private static final ResourceLocation TEXTURE=new ResourceLocation("thaumcraft","textures/misc/particles.png");
    public FirePowderRenderer(EntityRendererProvider.Context context) { super(context);shadowRadius=.1f;shadowStrength=.5f; }
    @Override public ResourceLocation getTextureLocation(T entity) { return TEXTURE; }
    @Override public void render(T orb,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        pose.pushPose();pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float scale=.1f+.3f*Math.max(0,(40-orb.age()-partial)/40);pose.scale(scale,scale,scale);
        int frame=(int)((orb.tickCount+partial)*2)%16;float u=frame/16f,v=(frame+1)/16f;
        int color=orb.powderType().color();
        float r=((color>>16)&255)/255f,g=((color>>8)&255)/255f,b=(color&255)/255f;
        var out=buffers.getBuffer(TCRenderTypes.runes(TEXTURE));var matrix=pose.last().pose();
        out.vertex(matrix,-.5f,-.25f,0).color(r,g,b,.5f).uv(u,.5625f).uv2(240).endVertex();
        out.vertex(matrix,.5f,-.25f,0).color(r,g,b,.5f).uv(v,.5625f).uv2(240).endVertex();
        out.vertex(matrix,.5f,.75f,0).color(r,g,b,.5f).uv(v,.5f).uv2(240).endVertex();
        out.vertex(matrix,-.5f,.75f,0).color(r,g,b,.5f).uv(u,.5f).uv2(240).endVertex();
        pose.popPose();super.render(orb,yaw,partial,pose,buffers,light);
    }
}
