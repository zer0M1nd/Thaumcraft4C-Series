package tc4revival.client;

import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;

/** Original numeric mesh data rebuilt as modern, separately selectable model parts. */
 class FortressArmorGeometry {
    private FortressArmorGeometry() {}
    static ModelPart bake(boolean chest) {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
        var head=root.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.ZERO);
        root.addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO);
        var body=root.addOrReplaceChild("body",CubeListBuilder.create(),PartPose.ZERO);
        var right=root.addOrReplaceChild("right_arm",CubeListBuilder.create(),PartPose.offset(-5,2,0));
        var left=root.addOrReplaceChild("left_arm",CubeListBuilder.create(),PartPose.offset(5,2,0));
        var rleg=root.addOrReplaceChild("right_leg",CubeListBuilder.create(),PartPose.offset(-1.9f,12,0));
        var lleg=root.addOrReplaceChild("left_leg",CubeListBuilder.create(),PartPose.offset(1.9f,12,0));
        for (int i=0;i<3;i++) box(head,"mask"+i,52+i*24,2,-4.5f,-5,-4.6f,9,5,1,0,0,0,false);
        box(head,"goggles",100,18,-4.5f,-5,-4.25f,9,5,1,0,0,0,false);
        box(head,"helmet",41,8,-4.5f,-9,-4.5f,9,4,9,0,0,0,false);
        box(head,"back",41,21,-4.5f,-3,5.5f,9,5,1,.5235988f,0,0,false);
        box(head,"cap",21,0,-4.5f,-6,-6.5f,9,1,2,0,0,0,false);
        box(head,"gem_frame",68,11,-1.5f,-9,-7,3,3,2,-.1396263f,0,0,false);
        box(head,"gem",72,8,-1,-8.5f,-7.5f,2,2,1,-.1396263f,0,0,false);
        for (boolean mirrored:new boolean[]{false,true}) {
            String side=mirrored?"l":"r"; float sign=mirrored?-1:1;
            box(head,"ornament1_"+side,78,8,mirrored?1.5f:-3.5f,-9,-6.5f,2,2,1,-.1396263f,0,0,mirrored);
            box(head,"ornament2_"+side,78,8,mirrored?3.5f:-4.5f,-10,-6.5f,1,2,1,-.1396263f,0,0,mirrored);
            box(head,"side_"+side,21,13,mirrored?5.5f:-6.5f,-3,-4.5f,1,5,9,0,0,sign*.5235988f,mirrored);
            box(head,"flap_"+side,59,10,mirrored?7:-10,-2,-1,3,3,1,0,-sign*.5235988f,sign*.5235988f,mirrored);
            var arm=mirrored?left:right;
            box(arm,"shoulder",56,35,mirrored?-1.5f:-3.5f,-2.5f,-2.5f,5,5,5,0,0,0,mirrored);
            box(arm,"gauntlet",mirrored?114:100,26,mirrored?1.5f:-3.5f,3.5f,-2.5f,2,6,5,0,0,0,false);
            for (int i=0;i<2;i++) box(arm,"strap"+i,84,31,-1.5f,3.5f+3*i,-2.5f,3,1,5,0,0,0,mirrored);
            box(arm,"plate_top",110,37,mirrored?3.5f:-5.5f,-2.5f,-3.5f,2,1,7,0,0,sign*.4363323f,mirrored);
            for (int i=0;i<3;i++) box(arm,"plate"+i,i==0?110:94,45,mirrored?3.5f-i:-4.5f+i,i==0?-1.5f:1.5f+(i-1)*2,-3.5f,1,i==0?4:3,7,0,0,sign*.4363323f,mirrored);
            var leg=mirrored?lleg:rleg;
            for (int i=0;i<3;i++) {
                box(leg,"front_inner"+i,i==1?8:0,i==2?56:51,mirrored?-2:-1,.5f+3*i,-3.5f+i,3,i==2?3:4,1,-.4363323f,0,0,mirrored);
                box(leg,"front_outer"+i,i==2?6:0,i==1?47:43,mirrored?1:-3,.5f+2*i,-3.5f+i,2,3,1,-.4363323f,0,0,mirrored);
                box(leg,"side"+i,i==2?12:0,i==0?22:31,mirrored?1.5f-i:-2.5f+i,i==0?.5f:3.5f+(i-1)*2,-2.5f,1,i==0?4:3,5,0,0,sign*.4363323f,mirrored);
                box(leg,"back"+i,0,18,mirrored?-2:-3,.5f+2*i,2.5f-i,5,3,1,.4363323f,0,0,mirrored);
            }
        }
        for (int x:new int[]{-5,4}) box(body,"belt"+x,76,44,x,chest?4:8,-3,1,3,6,0,0,0,false);
        if (chest) {
            box(body,"chest",56,45,-4,1,-4,8,7,2,0,0,0,false);
            box(body,"scroll",34,27,-2,9.5f,4,8,3,3,0,0,.1919862f,false);
            box(body,"back",36,45,-4,1,2,8,11,2,0,0,0,false);
            box(body,"book",100,8,1,-.3f,4,5,7,2,0,0,.7679449f,false);
        } else box(body,"belt_front",56,55,-4,8,-3,8,4,1,0,0,0,false);
        return LayerDefinition.create(mesh,128,64).bakeRoot();
    }
    private static void box(PartDefinition parent,String name,int u,int v,float x,float y,float z,float w,float h,float d,float rx,float ry,float rz,boolean mirror) {
        parent.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).mirror(mirror).addBox(x,y,z,w,h,d),PartPose.rotation(rx,ry,rz));
    }
}
