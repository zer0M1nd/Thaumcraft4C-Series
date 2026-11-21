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


/** The legacy casting bracelet is held around a visible wrist, rather than held like a wand. */
@Mod.EventBusSubscriber(modid="thaumicconcilium", value=Dist.CLIENT)
public class CastingBraceletHands {
    @SubscribeEvent public static void render(RenderHandEvent event) {
        var player=Minecraft.getInstance().player;
        if(player==null || !(event.getItemStack().getItem() instanceof CastingBracelet))return;
        event.setCanceled(true);
        if(player.isInvisible())return;
        var pose=event.getPoseStack();var buffers=event.getMultiBufferSource();
        var side=event.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();
        boolean left=side==HumanoidArm.LEFT;
        boolean casting=player.isUsingItem() && player.getUsedItemHand()==event.getHand();
        float swing=event.getSwingProgress(), equip=event.getEquipProgress();
        float wave=(float)Math.sin(Math.sqrt(swing)*Math.PI);
        pose.pushPose();
        if(left)pose.scale(-1,1,1);
        pose.translate(-.4*wave,.2*Math.sin(Math.sqrt(swing)*Math.PI*2),-.2*Math.sin(swing*Math.PI));
        pose.translate(.56,-.52-equip*.6,-.72);
        pose.mulPose(Axis.YP.rotationDegrees(45-(float)Math.sin(swing*swing*Math.PI)*20));
        pose.mulPose(Axis.ZP.rotationDegrees(-20*wave));pose.mulPose(Axis.XP.rotationDegrees(-80*wave));
        // Rebuild the old Forge EQUIPPED_BLOCK frame, shared by the arm and bracelet.
        pose.scale(.4F,.4F,.4F);pose.translate(-.5,-.5,-.5);
        if(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
            pose.pushPose();pose.translate(.5,1,1);
            pose.mulPose(Axis.ZP.rotationDegrees(90));pose.mulPose(Axis.XP.rotationDegrees(220));
            if(casting){pose.translate(-.5,0,0);pose.mulPose(Axis.ZP.rotationDegrees(-45));}
            pose.scale(3,3,3);
            if(left){pose.scale(-1,1,1);renderer.renderLeftHand(pose,buffers,event.getPackedLight(),player);}
            else renderer.renderRightHand(pose,buffers,event.getPackedLight(),player);
            pose.popPose();
        }
        pose.mulPose(Axis.YP.rotationDegrees(-45));pose.translate(1.2,.1,-1);pose.scale(.6F,.6F,.6F);
        if(casting){pose.translate(0,1,0);pose.mulPose(Axis.XP.rotationDegrees(35));pose.translate(0,0,-.7);pose.scale(.8F,.8F,.8F);}
        pose.scale(.2F,.2F,.2F);
        CastingBraceletRenderer.renderModel(event.getItemStack(),pose,buffers,event.getPackedLight(),OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
