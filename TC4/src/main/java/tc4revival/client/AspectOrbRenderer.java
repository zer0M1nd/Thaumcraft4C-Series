package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.AspectOrbEntity;

public  class AspectOrbRenderer extends EntityRenderer<AspectOrbEntity> {
    private static final ResourceLocation PARTICLES = ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    public AspectOrbRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = .1f; shadowStrength = .5f; }
    @Override public ResourceLocation getTextureLocation(AspectOrbEntity entity) { return PARTICLES; }
    @Override public void render(AspectOrbEntity orb, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose(); pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float scale = .1f + .3f * (orb.maxAge() - orb.age()) / orb.maxAge(); pose.scale(scale, scale, scale);
        int frame = (int) (System.nanoTime() / 25000000L % 16);
        float u0 = frame / 16f, u1 = (frame + 1) / 16f; int color = orb.aspect().color();
        int bright = (light & 0xff0000) | Math.min(240, (light & 255) + 120);
        var out = TCRenderTypes.buffer(buffers,orb.aspect().blendFactor() == 771 ? TCRenderTypes.orb(PARTICLES) : TCRenderTypes.runes(PARTICLES));
        var matrix = pose.last().pose(); int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        out.vertex(matrix, -.5f, -.25f, 0).color(r, g, b, 128).uv(u0, .5625f).uv2(bright).endVertex();
        out.vertex(matrix, .5f, -.25f, 0).color(r, g, b, 128).uv(u1, .5625f).uv2(bright).endVertex();
        out.vertex(matrix, .5f, .75f, 0).color(r, g, b, 128).uv(u1, .5f).uv2(bright).endVertex();
        out.vertex(matrix, -.5f, .75f, 0).color(r, g, b, 128).uv(u0, .5f).uv2(bright).endVertex();
        pose.popPose(); super.render(orb, yaw, partial, pose, buffers, light);
    }
}
