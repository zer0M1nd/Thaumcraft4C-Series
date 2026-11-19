package conciliummodern;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,value=Dist.CLIENT)
public final class DrumHands {
    @SubscribeEvent(priority=EventPriority.HIGH) public static void render(RenderHandEvent event){
        var mc=Minecraft.getInstance();var player=mc.player;if(player==null)return;
        var hand=player.getMainHandItem().getItem() instanceof ThaumaturgeDrum?InteractionHand.MAIN_HAND:
                player.getMainHandItem().isEmpty() && player.getOffhandItem().getItem() instanceof ThaumaturgeDrum?InteractionHand.OFF_HAND:null;
        if(hand==null || player.isUsingItem() && player.getUsedItemHand()!=hand)return;
        event.setCanceled(true);if(event.getHand()!=hand)return;
        var pose=event.getPoseStack();var buffers=event.getMultiBufferSource();
        boolean left=(hand==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite())==net.minecraft.world.entity.HumanoidArm.LEFT;
        // The old custom renderer received ItemRenderer's transform and the Forge
        // EQUIPPED_BLOCK helper. RenderHandEvent runs before those item transforms.
        pose.pushPose();if(left)pose.scale(-1,1,1);
        pose.translate(.56,-.52-event.getEquipProgress()*.6,-.72);
        pose.mulPose(Axis.YP.rotationDegrees(45));pose.scale(.4f,.4f,.4f);pose.translate(-.5,-.5,-.5);
        pose.translate(.5,.75,-1);pose.mulPose(Axis.YP.rotationDegrees(-135));
        float armPitch=net.minecraft.util.Mth.lerp(event.getPartialTick(),player.xBobO,player.xBob);
        float armYaw=net.minecraft.util.Mth.lerp(event.getPartialTick(),player.yBobO,player.yBob);
        pose.mulPose(Axis.XP.rotationDegrees((player.getXRot()-armPitch)*.1f));
        pose.mulPose(Axis.YP.rotationDegrees((player.getYRot()-armYaw)*.1f));
        pose.translate(-.56,.52+event.getEquipProgress()*1.5,.72);
        pose.mulPose(Axis.YP.rotationDegrees(90));pose.translate(0,0,-.72);pose.mulPose(Axis.YP.rotationDegrees(90));
        if(!player.isInvisible() && mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer){
            pose.pushPose();pose.scale(5,5,5);
            pose.pushPose();pose.translate(0,-1,-1);pose.mulPose(Axis.ZP.rotationDegrees(-90));renderer.renderRightHand(pose,buffers,event.getPackedLight(),player);pose.popPose();
            pose.pushPose();
            if(player.isUsingItem()){
                pose.translate(-.4,0,.6);pose.mulPose(Axis.ZP.rotationDegrees(-90));
                float wave=(float)Math.sin(player.getTicksUsingItem()+event.getPartialTick())*60;
                pose.mulPose(Axis.ZP.rotationDegrees(wave));pose.mulPose(Axis.XP.rotationDegrees(wave));
            }else{
                pose.translate(0,-.6,1.1);pose.mulPose(Axis.XP.rotationDegrees(-45));
                pose.mulPose(Axis.ZP.rotationDegrees(-90));pose.mulPose(Axis.ZP.rotationDegrees(59));pose.mulPose(Axis.YP.rotationDegrees(-65));
            }
            renderer.renderRightHand(pose,buffers,event.getPackedLight(),player);pose.popPose();pose.popPose();
        }
        pose.translate(.5,-5.5,-.7);pose.scale(3,3,3);
        DrumRenderer.instance().draw(player.getItemInHand(hand),pose,buffers,event.getPackedLight(),OverlayTexture.NO_OVERLAY);pose.popPose();
    }
    private DrumHands(){}
}
