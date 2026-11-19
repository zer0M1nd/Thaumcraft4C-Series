package conciliummodern;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,value=Dist.CLIENT)
public final class DrainageSyringeHands {
    @SubscribeEvent public static void render(RenderHandEvent event){
        if(!(event.getItemStack().getItem() instanceof DrainageSyringe))return;
        var mc=Minecraft.getInstance();var player=mc.player;if(player==null)return;event.setCanceled(true);
        boolean left=(event.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite())==HumanoidArm.LEFT;
        boolean using=player.isUsingItem()&&player.getUsedItemHand()==event.getHand(),open=DrainageSyringe.open(event.getItemStack());
        float ticks=using?player.getTicksUsingItem()+event.getPartialTick():0,wave=using?(float)Math.sin(ticks):0;
        var pose=event.getPoseStack();pose.pushPose();pose.translate(left?-.55:.55,-.5-event.getEquipProgress()*.6,-.8);
        if(left)pose.scale(-1,1,1);
        pose.mulPose(Axis.YP.rotationDegrees(using?Math.max(20,50-ticks*6):50));
        pose.mulPose(Axis.XP.rotationDegrees(using?Math.max(-20,-ticks*5):0));
        pose.mulPose((open?Axis.ZP:Axis.YP).rotationDegrees(wave));
        if(!player.isInvisible()&&mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer){
            pose.pushPose();pose.translate(.3,-.4,.45);pose.mulPose(Axis.XP.rotationDegrees(-70));pose.mulPose(Axis.ZP.rotationDegrees(-30));
            if(left)renderer.renderLeftHand(pose,event.getMultiBufferSource(),event.getPackedLight(),player);else renderer.renderRightHand(pose,event.getMultiBufferSource(),event.getPackedLight(),player);pose.popPose();
        }
        pose.scale(.65f,.65f,.65f);pose.mulPose(Axis.ZP.rotationDegrees(35));pose.translate(.4,.2,0);
        DrainageSyringeRenderer.instance().draw(event.getItemStack(),pose,event.getMultiBufferSource(),event.getPackedLight(),OverlayTexture.NO_OVERLAY);pose.popPose();
    }
}
