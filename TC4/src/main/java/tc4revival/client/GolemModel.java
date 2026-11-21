package tc4revival.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import tc4revival.content.*;

public  class GolemModel extends HierarchicalModel<GolemEntity> {
    private final ModelPart root=create().bakeRoot();
    private float partial;
    private int healing;
    private net.minecraft.world.level.block.state.BlockState appearance;
    private GolemEntity rendered;
    public static LayerDefinition create() {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
        root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-11,-5.5f,8,9,8),PartPose.offset(0,30,-2));
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,40).addBox(-8,-2,-6,16,12,11)
                .texOffs(0,70).addBox(-4.5f,10,-3,9,5,6,new CubeDeformation(.5f)),PartPose.offset(0,30,0));
        root.addOrReplaceChild("right_arm",CubeListBuilder.create().texOffs(60,21).addBox(-12,-2.5f,-3,4,25,6),PartPose.offset(0,30,0));
        root.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(60,21).mirror().addBox(8,-2.5f,-3,4,25,6),PartPose.offset(0,30,0));
        root.addOrReplaceChild("right_leg",CubeListBuilder.create().texOffs(37,0).addBox(-3.5f,-3,-3,6,16,5),PartPose.offset(-4,48,0));
        root.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(37,0).mirror().addBox(-3.5f,-3,-3,6,16,5),PartPose.offset(5,48,0));
        return LayerDefinition.create(mesh,128,128);
    }
    @Override public ModelPart root() { return root; }
    @Override public void prepareMobModel(GolemEntity entity,float walk,float amount,float partial) { this.partial=partial; healing=entity.healingTicks();appearance=entity.configuration().blockAppearance();rendered=entity; }
    @Override public void setupAnim(GolemEntity entity,float walk,float amount,float age,float yaw,float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        var head=root.getChild("head"); var right=root.getChild("right_arm"); var left=root.getChild("left_arm");
        if (!entity.configuration().hasTask() || entity.bootup()<0) { head.xRot=.57595867f; return; }
        if (entity.state().inactive()) head.xRot=.57595867f;
        else if (entity.bootup()>0) head.xRot=entity.bootup()/57.295776f;
        else { head.yRot=yaw/57.295776f; head.xRot=pitch/57.295776f; }
        root.getChild("right_leg").xRot=-1.5f*triangle(walk,13)*amount;
        root.getChild("left_leg").xRot=1.5f*triangle(walk,13)*amount;
        if (entity.configuration().core()==GolemCoreType.ESSENTIA) {
            float spread=(1-(.5f+Math.min(64,entity.configuration().carryLimit())/128f))*25/57.295776f;
            left.zRot=spread; right.zRot=-spread;
        }
        int action=3-Math.abs(entity.actionTicks()-3);
        if (action>0) right.xRot=left.xRot=-2+1.5f*triangle(action-partial,5);
        else if (entity.leftArmTicks()>0 || entity.rightArmTicks()>0) {
            if (entity.leftArmTicks()>0) left.xRot=-2+1.5f*triangle(entity.leftArmTicks()-partial,20);
            if (entity.rightArmTicks()>0) right.xRot=-2+1.5f*triangle(entity.rightArmTicks()-partial,20);
        } else if (!entity.carried().isEmpty() || entity.configuration().core()==GolemCoreType.LIQUID || entity.configuration().core()==GolemCoreType.ESSENTIA) right.xRot=left.xRot=-1;
        else { right.xRot=(-.2f+1.5f*triangle(walk,13))*amount; left.xRot=(-.2f-1.5f*triangle(walk,13))*amount; }
    }
    private static float triangle(float value,float period) { return (Math.abs(value%period-period*.5f)-period*.25f)/(period*.25f); }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer vertices,int light,int overlay,float red,float green,float blue,float alpha) {
        if(appearance!=null){
            var minecraft=net.minecraft.client.Minecraft.getInstance();
            var sprite=minecraft.getBlockRenderer().getBlockModel(appearance).getParticleIcon(net.minecraftforge.client.model.data.ModelData.EMPTY);
            vertices=sprite.wrap(vertices);
            int tint=minecraft.getBlockColors().getColor(appearance,rendered.level(),rendered.blockPosition(),0);
            if(tint!=-1){red*=((tint>>16)&255)/255f;green*=((tint>>8)&255)/255f;blue*=(tint&255)/255f;}
        }
        if (healing>0) { red*=.5f+healing/10f; green*=Math.min(1,.9f+healing/5f); blue*=.5f+healing/10f; }
        pose.pushPose(); pose.scale(.4f,.4f,.4f); root.render(pose,vertices,light,overlay,red,green,blue,alpha); pose.popPose();
    }
}
