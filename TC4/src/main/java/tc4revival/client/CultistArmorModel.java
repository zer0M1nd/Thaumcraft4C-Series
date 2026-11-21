package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import tc4revival.content.CultistEquipment.Style;

/** Armor poses follow the wearing model; cloth motion retains the original independently hinged pieces. */
public  class CultistArmorModel extends HumanoidModel<LivingEntity> {
    private final Style style;
    private final boolean chest;
    public CultistArmorModel(Style style,boolean chest) { super(CultistArmorGeometry.bake(style,chest)); this.style=style; this.chest=chest; }
    public void prepare(LivingEntity wearer,EquipmentSlot slot,HumanoidModel<?> original) {
        net.minecraftforge.client.ForgeHooksClient.copyModelProperties(original,this);
        setAllVisible(false); head.visible=slot==EquipmentSlot.HEAD; hat.visible=head.visible;
        body.visible=slot==EquipmentSlot.CHEST || slot==EquipmentSlot.LEGS;
        rightArm.visible=leftArm.visible=slot==EquipmentSlot.CHEST; rightLeg.visible=leftLeg.visible=slot==EquipmentSlot.LEGS;
        float partial=Minecraft.getInstance().getFrameTime();
        float amount=wearer.isPassenger() ? 0 : Math.min(1,wearer.walkAnimation.speed(partial));
        float phase=wearer.walkAnimation.position(partial)*(wearer.isBaby() ? 3 : 1);
        float right=Mth.cos(phase*.6662f)*1.4f*amount,left=Mth.cos(phase*.6662f+Mth.PI)*1.4f*amount,min=Math.min(right,left);
        if (style==Style.ROBE && !chest) {
            for (String side:new String[]{"left","right"}) {
                angle("front1_"+side,min-.1047198f); angle("front2_"+side,min-.3316126f);
                angle("back1_"+side,-min+.1047198f); angle("back2_"+side,-min+.2268928f); angle("back3_"+side,-min+.2268928f);
            }
        } else if (style!=Style.ROBE && chest) {
            if (style==Style.LEADER) { angle("leg_cloth_right",right-.1047198f); angle("leg_cloth_left",left-.1047198f); }
            else { angle("front1",min-.1047198f); angle("front2",min-.3316126f); }
            angle("cloak1",-min/2+.1396263f); angle("cloak2",-min/2+.3069452f); angle("cloak3",-min/2+.4465716f);
        }
        if (wearer instanceof Zombie || wearer instanceof AbstractSkeleton) {
            float swing=wearer.getAttackAnim(partial),attack=Mth.sin(swing*Mth.PI),ease=Mth.sin((1-(1-swing)*(1-swing))*Mth.PI);
            float age=wearer.tickCount+partial,sway=Mth.cos(age*.09f)*.05f+.05f,lift=Mth.sin(age*.067f)*.05f;
            rightArm.yRot=-(.1f-attack*.6f); leftArm.yRot=.1f-attack*.6f;
            rightArm.zRot=sway; leftArm.zRot=-sway;
            rightArm.xRot=-Mth.HALF_PI-attack*1.2f+ease*.4f+lift; leftArm.xRot=-Mth.HALF_PI-attack*1.2f+ease*.4f-lift;
        }
    }
    private void angle(String name,float angle) { body.getChild(name).xRot=angle; }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,float r,float g,float b,float a) {
        pose.pushPose();
        if (young) { pose.scale(.75f,.75f,.75f); pose.translate(0,1,0); }
        else pose.scale(1.01f,1.01f,1.01f);
        head.render(pose,out,light,overlay,r,g,b,a); pose.popPose();
        pose.pushPose(); if (young) { pose.scale(.5f,.5f,.5f); pose.translate(0,1.5,0); }
        for (ModelPart part:new ModelPart[]{body,rightArm,leftArm,rightLeg,leftLeg,hat}) part.render(pose,out,light,overlay,r,g,b,a);
        pose.popPose();
    }
}
