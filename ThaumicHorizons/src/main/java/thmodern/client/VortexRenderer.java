package thmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import tc4revival.client.TCRenderTypes;
import thmodern.plane.VortexEntity;

public class VortexRenderer implements BlockEntityRenderer<VortexEntity> {
    private static final ResourceLocation NODES = ResourceLocation.parse("thaumcraft:textures/misc/nodes.png");
    private static final ResourceLocation VORTEX = ResourceLocation.parse("thaumcraft:textures/misc/vortex.png");

    public VortexRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(VortexEntity vortex, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (vortex.getLevel() == null) return;
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        float alpha = (float) Math.max(0, 1 - camera.getPosition().distanceTo(vortex.getBlockPos().getCenter()) / 64);
        var aspects = vortex.aspects().aspects();
        if (alpha <= 0 || aspects.isEmpty()) return;
        VortexAudio.ensure(vortex);
        long nanos = System.nanoTime();
        int frame = (int) Math.floorMod(nanos / 40_000_000L + vortex.getBlockPos().getX(), 32);
        boolean contracted = vortex.beams() >= 6 && vortex.age() >= 50;
        float growth = vortex.collapsing() ? Math.max(0, 1 - vortex.age() / 25f) : Math.min(1, vortex.age() / 50f);
        float divisor = Math.max(1, aspects.size() / 2f);
        pose.pushPose();
        pose.translate(.5, .5, .5);
        pose.mulPose(camera.rotation());
        int index = 0;
        float angle = 0;
        for (var aspect : aspects) {
            angle = angle(nanos, index++);
            // Legacy renderFacingStrip uses scale as a radius, not a diameter.
            layer(buffers, pose, NODES, aspect.blendFactor() == 771, angle,
                    contracted ? 4f / 3 : 4, frame, 0, aspect.color(),
                    alpha * (aspect.blendFactor() == 771 ? 1.5f : 1) / divisor);
        }
        if (vortex.cheat() || contracted) {
            layer(buffers, pose, NODES, true, angle, 3, frame, 2, 0xffffff, alpha);
        } else {
            layer(buffers, pose, VORTEX, true, angle * 20 * growth / (1 + 2 * vortex.beams()),
                    .8f * growth, -1, 0, 0xffffff, .8f);
        }
        if (vortex.planeId() >= 0 && !vortex.hasEscrow()) {
            for (var aspect : aspects) {
                layer(buffers, pose, NODES, true, angle(nanos, index++), .5f, frame, 0,
                        aspect.color(), alpha * (aspect.blendFactor() == 771 ? 1.5f : 1) / divisor);
            }
        }
        pose.popPose();
    }

    private static float angle(long nanos, int index) {
        long period = (5000L + 500L * index) * 5_000_000L;
        return (float) (Math.floorMod(nanos, period) / (double) period * 360);
    }

    private static void layer(MultiBufferSource buffers, PoseStack pose, ResourceLocation texture,
                              boolean translucent, float angle, float radius, int frame, int strip, int rgb, float alpha) {
        // Preserve the legacy order of additive aspect layers, alpha core and plane overlay.
        if (buffers instanceof MultiBufferSource.BufferSource source) source.endBatch();
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        quad(buffers.getBuffer(TCRenderTypes.node(texture, translucent)), pose.last().pose(), radius,
                frame < 0 ? 0 : frame / 32f, frame < 0 ? 0 : strip / 32f,
                frame < 0 ? 1 : (frame + 1) / 32f, frame < 0 ? 1 : (strip + 1) / 32f,
                (rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f, alpha);
        if (buffers instanceof MultiBufferSource.BufferSource source) source.endBatch();
        pose.popPose();
    }

    private static void quad(VertexConsumer out, Matrix4f matrix, float radius, float u0, float v0,
                             float u1, float v1, float r, float g, float b, float alpha) {
        for (int i = 0; i < 4; i++) {
            out.vertex(matrix, i == 0 || i == 3 ? -radius : radius, i < 2 ? -radius : radius, 0)
                    .color(r, g, b, Math.min(1, alpha)).uv(i == 0 || i == 3 ? u0 : u1, i < 2 ? v1 : v0)
                    .uv2(220).endVertex();
        }
    }

    @Override public boolean shouldRenderOffScreen(VortexEntity vortex) { return true; }
}
