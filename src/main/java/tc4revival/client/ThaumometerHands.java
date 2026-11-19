package tc4revival.client;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.TC4Revival;
import tc4revival.content.TCItems;

/** First-person scanner uses both arms and its own equip transform, as the old renderer did. */
@Mod.EventBusSubscriber(modid = TC4Revival.MOD_ID, value = Dist.CLIENT)
public  class ThaumometerHands {
    private static net.minecraft.client.player.LocalPlayer lastPlayer;
    private static int offhandHoldTicks;
    private ThaumometerHands() {}
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        var player=Minecraft.getInstance().player;
        if(player!=lastPlayer){lastPlayer=player;offhandHoldTicks=0;}
        if(player==null || !player.getOffhandItem().is(TCItems.THAUMOMETER.get())
                || player.getMainHandItem().is(TCItems.THAUMOMETER.get())) {offhandHoldTicks=0;return;}
        if(player.isUsingItem() && player.getUsedItemHand()==InteractionHand.OFF_HAND)offhandHoldTicks=10;
        else if(offhandHoldTicks>0)offhandHoldTicks--;
    }
    @SubscribeEvent public static void render(RenderHandEvent event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;
        InteractionHand hand = player.getMainHandItem().is(TCItems.THAUMOMETER.get()) ? InteractionHand.MAIN_HAND
                : player.getOffhandItem().is(TCItems.THAUMOMETER.get()) ? InteractionHand.OFF_HAND : null;
        if (hand == null) return;
        // Keep the raised view for half a second after release, including the scan-completion gap.
        // Starting to use the other hand overrides this visual grace period immediately.
        if (hand == InteractionHand.OFF_HAND && (player.isUsingItem() ? player.getUsedItemHand()!=hand
                : player!=lastPlayer || offhandHoldTicks==0)) return;
        event.setCanceled(true);
        if (event.getHand() != hand) return;
        var pose = event.getPoseStack();
        var buffers = event.getMultiBufferSource();
        pose.pushPose();
        // Reconstruct the old vanilla/Forge first-person frame before applying the scanner's own frame.
        float equip = event.getEquipProgress();
        float swing = event.getSwingProgress();
        float wave = (float) Math.sin(Math.sqrt(swing) * Math.PI);
        pose.translate(0.56, -0.52 - equip * 0.6, -0.72);
        pose.mulPose(Axis.YP.rotationDegrees(45 - (float) Math.sin(swing * swing * Math.PI) * 20));
        pose.mulPose(Axis.ZP.rotationDegrees(-80 * wave));
        pose.mulPose(Axis.XP.rotationDegrees(-80 * wave));
        pose.scale(0.4f, 0.4f, 0.4f);
        // Forge 1.7.10's EQUIPPED_BLOCK helper translated before invoking the item renderer.
        pose.translate(-0.5, -0.5, -0.5);
        pose.translate(1, 0.75, -1);
        pose.mulPose(Axis.YP.rotationDegrees(-135));
        pose.translate(-0.56, 0.52 + equip * 1.5, 0.72);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.translate(0, 0, -0.72);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        if (!player.isInvisible() && mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
            for (int side : new int[]{-1, 1}) {
                pose.pushPose();
                pose.scale(5, 5, 5);
                pose.translate(0, -0.6, 1.1 * side);
                pose.mulPose(Axis.XP.rotationDegrees(-45 * side));
                pose.mulPose(Axis.ZP.rotationDegrees(-31));
                pose.mulPose(Axis.YP.rotationDegrees(-65 * side));
                renderer.renderRightHand(pose, buffers, event.getPackedLight(), player);
                pose.popPose();
            }
        }
        pose.mulPose(Axis.ZP.rotationDegrees(90));
        pose.translate(0.4, -0.4, 0);
        pose.scale(2, 2, 2);
        ThaumometerRenderer.instance().renderModel(pose, buffers, event.getPackedLight(), net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, true);
        pose.popPose();
    }
}
