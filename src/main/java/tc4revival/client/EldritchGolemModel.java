package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import tc4revival.content.EldritchGolemEntity;

/** Legacy numeric geometry with modern hierarchical arm and shoulder parts. */
public  class EldritchGolemModel extends HierarchicalModel<EldritchGolemEntity> {
    private final ModelPart root=create().bakeRoot();
    private static PartDefinition box(PartDefinition parent,String name,int u,int v,boolean mirror,float x,float y,float z,float w,float h,float d,float px,float py,float pz,float rx,float rz) {
        return parent.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).mirror(mirror).addBox(x,y,z,w,h,d),PartPose.offsetAndRotation(px,py,pz,rx,0,rz));
    }
    public static LayerDefinition create() {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
        box(root,"cloak1",0,47,false,-5,1.5f,4,10,12,1,0,0,-2.5f,.1396263f,0);
        box(root,"cloak2",0,59,false,-5,13.5f,1.7f,10,4,1,0,0,-2.5f,.3069452f,0);
        box(root,"cloak3",0,37,false,-5,17.5f,-.8f,10,4,1,0,0,-2.5f,.4465716f,0);
        for (int side:new int[]{-1,1}) box(root,"cloak_clasp"+side,0,43,false,side<0 ? -5 : 3,.5f,2,2,1,3,0,0,-2.5f,.1396263f,0);
        box(root,"head",47,12,false,-3.5f,-6,-2.5f,7,7,5,0,4.5f,-3.8f,-.1047198f,0);
        box(root,"headless",26,16,false,-2,-2,-2,4,4,4,0,0,-5,-.1047198f,0);
        box(root,"collar_left",75,50,false,3.5f,-.5f,-7,1,4,10,0,0,-2.5f,.837758f,0);
        box(root,"collar_right",67,50,false,-4.5f,-.5f,-7,1,4,10,0,0,-2.5f,.837758f,0);
        for (int side:new int[]{-1,1}) box(root,"collar"+side,77,59,false,-3.5f,-.5f,side<0 ? -7 : 2,7,4,1,0,0,-2.5f,.837758f,0);
        box(root,"collar_black",22,0,false,-3.5f,0,-6,7,1,8,0,0,-2.5f,.837758f,0);
        // The old calls to setTextureSize occur after addBox; those cubes use 128x64 UVs.
        box(root,"cloth0",114,52,false,-3,3.2f,-3.5f,6,10,1,0,0,-2.5f,.1745329f,0);
        box(root,"cloth1",114,39,false,-1,1.5f,-3.5f,6,6,1,-2,12,0,-.1047198f,0);
        box(root,"cloth2",114,47,false,-1,8.5f,-1.5f,6,3,1,-2,11,0,-.3316126f,0);
        box(root,"torso",34,45,false,-5,2.5f,-3,10,10,6,0,0,-2.5f,.1745329f,0);
        for (int side:new int[]{-1,1}) {
            boolean left=side>0; String suffix=left ? "left" : "right";
            var arm=box(root,"arm_"+suffix,78,32,left,left ? -.5f : -3.5f,1.5f,-2,4,13,5,side*5,3,-2,0,-side*.1047198f);
            box(arm,"shoulder",0,0,left,left ? .3f : -4.3f,-1,-3,4,5,7,0,0,0,0,-side*1.186824f);
            box(arm,"shoulder0",56,31,left,left ? -.5f : -4.5f,-1.5f,-2.5f,5,6,6,0,0,0,0,0);
            box(arm,"shoulder1",0,23,left,left ? 2.3f : -3.3f,4,-2.5f,1,2,6,0,0,0,0,-side*1.186824f);
            box(arm,"shoulder2",0,12,left,left ? .3f : -2.3f,4,-3,2,3,7,0,0,0,0,-side*1.186824f);
            box(root,"back_"+suffix,96,7,false,left ? -2 : 0,2.5f,-2.5f,2,2,5,side*2,12,0,0,-side*.1396263f);
            box(root,"waist1_"+suffix,96,14,false,left ? -2 : -3,-.5f,-2.5f,5,3,5,side*2,12,0,0,-side*.1396263f);
            box(root,"waist2_"+suffix,116,13,false,left ? 2 : -3,2.5f,-2.5f,1,4,5,side*2,12,0,0,-side*.1396263f);
            box(root,"waist3_"+suffix,114,5,!left,left ? 0 : -2,2.5f,-2.5f,2,3,5,side*2,12,0,0,-side*.1396263f);
            box(root,"leg_"+suffix,79,19,false,left ? -1.5f : -2.5f,2.5f,-2,4,9,4,side*2,12.5f,0,0,0);
        }
        return LayerDefinition.create(mesh,128,64);
    }
    @Override public ModelPart root() { return root; }
    @Override public void setupAnim(EldritchGolemEntity entity,float walk,float amount,float age,float yaw,float pitch) {
        var head=root.getChild("head"); var broken=root.getChild("headless"); head.visible=!entity.headless(); broken.visible=entity.headless();
        if (entity.spawnTimer()>0) { head.yRot=0; head.xRot=(entity.spawnTimer()/2)*Mth.DEG_TO_RAD; }
        else { head.yRot=yaw/4*Mth.DEG_TO_RAD; head.xRot=pitch/2*Mth.DEG_TO_RAD; broken.yRot=yaw*Mth.DEG_TO_RAD; broken.xRot=pitch*Mth.DEG_TO_RAD; }
        root.getChild("leg_right").xRot=Mth.cos(walk*.4662f)*1.4f*amount;
        root.getChild("leg_left").xRot=Mth.cos(walk*.4662f+Mth.PI)*1.4f*amount;
        float cloth=Math.min(Mth.cos(walk*.44f),Mth.cos(walk*.44f+Mth.PI))*1.4f*amount;
        root.getChild("cloth1").xRot=cloth-.1047198f; root.getChild("cloth2").xRot=cloth-.3316126f;
        root.getChild("cloak1").xRot=-cloth/3+.1396263f; root.getChild("cloak2").xRot=-cloth/3+.3069452f; root.getChild("cloak3").xRot=-cloth/3+.4465716f;
    }
    @Override public void prepareMobModel(EldritchGolemEntity entity,float walk,float amount,float partial) {
        float right,left;
        if (entity.attackTimer()>0) right=left=-2+1.5f*(Math.abs((entity.attackTimer()-partial)%10-5)-2.5f)/2.5f;
        else { right=Mth.cos(walk*.4f+Mth.PI)*amount; left=Mth.cos(walk*.4f)*amount; }
        root.getChild("arm_right").xRot=right; root.getChild("arm_left").xRot=left;
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,float r,float g,float b,float a) {
        root.render(pose,out,light,overlay,r,g,b,a);
        // TC4 draws this front cloth twice; keep its resulting translucent overlap.
        root.getChild("cloth1").render(pose,out,light,overlay,r,g,b,a);
    }
}
