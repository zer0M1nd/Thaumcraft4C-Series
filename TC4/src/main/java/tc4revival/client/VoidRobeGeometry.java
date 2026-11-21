package tc4revival.client;

import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;

 class VoidRobeGeometry {
    private VoidRobeGeometry() {}
    static ModelPart bake(boolean chest) {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
        var head=empty(root,"head",0,0,0); empty(root,"hat",0,0,0); var body=empty(root,"body",0,0,0);
        var right=empty(root,"right_arm",-5,2,0); var left=empty(root,"left_arm",5,2,0);
        var rleg=empty(root,"right_leg",-1.9f,12,0); var lleg=empty(root,"left_leg",1.9f,12,0);
        box(head,"hood0",16,7,-4.5f,-9,-4.6f,9,9,9,0,0,0,false);
        box(head,"hood1",52,13,-4,-9.7f,2,8,9,3,-.2268928f,0,0,false);
        box(head,"hood2",52,14,-3.5f,-10,3.5f,7,8,3,-.3490659f,0,0,false);
        box(head,"hood3",53,15,-3,-10.7f,3.5f,6,7,3,-.5759587f,0,0,false);
        box(body,"belt_front",16,55,-4,7,-3,8,5,1,0,0,0,false);
        box(body,"belt_back",16,55,-4,7,-4,8,5,1,0,3.141593f,0,false);
        for (int x:new int[]{-5,4}) box(body,"belt"+x,16,36,x,8,-3,1,3,6,0,0,0,false);
        if (chest) {
            box(body,"chest",16,25,-4,1,-3,8,6,1,0,0,0,false);
            box(body,"chest_ornament",56,50,-2.5f,1,-4,5,7,1,0,0,0,false);
            box(body,"scroll",78,25,-2,9.5f,4,8,3,3,0,0,.1919862f,false);
            box(body,"back",36,45,-4,1,1.9f,8,11,2,0,0,0,false);
            box(body,"book",81,16,1,0,4,5,7,2,0,0,.7679449f,false);
            box(body,"cloth_l",108,38,2.1f,.5f,-3.5f,2,8,1,0,0,0,true);
            box(body,"cloth_r",108,38,-4.1f,.5f,-3.5f,2,8,1,0,0,0,false);
        } else {
            box(lleg,"pouch",100,20,3.5f,.5f,-2.5f,3,6,5,0,0,-.122173f,false);
            for (boolean l:new boolean[]{false,true}) {
                String s=l?"l":"r"; float fx=l?0:-3,bx=l?0:-4;
                hinged(body,"front0_"+s,108,38,0,0,0,3,8,1,fx,11,-2.9f,-.1047198f,l);
                hinged(body,"front1_"+s,108,47,0,7.5f,1.7f,3,3,1,fx,11,-2.9f,-.3316126f,l);
                hinged(body,"back0_"+s,118,16,0,0,0,4,8,1,bx,11.5f,2.9f,.1047198f,!l);
                hinged(body,"back1_"+s,123,9,l?3:0,7.8f,-.9f,1,2,1,bx,11.5f,2.9f,.2268928f,l);
                hinged(body,"back2_"+s,120,12,l?0:1,7.8f,-.9f,3,3,1,bx,11.5f,2.9f,.2268928f,!l);
            }
        }
        for (boolean l:new boolean[]{false,true}) {
            var arm=l?left:right; var leg=l?lleg:rleg; float sign=l?-1:1;
            // The original left shoulder sets mirror after addBox; it has no effect on its UVs.
            box(arm,"shoulder",16,45,l?-1.5f:-3.5f,-2.5f,-2.5f,5,5,5,0,0,0,!l);
            box(arm,"sleeve0",88,39,l?-1.5f:-3.5f,2.5f,-2.5f,5,7,5,0,0,0,l);
            box(arm,"sleeve1",76,32,l?-1:-3,5.5f,2.5f,4,4,2,0,0,0,false);
            box(arm,"sleeve2",88,32,l?-.5f:-2.5f,3.5f,2.5f,3,2,1,0,0,0,false);
            box(arm,"plate_top",56,25,l?3.5f:-5.5f,-2.5f,-3.5f,2,1,7,0,0,sign*.4363323f,false);
            for (int i=0;i<3;i++) {
                box(arm,"plate"+i,i==0?56:40,33,l?3.5f-i:-4.5f+i,i==0?-1.5f:1.5f+2*(i-1),-3.5f,1,i==0?4:3,7,0,0,sign*.4363323f,false);
                box(leg,"front"+i,i==2?82:76,i==1?42:38,l?1:-3,.5f+i*2,-3.5f+i,2,3,1,-.4363323f,0,0,l);
            }
            box(leg,"plate_side",116,25,l?1.5f:-2.5f,.5f,-2.5f,1,4,5,0,0,sign*.4363323f,false);
            box(leg,"cloth0",116,42,l?1.5f:-2.5f,.5f,-2.5f,1,5,5,0,0,sign*.122173f,false);
            box(leg,"cloth1",116,34,l?.5f:-1.5f,5.5f,-2.5f,1,3,5,0,0,sign*.296706f,false);
            box(leg,"cloth2",116,1,l?-1.4f:.4f,8.4f,-2.5f,1,3,5,0,0,sign*.5235988f,false);
        }
        return LayerDefinition.create(mesh,128,64).bakeRoot();
    }
    private static PartDefinition empty(PartDefinition p,String name,float x,float y,float z) { return p.addOrReplaceChild(name,CubeListBuilder.create(),PartPose.offset(x,y,z)); }
    private static void box(PartDefinition p,String name,int u,int v,float x,float y,float z,float w,float h,float d,float rx,float ry,float rz,boolean mirror) {
        p.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).mirror(mirror).addBox(x,y,z,w,h,d),PartPose.rotation(rx,ry,rz));
    }
    private static void hinged(PartDefinition p,String name,int u,int v,float x,float y,float z,float w,float h,float d,float px,float py,float pz,float rx,boolean mirror) {
        p.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).mirror(mirror).addBox(x,y,z,w,h,d),PartPose.offsetAndRotation(px,py,pz,rx,0,0));
    }
}
