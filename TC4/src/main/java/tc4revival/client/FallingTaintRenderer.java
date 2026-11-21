package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.FallingTaintEntity;

public  class FallingTaintRenderer extends EntityRenderer<FallingTaintEntity> {
    private final net.minecraft.client.renderer.block.BlockRenderDispatcher blocks;
    public FallingTaintRenderer(EntityRendererProvider.Context context) { super(context); blocks=context.getBlockRenderDispatcher(); }
    @Override public void render(FallingTaintEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        pose.pushPose(); pose.translate(-.5,0,-.5);
        blocks.renderSingleBlock(entity.carriedState(),pose,buffers,light,OverlayTexture.NO_OVERLAY);
        pose.popPose(); super.render(entity,yaw,partial,pose,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(FallingTaintEntity entity) { return TextureAtlas.LOCATION_BLOCKS; }
}
