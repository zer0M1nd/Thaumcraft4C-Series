package tc4revival.client;

import com.mojang.math.Axis;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.ArchitectFocusItem;
import tc4revival.content.CastingWandItem;
import tc4revival.content.WandFocusItem;

@Mod.EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT)
public  class ArchitectOverlay {
    private static final ResourceLocation ARROWS = ResourceLocation.parse("thaumcraft:textures/misc/architect_arrows.png");
    private static net.minecraft.client.multiplayer.ClientLevel cachedLevel;
    private static BlockPos cachedOrigin;
    private static Direction cachedFace;
    private static net.minecraft.world.item.ItemStack cachedWand = net.minecraft.world.item.ItemStack.EMPTY;
    private static Set<BlockPos> selection = Set.of();
    private static long nextRefresh;
    private ArchitectOverlay() {}
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        cachedLevel = null; cachedOrigin = null; cachedFace = null; cachedWand = net.minecraft.world.item.ItemStack.EMPTY; selection = Set.of(); nextRefresh = 0;
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        var mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || mc.player == null || mc.level == null
                || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        var wand = mc.player.getMainHandItem().getItem() instanceof CastingWandItem || mc.player.getMainHandItem().getItem() instanceof tc4revival.content.ElementalShovelItem ? mc.player.getMainHandItem() : mc.player.getOffhandItem();
        boolean shovel=wand.getItem() instanceof tc4revival.content.ElementalShovelItem;
        ArchitectFocusItem focus=WandFocusItem.attached(wand).getItem() instanceof ArchitectFocusItem item ? item : null;
        if (shovel ? !mc.player.isShiftKeyDown() : focus==null || !focus.architect(wand)) return;
        long now = mc.level.getGameTime();
        if (cachedLevel != mc.level || !hit.getBlockPos().equals(cachedOrigin) || hit.getDirection() != cachedFace
                || !net.minecraft.world.item.ItemStack.matches(wand, cachedWand) || now >= nextRefresh) {
            cachedLevel = mc.level; cachedOrigin = hit.getBlockPos().immutable(); cachedFace = hit.getDirection(); cachedWand = wand.copy();
            selection = new HashSet<>(shovel ? tc4revival.content.ElementalShovelLayout.placement(mc.level,cachedOrigin,cachedFace,tc4revival.content.ElementalShovelLayout.orientation(wand),mc.player.getYRot()) : focus.blocks(wand, mc.level, cachedOrigin, cachedFace, mc.player)); nextRefresh = now + 5;
        }
        if (selection.isEmpty()) return;
        var camera = event.getCamera().getPosition(); var pose = event.getPoseStack(); var buffers = mc.renderBuffers().bufferSource();
        float ticks = mc.player.tickCount;
        for (var pos : selection) {
            var offset = Vec3.atLowerCornerOf(pos).subtract(camera); pose.pushPose(); pose.translate(offset.x, offset.y, offset.z);
            ConnectedFaces.render(pos, selection::contains, selection::contains, pose, buffers,
                    (float) Math.sin(ticks / 2 + pos.getX()) * .2f + .3f, (float) Math.sin(ticks / 3 + pos.getY()) * .2f + .3f,
                    (float) Math.sin(ticks / 4 + pos.getZ()) * .2f + .8f, .2f, true); pose.popPose();
        }
        if (shovel) { buffers.endBatch(); return; }
        var origin = Vec3.atCenterOf(hit.getBlockPos()).subtract(camera);
        pose.pushPose(); pose.translate(origin.x, origin.y, origin.z); pose.mulPose(Axis.XP.rotationDegrees(90));
        var arrowType = TCRenderTypes.revealedNode(ARROWS, false); var out = TCRenderTypes.buffer(buffers,arrowType);
        for (var axis : Direction.Axis.values()) {
            if (!focus.showAxis(wand, hit.getDirection(), axis)) continue;
            pose.pushPose();
            if (axis == Direction.Axis.Z) pose.mulPose(Axis.ZP.rotationDegrees(90));
            if (axis == Direction.Axis.Y) pose.mulPose(Axis.YP.rotationDegrees(90));
            for (int plane = 0; plane < 2; plane++) {
                if (plane == 1) pose.mulPose(Axis.XP.rotationDegrees(90));
                ClientFocusEffects.quad(out, pose.last().pose(), .5f, 0, 0, 1, 1,
                        (float) Math.sin(ticks / 4 + hit.getBlockPos().getX()) * .2f + .3f,
                        (float) Math.sin(ticks / 3 + hit.getBlockPos().getY()) * .2f + .3f,
                        (float) Math.sin(ticks / 2 + hit.getBlockPos().getZ()) * .2f + .8f, 1, 200);
            }
            pose.popPose();
        }
        pose.popPose(); buffers.endBatch();
    }
}
