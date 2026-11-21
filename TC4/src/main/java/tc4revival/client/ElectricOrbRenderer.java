package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.ShockOrbEntity;

public  class ElectricOrbRenderer extends EntityRenderer<ShockOrbEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    public ElectricOrbRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0; }
    @Override public ResourceLocation getTextureLocation(ShockOrbEntity entity) { return TEXTURE; }
    @Override public void render(ShockOrbEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose(); pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float size = 1.2f + (float) Math.sin(entity.tickCount / 5f) * .2f;
        pose.scale(size, size, size);
        float u = (1 + entity.tickCount % 6) / 8f;
        var consumer = TCRenderTypes.buffer(buffers,TCRenderTypes.runes(TEXTURE));
        ClientFocusEffects.quad(consumer, pose.last().pose(), .5f, u, .875f, u + .125f, 1, 1, 1, 1, .8f, 220);
        pose.popPose();
    }
}
