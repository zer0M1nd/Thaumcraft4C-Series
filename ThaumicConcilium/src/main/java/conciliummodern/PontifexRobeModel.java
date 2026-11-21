package conciliummodern;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class PontifexRobeModel extends HumanoidModel<LivingEntity> {
    public PontifexRobeModel(){super(PontifexRobeGeometry.bake());}
    void prepare(LivingEntity wearer,EquipmentSlot slot,HumanoidModel<?> original){
        net.minecraftforge.client.ForgeHooksClient.copyModelProperties(original,this);
        setAllVisible(false);head.visible=slot==EquipmentSlot.HEAD;body.visible=slot==EquipmentSlot.CHEST||slot==EquipmentSlot.LEGS;
        rightArm.visible=leftArm.visible=slot==EquipmentSlot.CHEST;rightLeg.visible=leftLeg.visible=slot==EquipmentSlot.LEGS||slot==EquipmentSlot.FEET;
        body.getChild("cloth").visible=slot==EquipmentSlot.LEGS;body.getChild("armor").visible=slot==EquipmentSlot.CHEST;
        rightLeg.getChild("Shape45RL").visible=leftLeg.getChild("Shape50LL").visible=slot==EquipmentSlot.LEGS;
        rightLeg.getChild("rightBoot").visible=leftLeg.getChild("leftBoot").visible=slot==EquipmentSlot.FEET;
        float partial=Minecraft.getInstance().getFrameTime(),phase=wearer.walkAnimation.position(partial),amount=wearer.isPassenger()?0:Math.min(1,wearer.walkAnimation.speed(partial));
        float c=Math.min(Mth.cos(phase*.6662f),Mth.cos(phase*.6662f+Mth.PI))*1.4f*amount;var cloth=body.getChild("cloth");
        for(String side:new String[]{"R","L"}){
            cloth.getChild("Frontcloth"+side+"1").xRot=c-.1047198f;cloth.getChild("Frontcloth"+side+"2").xRot=c-.3316126f;
            cloth.getChild("ClothBack"+side+"1").xRot=-c+.1047198f;
            cloth.getChild("ClothBack"+side+"2").xRot=cloth.getChild("ClothBack"+side+"3").xRot=-c+.2268928f;
        }
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,float r,float g,float b,float a){
        pose.pushPose();
        head.render(pose,out,light,overlay,r,g,b,a);body.render(pose,out,light,overlay,r,g,b,a);
        pose.pushPose();pose.scale(1.2f,1.2f,1.2f);rightArm.render(pose,out,light,overlay,r,g,b,a);leftArm.render(pose,out,light,overlay,r,g,b,a);pose.popPose();
        pose.pushPose();pose.scale(1.05f,1.05f,1.05f);rightLeg.render(pose,out,light,overlay,r,g,b,a);leftLeg.render(pose,out,light,overlay,r,g,b,a);pose.popPose();pose.popPose();
    }
    public static final class Extensions implements IClientItemExtensions {
        private PontifexRobeModel model;
        @Override public Model getGenericArmorModel(LivingEntity wearer,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original){if(model==null)model=new PontifexRobeModel();model.prepare(wearer,slot,original);return model;}
    }
}
