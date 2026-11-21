package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import tc4revival.content.EldritchGuardianEntity;

/** Original cuboid dimensions and articulated cloth, rebuilt with the modern model hierarchy. */
public  class EldritchGuardianModel<T extends net.minecraft.world.entity.Mob> extends HierarchicalModel<T> {
    private final ModelPart root,eye,hood,leftArm,rightArm,front,cloak,leftSide,rightSide;
    public float opacity=1;
    public EldritchGuardianModel() {
        root=create().bakeRoot(); var eyeMesh=new MeshDefinition();
        box(eyeMesh.getRoot(),"eye",0,0,-4,-8,-4,8,8,8,0,-6,0,0,0,0);
        eye=LayerDefinition.create(eyeMesh,128,64).bakeRoot().getChild("eye"); hood=root.getChild("hood"); leftArm=root.getChild("left_arm"); rightArm=root.getChild("right_arm");
        front=root.getChild("front"); cloak=root.getChild("cloak"); leftSide=root.getChild("left_side"); rightSide=root.getChild("right_side");
    }
    private static PartDefinition box(PartDefinition parent,String name,int u,int v,float x,float y,float z,float w,float h,float d,
                                      float px,float py,float pz,float rx,float ry,float rz) {
        return parent.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,w,h,d),PartPose.offsetAndRotation(px,py,pz,rx,ry,rz));
    }
    public static LayerDefinition create() {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
        for (int side:new int[]{-1,1}) for (int tier=0;tier<2;tier++)
            box(root,"belt_"+side+"_"+tier,76,44,side<0 ? -5 : 4,tier==0 ? 4 : 8,-3,1,3,6,0,-6,0,0,0,0);
        box(root,"belt_front",56,55,-4,8,-3,8,4,1,0,-6,0,0,0,0);
        box(root,"chest",56,45,-4,1,-4,8,7,2,0,-6,0,0,0,0);
        box(root,"back",36,45,-4,1,2,8,11,2,0,-6,0,0,0,0);
        var hood=box(root,"hood",40,12,-4,-8,-4,8,8,8,0,-6,0,0,0,0);
        box(hood,"fold1",36,28,-3.5f,-8.7f,2,7,7,3,0,0,0,-.2268928f,0,0);
        box(hood,"fold2",22,19,-3,-9,2.5f,6,6,3,0,0,0,-.3490659f,0,0);
        box(hood,"fold3",40,4,-2.5f,-9.7f,3.5f,5,5,3,0,0,0,-.5759587f,0,0);
        for (int side:new int[]{-1,1}) {
            String name=side<0 ? "right" : "left"; float pivot=side*5;
            float shoulderX=side<0 ? -3.5f : -1.5f;
            var shoulder=CubeListBuilder.create().texOffs(56,35);
            if (side>0) shoulder.mirror();
            root.addOrReplaceChild(name+"_shoulder",shoulder.addBox(shoulderX,-2.5f,-2.5f,5,5,5),
                    PartPose.offsetAndRotation(pivot,-4,0,-.3665191f,-side*.122173f,-side*.0349066f));
            box(root,name+"_shoulder_top",110,37,side<0 ? -5.5f : 3.5f,-2.5f,-3.5f,2,1,7,pivot,-4,0,-.3665191f,-side*.3141593f,-side*.4363323f);
            for (int tier=0;tier<3;tier++) box(root,name+"_shoulder_plate"+tier,tier==0 ? 110 : 94,45,
                    side<0 ? -4.5f+tier : 3.5f-tier,tier==0 ? -1.5f : tier==1 ? 1.5f : 3.5f,-3.5f,
                    1,tier==0 ? 4 : 3,7,pivot,-4,0,-.3665191f,-side*.3141593f,-side*.4363323f);
            float armX=side<0 ? -3 : -1;
            var arm=box(root,name+"_arm",72,8,armX,2.5f,-1.5f,4,10,5,pivot,-4,0,-.9599311f,-side*.1047198f,-side*.1919862f);
            box(arm,"lower",76,28,armX,9.5f,3.5f,4,3,3,0,0,0,0,0,0);
            box(arm,"upper",76,23,armX,6.5f,3.5f,4,3,2,0,0,0,0,0,0);
            for (int tier=0;tier<3;tier++) {
                box(root,name+"_leg_front"+tier,tier==2 ? 6 : 0,tier==1 ? 47 : 43,side<0 ? -3 : 1,.5f+tier*2,-3.5f+tier,
                        2,3,1,side*2,6,0,-.4363323f,0,0);
                box(root,name+"_leg_back"+tier,0,18,side<0 ? -3 : -2,.5f+tier*2,2.5f-tier,
                        5,3,1,side*2,6,0,.4363323f,0,0);
            }
            box(root,name+"_hip",0,22,side<0 ? -2.5f : 1.5f,.5f,-2.5f,1,4,5,side*2,6,0,0,0,-side*.4363323f);
            var panel=box(root,name+"_side",0,54,0,0,-.5f,1,5,5,side*4.5f,9.5f,-2,0,0,-side*.122173f);
            var middle=box(panel,"middle",0,35,0,0,-.5f,1,3,5,0,5,0,0,0,-side*.296706f);
            box(middle,"end",24,35,0,0,-.5f,1,3,5,0,3,0,0,0,-side*.5235988f);
        }
        var front=box(root,"front",16,45,-3,0,-.5f,6,8,1,0,5.5f,-3,0,0,0);
        var frontMiddle=box(front,"middle",16,54,-3,0,-.5f,6,4,1,0,8,0,0,0,0);
        box(frontMiddle,"end",32,59,-3,0,-.5f,6,4,1,0,4,0,0,0,0);
        var cloak=box(root,"cloak",106,0,0,0,-.5f,10,18,1,-5,-6,4,0,0,0);
        var cloakMiddle=box(cloak,"middle",106,19,0,0,-.5f,10,4,1,0,18,0,0,0,0);
        box(cloakMiddle,"end",106,24,0,0,-.5f,10,4,1,0,4,0,0,0,0);
        return LayerDefinition.create(mesh,128,64);
    }
    public void renderEye(PoseStack pose,VertexConsumer out,int light) {
        pose.pushPose(); pose.scale(1.01f,1.01f,1.01f); eye.render(pose,out,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY); pose.popPose();
    }
    @Override public ModelPart root() { return root; }
    @Override public void setupAnim(T entity,float walk,float amount,float age,float yaw,float pitch) {
        hood.yRot=yaw*Mth.DEG_TO_RAD; hood.xRot=pitch*Mth.DEG_TO_RAD;
        eye.xRot=hood.xRot; eye.yRot=hood.yRot;
        float left=entity instanceof EldritchGuardianEntity guardian ? guardian.leftArmLift : entity instanceof tc4revival.content.EldritchWardenEntity warden ? warden.leftArmLift : 0;
        float right=entity instanceof EldritchGuardianEntity guardian ? guardian.rightArmLift : entity instanceof tc4revival.content.EldritchWardenEntity warden ? warden.rightArmLift : 0;
        leftArm.xRot=-1-left+Mth.sin((age+20)/10)*.08f;
        rightArm.xRot=-1-right+Mth.sin(age/10)*.08f;
        cloth(front,age,8,-.15f,.12f,.13f,.14f,false);
        cloth(cloak,age,7,.2f,.08f,.1f,.12f,false);
        cloth(leftSide,age+10,8,-.2f,.12f,.13f,.14f,true);
        cloth(rightSide,age-5,8,.2f,.12f,.13f,.14f,true);
    }
    private static void cloth(ModelPart part,float time,float period,float base,float first,float second,float third,boolean sideways) {
        var middle=part.getChild("middle"); var end=middle.getChild("end");
        float a=base+Mth.sin(time/period)*first,b=Mth.sin((time-5)/period)*second,c=Mth.sin((time-10)/period)*third;
        if (sideways) { part.zRot=a; middle.zRot=b; end.zRot=c; } else { part.xRot=a; middle.xRot=b; end.xRot=c; }
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer vertices,int light,int overlay,float red,float green,float blue,float alpha) {
        root.render(pose,vertices,light,overlay,red,green,blue,alpha*opacity);
    }
}
