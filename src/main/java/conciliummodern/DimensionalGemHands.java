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

/** Palm presentation of the floating gem, mirrored for either hand and respecting invisible players. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,value=Dist.CLIENT)
public final class DimensionalGemHands {
    @SubscribeEvent public static void render(RenderHandEvent event){
        if(!(event.getItemStack().getItem() instanceof DimensionalGem))return;
        var mc=Minecraft.getInstance();var player=mc.player;if(player==null)return;event.setCanceled(true);
        boolean left=(event.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite())==HumanoidArm.LEFT;
        boolean using=player.isUsingItem()&&player.getUsedItemHand()==event.getHand();
        var pose=event.getPoseStack();pose.pushPose();pose.translate(left?-.55:.55,-.45-event.getEquipProgress()*.6,-.8);
        if(left)pose.scale(-1,1,1);
        float swing=net.minecraft.util.Mth.sin(net.minecraft.util.Mth.sqrt(event.getSwingProgress())*(float)Math.PI);
        pose.mulPose(Axis.YP.rotationDegrees(-15*swing));pose.mulPose(Axis.ZP.rotationDegrees(-10*swing));
        if(!player.isInvisible()&&mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer){
            pose.pushPose();pose.translate(.25,-.2,.25);pose.mulPose(Axis.XP.rotationDegrees(-75));pose.mulPose(Axis.ZP.rotationDegrees(-75));
            if(left)renderer.renderLeftHand(pose,event.getMultiBufferSource(),event.getPackedLight(),player);else renderer.renderRightHand(pose,event.getMultiBufferSource(),event.getPackedLight(),player);pose.popPose();
        }
        pose.translate(0,.15,-.1);pose.scale(.28f,.28f,.28f);
        DimensionalGemRenderer.instance().draw(event.getItemStack(),true,using,pose,event.getMultiBufferSource(),event.getPackedLight(),OverlayTexture.NO_OVERLAY);pose.popPose();
    }
}
