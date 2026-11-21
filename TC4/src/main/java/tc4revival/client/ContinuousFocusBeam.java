package tc4revival.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** Three rotating ribbons and the endpoint flare from the original sustained wand beam. */
@Mod.EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT)
public  class ContinuousFocusBeam {
    private static final ResourceLocation[] TEXTURES = {
        texture("beam"), texture("beam1"), texture("beam2"), texture("beam3")
    };
    private static final ResourceLocation PARTICLES = texture("particles");
    private static final Map<Integer, Beam> BEAMS = new HashMap<>();
    private static ClientLevel current;
    private static  class Beam {
        net.minecraft.world.entity.LivingEntity player; Vec3 end, previous; int type, color, age, maximum = 8, impact; float endScale; boolean reverse; int side=1;
    }
    private ContinuousFocusBeam() {}
    private static ResourceLocation texture(String name) { return ResourceLocation.parse("thaumcraft:textures/misc/" + name + ".png"); }
    public static void update(Player player, Vec3 end, int type, int color, float endScale, boolean reverse) {
        update((net.minecraft.world.entity.LivingEntity)player,end,type,color,endScale,reverse);
    }
    public static void update(net.minecraft.world.entity.LivingEntity player, Vec3 end, int type, int color, float endScale, boolean reverse) {
        var mc = Minecraft.getInstance(); if (mc.level == null || mc.level != player.level()) return;
        if (current != mc.level) { BEAMS.clear(); current = mc.level; }
        double limit = Minecraft.useFancyGraphics() ? 50 : 25;
        if (mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(player.position()) > limit * limit) return;
        var beam = BEAMS.get(player.getId());
        if (beam == null) { beam = new Beam(); beam.player = player; beam.previous = end; BEAMS.put(player.getId(), beam); }
        else { beam.maximum = Math.max(beam.maximum, beam.age + 4); beam.impact = endScale > 0 ? 5 : 0; }
        beam.end = end; beam.type = Mth.clamp(type, 0, 3); beam.color = color; beam.endScale = endScale; beam.reverse = reverse;
        beam.side=castingSide(player);
    }
    public static void release(Player player) {
        var beam = BEAMS.get(player.getId()); if (beam != null) beam.maximum = Math.min(beam.maximum, beam.age + 4);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { BEAMS.clear(); current = null; }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        var mc = Minecraft.getInstance(); if (event.phase != TickEvent.Phase.START || mc.isPaused()) return;
        if (current != mc.level) { BEAMS.clear(); current = mc.level; }
        for (var beam : BEAMS.values()) { beam.previous = beam.end; beam.age++; if (beam.impact > 0) beam.impact--; }
        BEAMS.values().removeIf(beam -> beam.age >= beam.maximum || beam.player.isRemoved());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        var mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || current == null || current != mc.level) return;
        var camera = event.getCamera().getPosition(); var pose = event.getPoseStack(); var buffers = mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick();
        for (var beam : BEAMS.values()) {
            var player = beam.player; float yaw = Mth.lerp(partial, player.yRotO, player.getYRot()) * Mth.DEG_TO_RAD;
            var start = player.getPosition(partial).add(-Mth.cos(yaw) * .066 * beam.side,
                    player == mc.getCameraEntity() ? player.getEyeHeight() - .06 : player.getBbHeight() / 2 + .19,
                    -Mth.sin(yaw) * .04 * beam.side).add(player.getViewVector(partial).scale(.3)).subtract(camera);
            var end = beam.previous.lerp(beam.end, partial).subtract(camera);
            var delta = end.subtract(start); if (delta.lengthSqr() < 1e-8) continue;
            var along = delta.normalize(); var first = along.cross(Math.abs(along.y) > .99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
            var second = along.cross(first).normalize();
            float growth = Math.min((beam.age + partial) / 4, 1);
            float alpha = .4f * Math.min(1, (beam.maximum - beam.age) / 4f);
            var tip = start.add(delta.scale(growth));
            float time = (mc.player == null ? player.tickCount : mc.player.tickCount) + partial;
            if (beam.reverse) time = -time;
            float scroll = -time * .2f - Mth.floor(-time * .1f);
            double rotation = Math.toRadians((current.getGameTime() % 72 + partial) * 5);
            var type = TCRenderTypes.runes(TEXTURES[beam.type]); var out = TCRenderTypes.buffer(buffers,type);
            for (int i = 0; i < 3; i++) {
                double angle = rotation + Math.PI / 3 * (i + 1);
                var width = first.scale(Math.cos(angle)).add(second.scale(Math.sin(angle))).scale(.15 * growth);
                var farWidth = width.scale(beam.endScale);
                float v = -1 + scroll + i / 3f, farV = v + (float) delta.length() * growth;
                vertex(out, pose.last().pose(), tip.subtract(farWidth), 1, farV, beam.color, alpha);
                vertex(out, pose.last().pose(), start.subtract(width), 1, v, beam.color, alpha);
                vertex(out, pose.last().pose(), start.add(width), 0, v, beam.color, alpha);
                vertex(out, pose.last().pose(), tip.add(farWidth), 0, farV, beam.color, alpha);
            }
            TCRenderTypes.endBatch(buffers,type);
            if (beam.impact > 0) {
                pose.pushPose(); pose.translate(end.x, end.y, end.z); pose.mulPose(event.getCamera().rotation());
                var flare = TCRenderTypes.runes(PARTICLES); float u = beam.age % 16 / 16f;
                ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,flare), pose.last().pose(), beam.endScale / 2 / (6 - beam.impact), u, .3125f, u + .0624375f, .3749375f,
                        (beam.color >> 16 & 255) / 255f, (beam.color >> 8 & 255) / 255f, (beam.color & 255) / 255f, .66f, 200);
                pose.popPose(); TCRenderTypes.endBatch(buffers,flare);
            }
        }
    }
    private static void vertex(VertexConsumer out, Matrix4f pose, Vec3 point, float u, float v, int color, float alpha) {
        out.vertex(pose, (float) point.x, (float) point.y, (float) point.z).color((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, alpha).uv(u, v).uv2(200).endVertex();
    }
    static int castingSide(net.minecraft.world.entity.LivingEntity player) {
        var arm=player.getUsedItemHand()==net.minecraft.world.InteractionHand.OFF_HAND?player.getMainArm().getOpposite():player.getMainArm();
        return arm==net.minecraft.world.entity.HumanoidArm.RIGHT?1:-1;
    }
}
