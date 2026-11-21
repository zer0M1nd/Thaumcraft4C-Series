package tc4revival.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT)
public  class WardEffects {
    private static final List<Flash> FLASHES = new ArrayList<>();
    private static ClientLevel current;
    private static  class Flash { Vec3 center; Direction face; int age, lifetime, rotation; float size, alpha = 1; }
    private WardEffects() {}
    public static void hit(Level level, BlockHitResult hit) {
        if (level != Minecraft.getInstance().level) return;
        if (current != level) { FLASHES.clear(); current = (ClientLevel) level; }
        var random = level.random; var local = hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos()));
        var face = hit.getDirection(); var flash = new Flash(); flash.face = face;
        flash.center = Vec3.atCenterOf(hit.getBlockPos()).add(face.getStepX() != 0 ? 0 : Mth.clamp(local.x - .6 + random.nextFloat() * .2, -.4, .4),
                face.getStepY() != 0 ? 0 : Mth.clamp(local.y - .6 + random.nextFloat() * .2, -.4, .4),
                face.getStepZ() != 0 ? 0 : Mth.clamp(local.z - .6 + random.nextFloat() * .2, -.4, .4));
        flash.lifetime = 12 + random.nextInt(5); flash.rotation = random.nextInt(360); flash.size = (float) (1.4 + random.nextGaussian() * .3);
        if (FLASHES.size() >= 256) FLASHES.remove(0); FLASHES.add(flash);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { FLASHES.clear(); current = null; }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || Minecraft.getInstance().isPaused()) return;
        if (current != Minecraft.getInstance().level) { FLASHES.clear(); current = Minecraft.getInstance().level; }
        FLASHES.removeIf(flash -> {
            // FXBlockWard computes alpha before incrementing age, then expires on the old age.
            float threshold = flash.lifetime / 5f;
            flash.alpha = flash.age <= threshold ? flash.age / threshold : (flash.lifetime - flash.age) / (float) flash.lifetime;
            return flash.age++ >= flash.lifetime;
        });
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || current == null || current != Minecraft.getInstance().level) return;
        var pose = event.getPoseStack(); var buffers = Minecraft.getInstance().renderBuffers().bufferSource(); var camera = event.getCamera().getPosition();
        for (var flash : FLASHES) {
            float age = flash.age + event.getPartialTick();
            float alpha = flash.alpha;
            var center = flash.center.subtract(camera);
            pose.pushPose(); pose.translate(center.x, center.y, center.z);
            pose.mulPoseMatrix(tc4revival.content.WardParticleGeometry.transform(flash.face, flash.rotation));
            var type = TCRenderTypes.runes(ResourceLocation.parse("thaumcraft:textures/models/hemis" + Math.min(15, (int) (15 * age / flash.lifetime)) + ".png"));
            ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,type), pose.last().pose(), flash.size * .5f, 1, 1, 0, 0, 1, 1, 1, alpha / 2, 240);
            pose.popPose(); TCRenderTypes.endBatch(buffers,type);
        }
    }
}
