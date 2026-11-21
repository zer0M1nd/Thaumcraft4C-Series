package tbmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import tbmodern.Bracelet;

/** Attaches both held bracelets to the animated player arms, including slim skins and crouching. */
public class BraceletLayer extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    public BraceletLayer(RenderLayerParent<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> parent){super(parent);}
    @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,AbstractClientPlayer player,
            float swing,float amount,float partial,float age,float yaw,float pitch) {
        if(player.isInvisible())return;
        for(var hand:InteractionHand.values()) {
            var stack=player.getItemInHand(hand);
            if(!(stack.getItem() instanceof Bracelet))continue;
            var side=hand==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();
            pose.pushPose();
            if(player.isBaby()){pose.translate(0,.75,0);pose.scale(.5F,.5F,.5F);}
            var arm=side==HumanoidArm.RIGHT?getParentModel().rightArm:getParentModel().leftArm;
            arm.translateAndRotate(pose);
            float center=player.getModelName().equals("slim")?.5F:1F;
            pose.translate((side==HumanoidArm.RIGHT?-center:center)/16F,9F/16,0);
            pose.mulPose(Axis.XP.rotationDegrees(90));pose.scale(.05F,.05F,.05F);
            BraceletRenderer.renderModel(stack,pose,buffers,light,OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }
}
