package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tc4revival.content.GolemOrbEntity;

public  class GolemOrbRenderer extends EntityRenderer<GolemOrbEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    public GolemOrbRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(GolemOrbEntity entity) { return TEXTURE; }
    @Override public void render(GolemOrbEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        pose.pushPose(); pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float u=(1+entity.tickCount%6)/8f,v=entity.red() ? .75f : .875f,scale=1.2f+Mth.sin(entity.tickCount/5f)*.2f;
        ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,TCRenderTypes.softParticles(TEXTURE)),pose.last().pose(),.5f*scale,u,v,u+.125f,v+.125f,1,1,1,.8f,220);
        pose.popPose(); super.render(entity,yaw,partial,pose,buffers,light);
    }
}
