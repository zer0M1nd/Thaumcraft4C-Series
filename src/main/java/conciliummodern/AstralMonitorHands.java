package conciliummodern;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Original two-arm instrument view, expressed in the modern hand rendering frame. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,value=Dist.CLIENT)
public final class AstralMonitorHands {
    @SubscribeEvent(priority=EventPriority.HIGH) public static void render(RenderHandEvent event){
        var mc=Minecraft.getInstance();var player=mc.player;if(player==null)return;
        var hand=player.getMainHandItem().getItem() instanceof AstralMonitor?InteractionHand.MAIN_HAND:
                player.getMainHandItem().isEmpty() && player.getOffhandItem().getItem() instanceof AstralMonitor?InteractionHand.OFF_HAND:null;
        if(hand==null || (player.isUsingItem() && player.getUsedItemHand()!=hand))return;
        event.setCanceled(true);if(event.getHand()!=hand)return;
        var pose=event.getPoseStack();var buffers=event.getMultiBufferSource();float equip=event.getEquipProgress(),swing=event.getSwingProgress();
        float wave=(float)Math.sin(Math.sqrt(swing)*Math.PI);
        pose.pushPose();pose.translate(.56,-.52-equip*.6,-.72);
        pose.mulPose(Axis.YP.rotationDegrees(45-(float)Math.sin(swing*swing*Math.PI)*20));pose.mulPose(Axis.ZP.rotationDegrees(-80*wave));pose.mulPose(Axis.XP.rotationDegrees(-80*wave));pose.scale(.4f,.4f,.4f);
        pose.translate(-.5,-.5,-.5);pose.translate(1,.75,-1);pose.mulPose(Axis.YP.rotationDegrees(-135));
        pose.translate(-.56,.52+equip*1.5,.72);pose.mulPose(Axis.YP.rotationDegrees(90));pose.translate(0,0,-.72);pose.mulPose(Axis.YP.rotationDegrees(90));
        if(!player.isInvisible() && mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer){
            for(int side:new int[]{-1,1}){pose.pushPose();pose.scale(5,5,5);pose.translate(0,-.6,1.1*side);pose.mulPose(Axis.XP.rotationDegrees(-45*side));pose.mulPose(Axis.ZP.rotationDegrees(-31));pose.mulPose(Axis.YP.rotationDegrees(-65*side));
                if(side<0)renderer.renderRightHand(pose,buffers,event.getPackedLight(),player);else renderer.renderLeftHand(pose,buffers,event.getPackedLight(),player);pose.popPose();}
        }
        pose.mulPose(Axis.ZP.rotationDegrees(90));pose.translate(.4,-.4,0);pose.scale(2,2,2);
        AstralMonitorRenderer.instance().renderModel(player.getItemInHand(hand),true,pose,buffers);pose.popPose();
    }
    private AstralMonitorHands(){}
}
