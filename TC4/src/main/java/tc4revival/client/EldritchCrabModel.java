package tc4revival.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import tc4revival.content.EldritchCrabEntity;

public  class EldritchCrabModel extends HierarchicalModel<EldritchCrabEntity> {
    private final ModelPart root;
    public EldritchCrabModel() { root=definition().bakeRoot(); }
    private static void part(PartDefinition root,String name,int u,int v,float x,float y,float z,int w,int h,int d,float px,float py,float pz,float rx,float ry,boolean mirror) {
        var cube=CubeListBuilder.create().texOffs(u,v); if (mirror) cube.mirror();
        root.addOrReplaceChild(name,cube.addBox(x,y,z,w,h,d),PartPose.offsetAndRotation(px,py,pz,rx,ry,0));
    }
    public static LayerDefinition definition() {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
        part(root,"shell",0,0,-4.5f,-4.5f,-.4f,9,9,9,0,18,0,.1047198f,0,false);
        part(root,"tail",64,0,-4,-4,-.4f,8,8,8,0,18,0,.1047198f,0,false);
        part(root,"torso",0,18,-3.5f,-3.5f,-6.066667f,7,7,6,0,18,0,.0523599f,0,false);
        part(root,"head",0,31,-2.5f,-2,-8.066667f,5,5,2,0,18,0,0,0,false);
        part(root,"mouth",0,38,-2,-1.5f,-9.066667f,4,4,1,0,18,0,0,0,false);
        for (int side:new int[]{-1,1}) {
            String name=side<0 ? "right" : "left";
            part(root,name+"_claw_base",0,55,-2,-2.5f,-3.066667f,4,5,3,side*6,17,-7,0,0,side>0);
            part(root,name+"_claw_upper",0,47,-2,-1,-5.066667f,4,3,5,side*6,15.5f,-10,0,0,side>0);
            part(root,name+"_claw_lower",14,54,-1.5f,-1,-4.066667f,3,2,5,side*6,18.5f,-10,.3141593f,0,false);
            part(root,name+"_arm",44,4,-1,-1,side<0 ? -5.066667f : -4.066667f,2,2,6,side<0 ? -3 : 4,17,side<0 ? -4 : -5,0,-side*.7504916f,false);
            for (int row=0;row<2;row++) for (int segment=0;segment<2;segment++) {
                boolean front=row==1; float x=side<0 ? (front ? -5 : -4.5f) : segment==0 ? (front ? -1 : -1.5f) : (front ? 3 : 2.5f);
                part(root,name+"_leg_"+row+"_"+segment,36,segment==0 ? 0 : 4,x,segment==0 ? -1 : 1,front ? -1.066667f : -.9f,
                        segment==0 ? 6 : 2,segment==0 ? 2 : 5,2,side*4,20,front ? -3.5f : -1.5f,0,0,false);
            }
        }
        return LayerDefinition.create(mesh,128,64);
    }
    @Override public ModelPart root() { return root; }
    @Override public void setupAnim(EldritchCrabEntity entity,float walk,float amplitude,float age,float yaw,float pitch) {
        root.getChild("shell").visible=entity.hasHelm(); root.getChild("tail").visible=!entity.hasHelm();
        for (int side:new int[]{-1,1}) {
            String name=side<0 ? "right" : "left";
            for (int row=0;row<2;row++) for (int segment=0;segment<2;segment++) {
                var leg=root.getChild(name+"_leg_"+row+"_"+segment);
                float wave=-Mth.cos(walk*.6662f*2+(row==0 ? 0 : Mth.PI))*.4f*amplitude;
                leg.yRot=-side*((row==0 ? 1 : -1)*.2094395f+wave); leg.zRot=-side*(.4363323f+wave);
            }
            float oscillation=Mth.sin(entity.tickCount/(side<0 ? 4f : 4.1f));
            root.getChild(name+"_claw_lower").xRot=.3141593f+side*oscillation*.25f;
            root.getChild(name+"_claw_upper").xRot=-side*oscillation*.125f;
        }
        float swing=Mth.cos(walk*.6662f)*amplitude*.125f;
        for (String name:new String[]{"shell","tail"}) { var tail=root.getChild(name); tail.yRot=swing*2; tail.zRot=swing; }
    }
}
