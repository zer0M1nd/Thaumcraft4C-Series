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
public final class ShardMillHands {
    @SubscribeEvent(priority=EventPriority.HIGH) public static void render(RenderHandEvent event){
        var mc=Minecraft.getInstance();var player=mc.player;var hand=ShardMillClient.hand();if(player==null || hand==null)return;
        if(!player.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND).isEmpty())return;
        if(player.isUsingItem() && player.getUsedItemHand()!=hand)return;
        event.setCanceled(true);if(event.getHand()!=hand)return;
        var stack=player.getItemInHand(hand);boolean open=ShardMill.open(stack),using=player.isUsingItem();
        float time=player.tickCount+event.getPartialTick(),load=using && open?Math.min(1,(player.getTicksUsingItem()+event.getPartialTick())/20f):-1;
        var pose=event.getPoseStack();var buffers=event.getMultiBufferSource();boolean left=(hand==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite())==net.minecraft.world.entity.HumanoidArm.LEFT;
        // Recreate the original ItemRenderer + EQUIPPED_BLOCK helper base before its OBJ transforms.
        pose.pushPose();if(left)pose.scale(-1,1,1);
        pose.translate(.56,-.52-event.getEquipProgress()*.6,-.72);pose.mulPose(Axis.YP.rotationDegrees(45));
        pose.scale(.4f,.4f,.4f);pose.translate(-.5,-.5,-.5);
        if(!player.isInvisible() && mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer){
            pose.pushPose();pose.translate(-.5,.6,.1);pose.mulPose(Axis.XP.rotationDegrees(-75));pose.mulPose(Axis.ZP.rotationDegrees(35));renderer.renderLeftHand(pose,buffers,event.getPackedLight(),player);pose.popPose();
            pose.pushPose();pose.translate(.5,open?1.1:.9,.2);pose.mulPose(Axis.XP.rotationDegrees(-75));pose.mulPose(Axis.ZP.rotationDegrees(-35));
            if(using)pose.mulPose(Axis.XP.rotationDegrees(open?-(1-load)*100:-(time*9)%360));
            renderer.renderRightHand(pose,buffers,event.getPackedLight(),player);pose.popPose();
        }
        if(open){pose.mulPose(Axis.YP.rotationDegrees(45));pose.mulPose(Axis.ZP.rotationDegrees(20));pose.translate(0,-1.5,0);}
        else {pose.mulPose(Axis.XP.rotationDegrees(90));pose.mulPose(Axis.ZP.rotationDegrees(45));pose.translate(1,-2.2,-.5);}
        ShardMillRenderer.instance().draw(stack,pose,buffers,event.getPackedLight(),OverlayTexture.NO_OVERLAY,open,using?time*9:0,load);pose.popPose();
    }
}
