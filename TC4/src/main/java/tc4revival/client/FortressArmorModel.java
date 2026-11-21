package tc4revival.client;

import com.mojang.blaze3d.vertex.*;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import tc4revival.content.FortressArmorItem;

public  class FortressArmorModel extends HumanoidModel<LivingEntity> {
    private static final WeakHashMap<LivingEntity,Appearance> APPEARANCE=new WeakHashMap<>();
    private static  class Appearance { int pieces=-1,mask=-1; boolean goggles; }
    private final boolean chest;
    public FortressArmorModel(boolean chest) { super(FortressArmorGeometry.bake(chest)); this.chest=chest; }
    public void prepare(LivingEntity wearer,EquipmentSlot slot,HumanoidModel<?> original) {
        net.minecraftforge.client.ForgeHooksClient.copyModelProperties(original,this);
        setAllVisible(false); head.visible=hat.visible=slot==EquipmentSlot.HEAD;
        body.visible=slot==EquipmentSlot.CHEST || slot==EquipmentSlot.LEGS;
        rightArm.visible=leftArm.visible=slot==EquipmentSlot.CHEST; rightLeg.visible=leftLeg.visible=slot==EquipmentSlot.LEGS;
        var look=APPEARANCE.computeIfAbsent(wearer,key->new Appearance());
        if (wearer.tickCount%20==0) {
            int count=0;
            for (var armorSlot:new EquipmentSlot[]{EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD}) {
                var piece=wearer.getItemBySlot(armorSlot);
                if (!(piece.getItem() instanceof FortressArmorItem)) continue;
                count++;
                if (armorSlot==EquipmentSlot.HEAD) { look.mask=FortressArmorItem.mask(piece); look.goggles=FortressArmorItem.hasGoggles(piece); }
            }
            look.pieces=count>0?count:-1;
        }
        int count=look.pieces;
        head.getChild("goggles").visible=look.goggles;
        for (int i=0;i<3;i++) head.getChild("mask"+i).visible=look.mask==i;
        for (String name:new String[]{"gem","gem_frame","ornament1_l","ornament2_l","ornament1_r","ornament2_r"}) head.getChild(name).visible=count>=3;
        head.getChild("flap_l").visible=head.getChild("flap_r").visible=count>=2;
        if (chest) { body.getChild("scroll").visible=count>=3; body.getChild("book").visible=count>=2; }
        for (var arm:new ModelPart[]{leftArm,rightArm}) {
            arm.getChild("plate_top").visible=arm.getChild("plate0").visible=count>=2;
            arm.getChild("plate1").visible=arm.getChild("plate2").visible=count>=3;
        }
        for (var leg:new ModelPart[]{leftLeg,rightLeg}) { leg.getChild("side1").visible=count>=2; leg.getChild("side2").visible=count>=3; }
        if (wearer instanceof Zombie || wearer instanceof AbstractSkeleton) {
            float partial=Minecraft.getInstance().getFrameTime(),swing=wearer.getAttackAnim(partial);
            float attack=Mth.sin(swing*Mth.PI),ease=Mth.sin((1-(1-swing)*(1-swing))*Mth.PI),age=wearer.tickCount+partial;
            float sway=Mth.cos(age*.09f)*.05f+.05f,lift=Mth.sin(age*.067f)*.05f;
            rightArm.yRot=-.1f+attack*.6f; leftArm.yRot=.1f-attack*.6f;
            rightArm.zRot=sway; leftArm.zRot=-sway;
            rightArm.xRot=-Mth.HALF_PI-attack*1.2f+ease*.4f+lift; leftArm.xRot=-Mth.HALF_PI-attack*1.2f+ease*.4f-lift;
        }
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,float r,float g,float b,float a) {
        pose.pushPose();
        if (young) { pose.scale(.75f,.75f,.75f); pose.translate(0,1,0); } else pose.scale(1.01f,1.01f,1.01f);
        head.render(pose,out,light,overlay,r,g,b,a); pose.popPose();
        pose.pushPose(); if (young) { pose.scale(.5f,.5f,.5f); pose.translate(0,1.5,0); }
        for (var part:new ModelPart[]{body,rightArm,leftArm,rightLeg,leftLeg,hat}) part.render(pose,out,light,overlay,r,g,b,a);
        pose.popPose();
    }
}
