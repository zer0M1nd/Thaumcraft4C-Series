package emtmodern.client;

import com.mojang.blaze3d.vertex.*;
import emtmodern.item.InfusedQuantumChestplate;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class QuantumChestArmorExtensions implements IClientItemExtensions {
    protected final java.util.Map<Integer,ChestModel> models=new java.util.HashMap<>();
    @Override public Model getGenericArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original){
        var attachment=InfusedQuantumChestplate.attachment(stack);int kind=attachment==null?0:attachment.number()==1?1:2;
        ChestModel model=models.computeIfAbsent(kind,ChestModel::new);model.prepare(entity,stack,original);return model;
    }
    public static class ChestModel extends HumanoidModel<LivingEntity> {
        protected final int kind;
        protected final ModelPart rightWing,leftWing,center,jetpack;
        protected boolean sneaking;
        public ChestModel(int kind){
            super(body(kind));this.kind=kind;
            var wingMesh=new MeshDefinition();var root=wingMesh.getRoot();
            root.addOrReplaceChild("right",CubeListBuilder.create().texOffs(24,32).addBox(-11,-1,0,11,13,1),PartPose.offset(-2,0,3));
            root.addOrReplaceChild("left",CubeListBuilder.create().texOffs(0,32).addBox(0,-1,0,11,13,1),PartPose.offset(2,0,3));
            root.addOrReplaceChild("center",CubeListBuilder.create().texOffs(0,46).addBox(0,0,1,8,14,1),PartPose.offset(-4,0,3));
            // Original sets mirror and 64x32 AFTER addBox: the constructed wing cubes
            // keep non-mirrored 64x64 UVs. The jetpack sets 64x48 BEFORE addBox.
            var wings=LayerDefinition.create(wingMesh,64,64).bakeRoot();rightWing=wings.getChild("right");leftWing=wings.getChild("left");center=wings.getChild("center");
            var jetMesh=new MeshDefinition();jetMesh.getRoot().addOrReplaceChild("jetpack",CubeListBuilder.create().texOffs(16,69).addBox(-4,-12,-2,8,24,4,new CubeDeformation(2)),PartPose.ZERO);jetpack=LayerDefinition.create(jetMesh,64,48).bakeRoot().getChild("jetpack");
        }
        protected static ModelPart body(int kind){return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1),0),kind==1?128:64,kind==0?32:kind==1?96:64).bakeRoot();}
        @SuppressWarnings("unchecked") public void prepare(LivingEntity entity,ItemStack stack,HumanoidModel<?> original){
            ((HumanoidModel<LivingEntity>)original).copyPropertiesTo(this);setAllVisible(false);body.visible=leftArm.visible=rightArm.visible=true;sneaking=entity.isShiftKeyDown();
            boolean jump=stack.hasTag()&&stack.getTag().getBoolean("isJumping");rightWing.yRot=jump?.5f:.1570796f;leftWing.yRot=-rightWing.yRot;
            rightWing.xRot=leftWing.xRot=center.xRot=jetpack.xRot=sneaking?.5f:0;
        }
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,float red,float green,float blue,float alpha){
            body.render(pose,buffer,light,overlay,red,green,blue,alpha);rightArm.render(pose,buffer,light,overlay,red,green,blue,alpha);leftArm.render(pose,buffer,light,overlay,red,green,blue,alpha);
            if(kind==1)jetpack.render(pose,buffer,light,overlay,red,green,blue,alpha);
            if(kind==2){pose.pushPose();if(sneaking)pose.translate(0,-.1,0);center.render(pose,buffer,light,overlay,red,green,blue,alpha);leftWing.render(pose,buffer,light,overlay,red,green,blue,alpha);rightWing.render(pose,buffer,light,overlay,red,green,blue,alpha);pose.popPose();}
        }
    }
}
