package tc4revival.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import tc4revival.content.PechEntity;

public  class PechModel extends EntityModel<PechEntity> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(net.minecraft.resources.ResourceLocation.parse("thaumcraft:pech"),"main");
    private final ModelPart root,head,jaw,leftArm,rightLeg,leftLeg,lowerPack;
    public final ModelPart rightArm;
    public PechModel(ModelPart root) { this.root=root; head=root.getChild("head"); jaw=root.getChild("jaw"); rightArm=root.getChild("right_arm"); leftArm=root.getChild("left_arm"); rightLeg=root.getChild("right_leg"); leftLeg=root.getChild("left_leg"); lowerPack=root.getChild("lower_pack"); }
    public static LayerDefinition layer() {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(34,12).addBox(-3,0,0,6,10,6),PartPose.offsetAndRotation(0,9,-3,.3129957f,0,0));
        root.addOrReplaceChild("right_leg",CubeListBuilder.create().texOffs(35,1).mirror().addBox(-2.9f,0,0,3,6,3),PartPose.offset(0,18,0));
        root.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(35,1).addBox(-.1f,0,0,3,6,3),PartPose.offset(0,18,0));
        root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(2,11).addBox(-3.5f,-5,-5,7,5,5),PartPose.offset(0,8,0));
        root.addOrReplaceChild("jaw",CubeListBuilder.create().texOffs(1,21).addBox(-4,-1,-6,8,3,5),PartPose.offset(0,8,0));
        root.addOrReplaceChild("lower_pack",CubeListBuilder.create().texOffs(0,0).addBox(-5,0,0,10,5,5),PartPose.offsetAndRotation(0,10,3.5f,.3013602f,0,0));
        root.addOrReplaceChild("upper_pack",CubeListBuilder.create().texOffs(64,1).addBox(-7.5f,-14,0,15,14,11),PartPose.offsetAndRotation(0,10,3,.4537856f,0,0));
        root.addOrReplaceChild("right_arm",CubeListBuilder.create().texOffs(52,2).mirror().addBox(-2,0,-1,2,6,2),PartPose.offset(-3,10,-1));
        root.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(52,2).addBox(0,0,-1,2,6,2),PartPose.offset(3,10,-1));
        return LayerDefinition.create(mesh,128,64);
    }
    @Override public void setupAnim(PechEntity entity,float walk,float amplitude,float age,float yaw,float pitch) {
        head.yRot=yaw*(float)Math.PI/180; head.xRot=pitch*(float)Math.PI/180;
        jaw.yRot=head.yRot; jaw.xRot=head.xRot+.2617994f+(float)Math.cos(walk*.6662f)*amplitude*.25f+.34906587f*Math.abs((float)Math.sin(entity.mumble));
        rightArm.xRot=(float)Math.cos(walk*.6662f+Math.PI)*amplitude; leftArm.xRot=(float)Math.cos(walk*.6662f)*amplitude;
        rightLeg.xRot=(float)Math.cos(walk*.6662f)*1.4f*amplitude; leftLeg.xRot=(float)Math.cos(walk*.6662f+Math.PI)*1.4f*amplitude;
        rightLeg.yRot=leftLeg.yRot=rightArm.yRot=leftArm.yRot=0;
        lowerPack.yRot=lowerPack.zRot=(float)Math.cos(walk*.6662f)*amplitude*.25f;
        if (riding) { rightArm.xRot-=.62831855f; leftArm.xRot-=.62831855f; rightLeg.xRot=leftLeg.xRot=-1.2566371f; rightLeg.yRot=.31415927f; leftLeg.yRot=-.31415927f; }
        float swing=1-(float)Math.pow(1-attackTime,4);
        rightArm.xRot-=(float)Math.sin(swing*Math.PI)*1.2f+(float)Math.sin(attackTime*Math.PI)*-(head.xRot-.7f)*.75f;
        rightArm.zRot=(float)Math.sin(attackTime*Math.PI)*-.4f; leftArm.zRot=0;
        if (entity.isShiftKeyDown()) { rightArm.xRot+=.4f; leftArm.xRot+=.4f; }
        float sway=(float)Math.cos(age*.09f)*.05f+.05f,bob=(float)Math.sin(age*.067f)*.05f;
        rightArm.zRot+=sway; leftArm.zRot-=sway; rightArm.xRot+=bob; leftArm.xRot-=bob;
    }
    @Override public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,com.mojang.blaze3d.vertex.VertexConsumer buffer,int light,int overlay,float r,float g,float b,float alpha) { root.render(pose,buffer,light,overlay,r,g,b,alpha); }
}
