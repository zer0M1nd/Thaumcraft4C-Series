package tc4revival.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;

public  class VoidRobeModel extends HumanoidModel<LivingEntity> {
    private final boolean chest;
    public VoidRobeModel(boolean chest) { super(VoidRobeGeometry.bake(chest)); this.chest=chest; }
    public void prepare(LivingEntity wearer,EquipmentSlot slot,HumanoidModel<?> original) {
        net.minecraftforge.client.ForgeHooksClient.copyModelProperties(original,this);
        setAllVisible(false); head.visible=hat.visible=slot==EquipmentSlot.HEAD;
        body.visible=slot==EquipmentSlot.CHEST || slot==EquipmentSlot.LEGS;
        rightArm.visible=leftArm.visible=slot==EquipmentSlot.CHEST; rightLeg.visible=leftLeg.visible=slot==EquipmentSlot.LEGS;
        float partial=Minecraft.getInstance().getFrameTime(),age=wearer.tickCount+partial;
        float phase=wearer.walkAnimation.position(partial)*(wearer.isBaby()?3:1),amount=wearer.isPassenger()?0:Math.min(1,wearer.walkAnimation.speed(partial));
        float cloth=Math.min(Mth.cos(phase*.6662f),Mth.cos(phase*.6662f+Mth.PI))*1.4f*amount;
        if (!chest) for (String side:new String[]{"l","r"}) {
            body.getChild("front0_"+side).xRot=cloth-.1047198f; body.getChild("front1_"+side).xRot=cloth-.3316126f;
            body.getChild("back0_"+side).xRot=-cloth+.1047198f;
            body.getChild("back1_"+side).xRot=body.getChild("back2_"+side).xRot=-cloth+.2268928f;
        }
        if (wearer instanceof Zombie || wearer instanceof AbstractSkeleton) {
            float swing=wearer.getAttackAnim(partial),attack=Mth.sin(swing*Mth.PI),ease=Mth.sin((1-(1-swing)*(1-swing))*Mth.PI);
            float sway=Mth.cos(age*.09f)*.05f+.05f,lift=Mth.sin(age*.067f)*.05f;
            rightArm.yRot=-.1f+attack*.6f; leftArm.yRot=.1f-attack*.6f; rightArm.zRot=sway; leftArm.zRot=-sway;
            rightArm.xRot=-Mth.HALF_PI-attack*1.2f+ease*.4f+lift; leftArm.xRot=-Mth.HALF_PI-attack*1.2f+ease*.4f-lift;
        }
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,float r,float g,float b,float a) {
        pose.pushPose(); if (young) { pose.scale(.75f,.75f,.75f); pose.translate(0,1,0); } else pose.scale(1.01f,1.01f,1.01f);
        head.render(pose,out,light,overlay,r,g,b,a); pose.popPose();
        pose.pushPose(); if (young) { pose.scale(.5f,.5f,.5f); pose.translate(0,1.5,0); }
        for (var part:new ModelPart[]{body,rightArm,leftArm,rightLeg,leftLeg,hat}) part.render(pose,out,light,overlay,r,g,b,a);
        pose.popPose();
    }
}
