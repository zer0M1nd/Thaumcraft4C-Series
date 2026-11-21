package conciliummodern;

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


/** Attaches both held bracelets to the animated player arms, including slim skins and crouching. */
public class CastingBraceletLayer extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    public CastingBraceletLayer(RenderLayerParent<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> parent){super(parent);}
    @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,AbstractClientPlayer player,
            float swing,float amount,float partial,float age,float yaw,float pitch) {
        if(player.isInvisible())return;
        for(var hand:InteractionHand.values()) {
            var stack=player.getItemInHand(hand);
            if(!(stack.getItem() instanceof CastingBracelet))continue;
            var side=hand==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();
            pose.pushPose();
            if(player.isBaby()){pose.translate(0,.75,0);pose.scale(.5F,.5F,.5F);}
            var arm=side==HumanoidArm.RIGHT?getParentModel().rightArm:getParentModel().leftArm;
            arm.translateAndRotate(pose);
            if(side==HumanoidArm.LEFT)pose.scale(-1,1,1);
            // Original full-3D held-item frame followed by the bracelet EQUIPPED branch.
            pose.translate(-.0625,.4375,.0625);pose.translate(0,.1875,0);pose.scale(.625f,-.625f,.625f);
            pose.mulPose(Axis.XP.rotationDegrees(-100));pose.mulPose(Axis.YP.rotationDegrees(45));pose.translate(-.5,-.5,-.5);
            pose.scale(.25f,.25f,.25f);pose.mulPose(Axis.YP.rotationDegrees(-45));pose.translate(2.85,2.9,1.3);pose.scale(.2f,.2f,.2f);
            CastingBraceletRenderer.renderModel(stack,pose,buffers,light,OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }
}
