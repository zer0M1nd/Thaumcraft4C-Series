package tc4revival.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.TC4Revival;
import tc4revival.content.TCSounds;
import tc4revival.network.ScanEffectPacket;

/** Retains the old private particle engine's per-dimension lifetime and 2000-entry layer limit. */
@Mod.EventBusSubscriber(modid = TC4Revival.MOD_ID, value = Dist.CLIENT)
public  class ClientScanEffects {
    private record Rune(double x, double z, RuneMotion motion, int rotation, int glyph, double offsetX, double offsetY, float scale, float red, float blue) {}
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private static final Map<ResourceLocation, List<Rune>> PARTICLES = new HashMap<>();
    private ClientScanEffects() {}
    public static void receive(ScanEffectPacket packet, Connection source) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getConnection() == null || mc.getConnection().getConnection() != source
                || !source.isConnected() || !mc.level.dimension().location().equals(packet.dimension())) return;
        var random = mc.level.random;
        var list = PARTICLES.computeIfAbsent(packet.dimension(), ignored -> new ArrayList<>());
        if (list.size() >= 2000) list.remove(0);
        float red = 0.3f + random.nextFloat() * 0.7f, blue = 0.3f + random.nextFloat() * 0.7f;
        list.add(new Rune(packet.origin().x + 0.5, packet.origin().z + 0.5, new RuneMotion(packet.duration(), packet.origin().y + 0.5),
                random.nextInt(4) * 90, random.nextInt(16), random.nextFloat() * 0.2, -0.3 + random.nextFloat() * 0.6,
                (float) (1 + random.nextGaussian() * 0.1f), red, blue));
        if (packet.tickSound()) mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), TCSounds.CAMERA_TICKS.get(),
                SoundSource.MASTER, 0.2f, 0.45f + random.nextFloat() * 0.1f, false);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { PARTICLES.clear(); }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        var mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.START || mc.level == null) return;
        var particles = PARTICLES.get(mc.level.dimension().location());
        if (particles == null) return;
        particles.forEach(particle -> particle.motion().tick());
        particles.removeIf(particle -> particle.motion().removed());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        var mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || mc.level == null) return;
        var particles = PARTICLES.get(mc.level.dimension().location());
        if (particles == null || particles.isEmpty()) return;
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        var type = TCRenderTypes.runes(TEXTURE);
        var vertices = TCRenderTypes.buffer(buffers,type);
        for (var rune : particles) {
            if (rune.motion().removed()) continue;
            pose.pushPose();
            pose.translate(rune.x() - camera.x, rune.motion().y(event.getPartialTick()) - camera.y, rune.z() - camera.z);
            pose.mulPose(Axis.YP.rotationDegrees(rune.rotation()));
            pose.mulPose(Axis.ZP.rotationDegrees(90));
            pose.translate(rune.offsetX(), rune.offsetY(), -0.51);
            float half = 0.15f * rune.scale(), u = rune.glyph() / 16f, v = 0.375f;
            vertex(vertices, pose.last().pose(), rune, -half, half, u + 0.0624375f, v + 0.0624375f);
            vertex(vertices, pose.last().pose(), rune, half, half, u + 0.0624375f, v);
            vertex(vertices, pose.last().pose(), rune, half, -half, u, v);
            vertex(vertices, pose.last().pose(), rune, -half, -half, u, v + 0.0624375f);
            pose.popPose();
        }
        TCRenderTypes.endBatch(buffers,type);
    }
    private static void vertex(VertexConsumer vertices, org.joml.Matrix4f matrix, Rune rune, float x, float y, float u, float v) {
        vertices.vertex(matrix, x, y, 0).color(rune.red(), 0, rune.blue(), rune.motion().alpha() / 2).uv(u, v).uv2(240).endVertex();
    }
}
