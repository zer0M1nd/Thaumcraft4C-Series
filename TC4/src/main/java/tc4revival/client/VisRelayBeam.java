package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import tc4revival.content.VisRelayBlockEntity;

/** Two crossed ribbons and a receiving flare using the retained power-beam textures. */
 class VisRelayBeam {
    private static final ResourceLocation BEAM = ResourceLocation.parse("thaumcraft:textures/misc/beam1.png");
    private static final ResourceLocation PARTICLES = ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private VisRelayBeam() {}
    static void render(VisRelayBlockEntity relay, float ticks, PoseStack pose, MultiBufferSource buffers) {
        Vec3 source = relay.displayedParentPoint(); if(source==null)return;
        var mc = Minecraft.getInstance(); int distance = Minecraft.useFancyGraphics() ? 50 : 25;
        if (mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(source) > distance * distance) return;
        Vec3 origin = Vec3.atLowerCornerOf(relay.getBlockPos()), start = source.subtract(origin), end = relay.beamPoint().subtract(origin);
        Vec3 delta = end.subtract(start); double length = delta.length(); if (length < 1e-6) return;
        Vec3 axis = delta.scale(1 / length);
        Vec3 across = axis.cross(Math.abs(axis.y) > .99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 second = axis.cross(across).normalize(); float scroll = -ticks * .2f - (float) Math.floor(-ticks * .1f);
        // Vanilla's lightmap shader discards alpha < .1, including this entire
        // faint beam (.03-.08). Use the existing TC4 1/255 alpha threshold.
        var out = TCRenderTypes.buffer(buffers,TCRenderTypes.node(BEAM, false));
        // Until the revealing equipment is equipped, the original network is faintly visible.
        boolean revealed = mc.getCameraEntity() instanceof net.minecraft.world.entity.LivingEntity viewer
                && tc4revival.content.RevealingGear.visible(viewer,true);
        float alpha = VisBeamVisibility.ribbon(relay.beamOpacity(),revealed);
        for (int i = 0; i < 2; i++) {
            Vec3 offset = (i == 0 ? across : second).scale(.105); float v = -1 + scroll + i / 3f;
            vertex(out, pose, end.subtract(offset), 1, v + (float) length, relay, alpha);
            vertex(out, pose, start.subtract(offset), 1, v, relay, alpha);
            vertex(out, pose, start.add(offset), 0, v, relay, alpha);
            vertex(out, pose, end.add(offset), 0, v + (float) length, relay, alpha);
        }
        pose.pushPose(); pose.translate(end.x, end.y, end.z); pose.mulPose(mc.gameRenderer.getMainCamera().rotation());
        float u = ((int) ticks % 16) / 16f;
        ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,TCRenderTypes.node(PARTICLES, false)), pose.last().pose(), .66f * relay.beamOpacity(),
                u, .3125f, u + .0624375f, .3749375f, relay.red(), relay.green(), relay.blue(), VisBeamVisibility.flare(relay.beamOpacity(),revealed), 200);
        pose.popPose();
    }
    private static void vertex(VertexConsumer out, PoseStack pose, Vec3 point, float u, float v, VisRelayBlockEntity relay, float alpha) {
        out.vertex(pose.last().pose(), (float) point.x, (float) point.y, (float) point.z).color(relay.red(), relay.green(), relay.blue(), alpha).uv(u, v).uv2(200).endVertex();
    }
}
