package conciliummodern;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,value=Dist.CLIENT)
public final class CrimsonControlClient {
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        while(CrimsonClient.ROBE.consumeClick())if(mc.screen==null&&mc.player!=null&&PontifexRobe.fullSet(mc.player))PontifexRobe.sendToggle();
    }
    @SubscribeEvent public static void input(MovementInputUpdateEvent event){
        var player=event.getEntity();if(!player.hasEffect(CrimsonControl.ENSLAVED.get()))return;
        var input=event.getInput();input.forwardImpulse=1;input.leftImpulse=player.getRandom().nextFloat()-.5f;input.jumping=input.shiftKeyDown=false;
        if(player.tickCount%60==0)player.setYRot(360*(player.getRandom().nextFloat()-.5f));player.setXRot(net.minecraft.util.Mth.cos(player.tickCount));
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void hand(RenderHandEvent event){
        var mc=Minecraft.getInstance();var player=mc.player;if(player==null||!CrimsonControl.chained(player))return;event.setCanceled(true);
        if(event.getHand()!=InteractionHand.MAIN_HAND||player.isInvisible())return;
        var pose=event.getPoseStack();var buffers=event.getMultiBufferSource();var effect=player.getEffect(CrimsonControl.CHAINED.get());int time=effect==null?0:effect.getDuration();
        if(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer){
            pose.pushPose();pose.translate(.53,-.8,-.8);pose.mulPose(Axis.XP.rotationDegrees(-20));pose.mulPose(Axis.YP.rotationDegrees(180));pose.mulPose(Axis.ZP.rotationDegrees(-5));pose.translate(.5,0,.1);renderer.renderRightHand(pose,buffers,event.getPackedLight(),player);pose.popPose();
            pose.pushPose();pose.translate(-.05,-.8,-.8);pose.mulPose(Axis.ZP.rotationDegrees(-5));pose.mulPose(Axis.XP.rotationDegrees(-30));renderer.renderLeftHand(pose,buffers,event.getPackedLight(),player);pose.popPose();
        }
        pose.pushPose();pose.translate(0,0,-1.2);pose.mulPose(Axis.XP.rotationDegrees(-30));float expansion=time<10?(10-time)/30f:0;pose.scale(1.05f+expansion,1.05f,1.05f+expansion);
        CrimsonObjRenderer.ethereal("shackles",pose,buffers,Math.max(0,100-time)/150f);CrimsonObjRenderer.ethereal("chain",pose,buffers,Math.max(0,100-time)/150f);pose.popPose();
    }
}
