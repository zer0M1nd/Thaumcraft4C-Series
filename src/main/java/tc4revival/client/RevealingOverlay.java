package tc4revival.client;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.RevealedAspectContainer;
import tc4revival.content.RevealingGear;

@Mod.EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT)
public  class RevealingOverlay {
    private static float scale;
    private RevealingOverlay() {}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { scale = 0; }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        scale = Math.max(0, scale - .005f);
        var mc = Minecraft.getInstance(); if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        var helmet = mc.player.getItemBySlot(EquipmentSlot.HEAD);
        if (!RevealingGear.visible(mc.player,false)) return;
        var eye=event.getCamera().getPosition();
        var end=eye.add(mc.player.getViewVector(event.getPartialTick()).scale(mc.player.getAttributeValue(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get())));
        BlockHitResult hit=mc.hitResult instanceof BlockHitResult h&&h.getType()==HitResult.Type.BLOCK?h:null;
        if(hit!=null&&eye.distanceToSqr(hit.getLocation())<eye.distanceToSqr(end))end=hit.getLocation();
        var moving=thaumcraft.api.aspects.RevealingTargets.find(mc.player,eye,end,event.getPartialTick());
        if(moving==null&&hit==null)return;
        var pos=moving!=null?moving.local():hit.getBlockPos();
        var state=mc.level.getBlockState(pos); var pose=event.getPoseStack();
        var buffers = mc.renderBuffers().bufferSource(); var camera = event.getCamera().getPosition();
        float yaw = (float) Math.toDegrees(Math.atan2(camera.x - pos.getX() - .5, camera.z - pos.getZ() - .5)) + 180;
        int note=state.getBlock() instanceof NoteBlock ? state.getValue(NoteBlock.NOTE) : mc.level.getBlockEntity(pos) instanceof tc4revival.content.ArcaneEarBlockEntity ear ? ear.note() : -1;
        if (moving==null&&note>=0) {
            var text = "Note: " + note; var offset = Vec3.atCenterOf(pos.above()).subtract(camera);
            pose.pushPose(); pose.translate(offset.x, offset.y, offset.z); pose.mulPose(Axis.YP.rotationDegrees(yaw)); pose.mulPose(Axis.ZP.rotationDegrees(180));
            pose.scale(.02f, .02f, .02f);
            mc.font.drawInBatch(text, -mc.font.width(text) / 2f, 0, 0xffffff, true, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, 220);
            pose.popPose(); buffers.endBatch(); return;
        }
        var container=moving!=null?moving.container():mc.level.getBlockEntity(pos) instanceof RevealedAspectContainer c?c:null;
        if(container==null)return;
        var aspects = thaumcraft.api.client.ClientAspectViews.project(container.revealedAspects(),thaumcraft.api.aspects.AspectViews.Purpose.REVEALING); if (aspects.isEmpty()) return;
        if (scale < .3f) scale += .031f - scale / 10;
        boolean above=moving!=null?moving.above():mc.level.isEmptyBlock(pos.above());
        var face=above?Direction.UP:moving!=null?moving.face():hit.getDirection();
        var knowledge = ClientKnowledge.displaySnapshot(); var entries = aspects.aspects();
        for (int index = 0; index < entries.size(); index++) {
            var aspect = entries.get(index); int row = index / 5, column = index % 5, width = Math.min(5, entries.size() - row * 5);
            var center = Vec3.atCenterOf(pos).add(scale * 2 * face.getStepX(),
                    (above ? .4 : 0) + container.popupHeight() + row * scale * 1.05 + scale * 2 * face.getStepY(), scale * 2 * face.getStepZ());
            center=(moving!=null?moving.toWorld().apply(center):center).subtract(camera);
            pose.pushPose(); pose.translate(center.x, center.y, center.z); pose.mulPose(event.getCamera().rotation());
            pose.translate((column - width / 2f + .5) * scale * scale * 4, 0, 0); pose.scale(-scale, -scale, scale);
            boolean known = knowledge.map(value -> value.knows(aspect)).orElse(false); int color = thaumcraft.api.client.ClientAspectViews.view(aspect).color();
            var texture = known ? thaumcraft.api.client.ClientAspectViews.view(aspect).texture() : ResourceLocation.parse("thaumcraft:textures/aspects/_unknown.png");
            // This overlay already rotates Z by 180 degrees, unlike particle billboards.
            // Match old UtilsFX: local (-x,-y) has UV (0,0), (+x,+y) has (1,1).
            ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,TCRenderTypes.revealedAspect(texture)), pose.last().pose(), .5f, 1, 1, 0, 0,
                    (color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, .75f, 220);
            // Submit the icon before lettering and the shadow before foreground; depth sorting
            // must not reverse these layers when looking upward at the container.
            buffers.endBatch();
            if(aspects.amount(aspect)<=0) { pose.popPose(); continue; }
            String amount = Integer.toString(aspects.amount(aspect));
            pose.translate(.52,.5,.01);pose.scale(.04f,.04f,.04f);
            mc.font.drawInBatch(amount,1-mc.font.width(amount),1-mc.font.lineHeight,0xff111111,false,pose.last().pose(),buffers,Font.DisplayMode.SEE_THROUGH,0,220);
            buffers.endBatch();
            mc.font.drawInBatch(amount,-mc.font.width(amount),-mc.font.lineHeight,0xffffffff,false,pose.last().pose(),buffers,Font.DisplayMode.SEE_THROUGH,0,220);
            buffers.endBatch();
            pose.popPose();
        }
        buffers.endBatch();
    }
}
