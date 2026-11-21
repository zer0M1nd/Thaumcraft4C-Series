package thmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import thmodern.containment.SoulEntity;

public final class SoulRenderer extends EntityRenderer<SoulEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumichorizons:textures/misc/soul.png");
    public SoulRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @Override public ResourceLocation getTextureLocation(SoulEntity soul){return TEXTURE;}
    @Override public void render(SoulEntity soul,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        if(!soul.isAlive())return;pose.pushPose();pose.mulPose(entityRenderDispatcher.cameraOrientation());
        int frame=(int)(soul.level().getGameTime()%16);var out=buffers.getBuffer(tc4revival.client.TCRenderTypes.eldritchEye(TEXTURE));
        for(int i=0;i<4;i++)out.vertex(pose.last().pose(),i==0||i==3?-.25f:.25f,i<2?-.25f:.25f,0).color(255,255,255,255).uv((frame+(i==0||i==3?0:1))/16f,i<2?1:0).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(pose.last().normal(),0,0,1).endVertex();
        pose.popPose();super.render(soul,yaw,partial,pose,buffers,light);
    }
}
