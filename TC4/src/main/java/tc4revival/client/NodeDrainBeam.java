package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import tc4revival.config.TCConfig;
import tc4revival.content.AuraNodeBlockEntity;

 class NodeDrainBeam {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("thaumcraft:textures/misc/wispy.png");
    private record Point(Vec3 position, float alpha, float u) {}
    private NodeDrainBeam() {}
    static void render(AuraNodeBlockEntity node, float partial, PoseStack pose, MultiBufferSource buffers) {
        if (node.getLevel() == null || node.drainer() == null) return;
        var player = node.getLevel().getPlayerByUUID(node.drainer());
        if (player == null || !player.isUsingItem()) return;
        float used = player.getTicksUsingItem(), sway = (float) Math.sin(used / 10) * 10;
        var offset = new Vec3(-.1, -.1, .5).xRot(-Mth.lerp(partial, player.xRotO, player.getXRot()) * Mth.DEG_TO_RAD)
                .yRot(-Mth.rotLerp(partial, player.yRotO, player.getYRot()) * Mth.DEG_TO_RAD).yRot(-sway * .01f).xRot(-sway * .015f);
        Vec3 hand = player.getEyePosition(partial).add(offset), center = Vec3.atCenterOf(node.getBlockPos());
        between(Vec3.atLowerCornerOf(node.getBlockPos()), hand, center, node.drainColor(), Math.min(used, 10) / 10, pose, buffers);
    }
    static void between(Vec3 origin, Vec3 hand, Vec3 center, int color, float growth, PoseStack pose, MultiBufferSource buffers) {
        Vec3 delta = hand.subtract(center);
        float distance = (float) delta.length(), quality = TCConfig.CLIENT.golemLinkQuality() / 2f;
        float segments = Math.round(distance) * quality;
        if (segments <= 0 || distance < 1e-6) return;
        int last = (int) (segments * growth);
        var out = TCRenderTypes.buffer(buffers,TCRenderTypes.runes(TEXTURE));
        float time = System.nanoTime() / 30_000_000L;
        var translation = center.subtract(origin); pose.pushPose(); pose.translate(translation.x, translation.y, translation.z);
        for (int plane = 0; plane < 2; plane++) {
            Vec3 width = plane == 0 ? new Vec3(0, .15, 0) : new Vec3(.15, 0, 0);
            Point previous = point(0, segments, delta, hand, distance, quality, time);
            for (int i = 1; i <= last; i++) {
                Point next = point(i, segments, delta, hand, distance, quality, time);
                vertex(out, pose, previous, width.scale(-1), 1, color);
                vertex(out, pose, previous, width, 0, color);
                vertex(out, pose, next, width, 0, color);
                vertex(out, pose, next, width.scale(-1), 1, color);
                previous = next;
            }
        }
        pose.popPose();
    }
    private static Point point(int index, float segments, Vec3 delta, Vec3 hand, float distance, float quality, float time) {
        float progress = index / segments, alpha = 1 - Math.abs(index - segments / 2) / (segments / 2);
        double wave = distance * (1 - progress) * quality - time % 32767 / 5;
        Vec3 shifted = delta.add(Math.sin((hand.z % 16 + wave) / 4) * .5 * alpha,
                Math.sin((hand.x % 16 + wave) / 3) * .5 * alpha, Math.sin((hand.y % 16 + wave) / 2) * .5 * alpha).scale(progress);
        return new Point(shifted, alpha, (1 - progress) * distance + time * .02f);
    }
    private static void vertex(VertexConsumer out, PoseStack pose, Point point, Vec3 offset, float v, int color) {
        Vec3 position = point.position().add(offset);
        out.vertex(pose.last().pose(), (float) position.x, (float) position.y, (float) position.z)
                .color((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, point.alpha()).uv(point.u(), v).uv2(220).endVertex();
    }
}
