package tc4revival.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import tc4revival.content.CultistEquipment.Style;

/** Original cuboid measurements and UVs rebuilt as modern part definitions. No legacy rendering code is used. */
 class CultistArmorGeometry {
    private final MeshDefinition mesh=new MeshDefinition();
    private final PartDefinition root=mesh.getRoot();
    private CultistArmorGeometry() {
        for (String part:new String[]{"head","hat","body"}) root.addOrReplaceChild(part,CubeListBuilder.create(),PartPose.ZERO);
        root.addOrReplaceChild("right_arm",CubeListBuilder.create(),PartPose.offset(-5,2,0));
        root.addOrReplaceChild("left_arm",CubeListBuilder.create(),PartPose.offset(5,2,0));
        root.addOrReplaceChild("right_leg",CubeListBuilder.create(),PartPose.offset(-1.9f,12,0));
        root.addOrReplaceChild("left_leg",CubeListBuilder.create(),PartPose.offset(1.9f,12,0));
    }
    static ModelPart bake(Style style,boolean chest) {
        var geometry=new CultistArmorGeometry();
        if (style==Style.ROBE) geometry.robe(chest); else geometry.plate(chest,style==Style.LEADER);
        return LayerDefinition.create(geometry.mesh,128,64).bakeRoot();
    }
    private void box(String parent,String name,int u,int v,float x,float y,float z,float w,float h,float d,
                     float px,float py,float pz,float rx,float ry,float rz,boolean mirror) {
        var cube=CubeListBuilder.create().texOffs(u,v).mirror(mirror).addBox(x,y,z,w,h,d);
        root.getChild(parent).addOrReplaceChild(name,cube,PartPose.offsetAndRotation(px,py,pz,rx,ry,rz));
    }
    private void box(String parent,String name,int u,int v,float x,float y,float z,float w,float h,float d) {
        box(parent,name,u,v,x,y,z,w,h,d,0,0,0,0,0,0,false);
    }
    private void robe(boolean chest) {
        box("head","hood1",16,7,-4.5f,-9,-4.6f,9,9,9);
        box("head","hood2",52,13,-4,-9.7f,2,8,9,3,0,0,0,-.2268928f,0,0,false);
        box("head","hood3",52,14,-3.5f,-10,3.5f,7,8,3,0,0,0,-.3490659f,0,0,false);
        box("head","hood4",53,15,-3,-10.7f,3.5f,6,7,3,0,0,0,-.5759587f,0,0,false);
        box("body","belt",16,55,-4,7,-3,8,5,1);
        box("body","belt_back",16,55,-4,7,-4,8,5,1,0,0,0,0,3.141593f,0,false);
        box("body","belt_left",16,36,4,8,-3,1,3,6); box("body","belt_right",16,36,-5,8,-3,1,3,6);
        if (chest) {
            box("body","chest",16,25,-4,1,-3,8,6,1); box("body","ornament",56,50,-2.5f,1,-4,5,7,1);
            box("body","scroll",78,25,-2,9.5f,4,8,3,3,0,0,0,0,0,.1919862f,false);
            box("body","back",36,45,-4,1,1.9f,8,11,2);
            box("body","book",81,16,1,0,4,5,7,2,0,0,0,0,0,.7679449f,false);
            box("body","cloth_left",108,38,2.1f,.5f,-3.5f,2,8,1,0,0,0,0,0,0,true);
            box("body","cloth_right",108,38,-4.1f,.5f,-3.5f,2,8,1);
        } else {
            box("left_leg","pouch",100,20,3.5f,.5f,-2.5f,3,6,5,0,0,0,0,0,-.122173f,false);
            for (boolean left:new boolean[]{false,true}) {
                String side=left ? "left" : "right"; float frontX=left ? 0 : -3,backX=left ? 0 : -4;
                box("body","front1_"+side,108,38,0,0,0,3,8,1,frontX,11,-2.9f,-.1047198f,0,0,left);
                box("body","front2_"+side,108,47,0,7.5f,1.7f,3,3,1,frontX,11,-2.9f,-.3316126f,0,0,left);
                box("body","back1_"+side,118,16,0,0,0,4,8,1,backX,11.5f,2.9f,.1047198f,0,0,!left);
                box("body","back2_"+side,123,9,left ? 3 : 0,7.8f,-.9f,1,2,1,backX,11.5f,2.9f,.2268928f,0,0,left);
                box("body","back3_"+side,120,12,left ? 0 : 1,7.8f,-.9f,3,3,1,backX,11.5f,2.9f,.2268928f,0,0,!left);
            }
        }
        for (boolean left:new boolean[]{false,true}) {
            String arm=left ? "left_arm" : "right_arm",leg=left ? "left_leg" : "right_leg"; float sign=left ? -1 : 1;
            box(arm,"shoulder",16,45,left ? -1.5f : -3.5f,-2.5f,-2.5f,5,5,5,0,0,0,0,0,0,!left);
            box(arm,"sleeve",88,39,left ? -1.5f : -3.5f,2.5f,-2.5f,5,7,5,0,0,0,0,0,0,left);
            box(arm,"arm2",76,32,left ? -1 : -3,5.5f,2.5f,4,4,2);
            box(arm,"arm3",88,32,left ? -.5f : -2.5f,3.5f,2.5f,3,2,1);
            box(arm,"shoulder_top",56,25,left ? 3.5f : -5.5f,-2.5f,-3.5f,2,1,7,0,0,0,0,0,sign*.4363323f,false);
            box(arm,"shoulder1",56,33,left ? 3.5f : -4.5f,-1.5f,-3.5f,1,4,7,0,0,0,0,0,sign*.4363323f,false);
            box(arm,"shoulder2",40,33,left ? 2.5f : -3.5f,1.5f,-3.5f,1,3,7,0,0,0,0,0,sign*.4363323f,false);
            box(arm,"shoulder3",40,33,left ? 1.5f : -2.5f,3.5f,-3.5f,1,3,7,0,0,0,0,0,sign*.4363323f,false);
            box(leg,"leg4",76,38,left ? 1 : -3,.5f,-3.5f,2,3,1,0,0,0,-.4363323f,0,0,left);
            box(leg,"leg5",76,42,left ? 1 : -3,2.5f,-2.5f,2,3,1,0,0,0,-.4363323f,0,0,left);
            box(leg,"leg6",82,38,left ? 1 : -3,4.5f,-1.5f,2,3,1,0,0,0,-.4363323f,0,0,left);
            box(leg,"panel",116,25,left ? 1.5f : -2.5f,.5f,-2.5f,1,4,5,0,0,0,0,0,sign*.4363323f,false);
            box(leg,"side1",116,42,left ? 1.5f : -2.5f,.5f,-2.5f,1,5,5,0,0,0,0,0,sign*.122173f,false);
            box(leg,"side2",116,34,left ? .5f : -1.5f,5.5f,-2.5f,1,3,5,0,0,0,0,0,sign*.296706f,false);
            box(leg,"side3",116,1,left ? -1.4f : .4f,8.4f,-2.5f,1,3,5,0,0,0,0,0,sign*.5235988f,false);
        }
    }
    private void plate(boolean chest,boolean leader) {
        box("head","helmet",41,8,-4.5f,-9,-4.5f,9,9,9);
        box("body","belt",56,55,-4,8,-3,8,4,1);
        box("body","belt_left",76,44,4,8,-3,1,3,6); box("body","belt_right",76,44,-5,8,-3,1,3,6);
        if (chest) {
            box("body","chest",56,45,-4,1,leader ? -3.8f : -3,8,7,leader ? 2 : 1);
            box("body","back",36,45,-4,1,2,8,11,2);
            if (leader) {
                box("body","upper_belt_left",76,44,4,4,-3,1,3,6); box("body","upper_belt_right",76,44,-5,4,-3,1,3,6);
                box("body","ornament",76,53,-2.5f,3,-4.8f,5,5,1);
                box("body","collar_back",17,26,-4.5f,-1.5f,7,9,4,1,0,0,-2.5f,.2268928f,0,0,false);
                box("body","collar_right",17,11,-5.5f,-1.5f,-3,1,4,11,0,0,-2.5f,.2268928f,0,0,false);
                box("body","collar_left",17,11,4.5f,-1.5f,-3,1,4,11,0,0,-2.5f,.2268928f,0,0,false);
                box("body","collar_front",17,31,-4.5f,-1.5f,-3,9,4,1,0,0,-2.5f,.2268928f,0,0,false);
                for (boolean left:new boolean[]{false,true}) {
                    String side=left ? "left" : "right";
                    box("body","chest_cloth_"+side,20,47,left ? 1.5f : -4.5f,1.2f,-4.5f,3,9,1,0,0,0,.0663225f,0,0,left);
                    box("body","leg_cloth_"+side,20,55,0,0,0,3,8,1,left ? 1.5f : -4.5f,10.4f,-3.9f,-.0349066f,0,0,left);
                }
                box("body","cloak1",0,47,-4.5f,2,1,9,12,1,0,0,3,.1396263f,0,0,false);
                box("body","cloak2",0,59,-4.5f,14,-1.3f,9,4,1,0,0,3,.3069452f,0,0,false);
                box("body","cloak3",0,59,-4.5f,17,-3.7f,9,4,1,0,0,3,.4465716f,0,0,false);
                box("body","cloak_left",0,43,2.5f,1,-1,2,1,3,0,0,3,.1396263f,0,0,false);
                box("body","cloak_right",0,43,-4.5f,1,-1,2,1,3,0,0,3,.1396263f,0,0,false);
            } else {
                box("body","tabard",114,52,-3,1.2f,-3.5f,6,10,1);
                box("body","front1",120,39,0,0,0,6,8,1,-3,11,-3.5f,-.1047198f,0,0,false);
                box("body","front2",100,37,0,7.5f,1.8f,6,3,1,-3,11,-3.5f,-.3316126f,0,0,false);
                box("body","cloak1",0,47,0,0,0,9,12,1,-4.5f,1.3f,4.2f,.1396263f,0,0,false);
                box("body","cloak2",0,59,0,11.7f,-2,9,4,1,-4.5f,1.3f,4.2f,.3069452f,0,0,false);
                box("body","cloak3",0,59,0,15.2f,-4.2f,9,4,1,-4.5f,1.3f,4.2f,.4465716f,0,0,false);
                box("body","cloak_left",0,43,2.5f,1,2,2,1,3,0,0,0,.1396263f,0,0,false);
                box("body","cloak_right",0,43,-4.5f,1,2,2,1,3,0,0,0,.1396263f,0,0,false);
            }
        }
        for (boolean left:new boolean[]{false,true}) {
            String arm=left ? "left_arm" : "right_arm",leg=left ? "left_leg" : "right_leg"; float sign=left ? -1 : 1;
            box(arm,"shoulder",56,35,left ? -1.5f : -3.5f,-2.5f,-2.5f,5,5,5);
            box(arm,"shoulder1",0,0,left ? 1.3f : -4.3f,-1.5f,-3,3,5,6,0,0,0,0,0,sign*.7853982f,false);
            box(arm,"shoulder2",0,19,left ? 2.3f : -3.3f,3.5f,-2.5f,1,1,5,0,0,0,0,0,sign*.7853982f,left);
            box(arm,"shoulder3",0,11,left ? 1.3f : -2.3f,3.5f,-3,1,2,6,0,0,0,0,0,sign*.7853982f,false);
            if (leader) {
                for (int z:new int[]{-4,3}) box(arm,"shoulder_edge_"+z,18,4,left ? 1.3f : -2.3f,-1.5f,z,1,6,1,0,0,0,0,0,sign*.7853982f,false);
            }
            box(arm,"gauntlet",left ? 114 : 100,26,left ? 1.5f : -3.5f,3.5f,-2.5f,2,6,5);
            if (leader) box(arm,"gauntlet_ridge",102,37,left ? 4 : -5,3.5f,-2,1,5,4,0,0,0,0,0,-sign*.1675516f,false);
            for (int band=0;band<2;band++) box(arm,"strap"+band,84,31,-1.5f,3.5f+band*3,-2.5f,3,1,5,0,0,0,0,0,0,left);
            if (leader) {
                box(leg,"back",0,25,left ? -2 : -3,-.5f,2.5f,5,7,1,0,0,0,.0698132f,0,0,false);
                box(leg,"top",96,14,left ? -2 : -3,-.5f,-2.5f,5,3,5,0,0,0,0,0,sign*.1396263f,false);
                box(leg,"side",116,13,left ? 2 : -3,2.5f,-2.5f,1,4,5,0,0,0,0,0,sign*.1396263f,false);
                box(leg,"front",0,25,left ? -2 : -3,-.5f,-3.5f,5,7,1,0,0,0,-.0349066f,0,0,!left);
            } else {
                box(leg,"panel0",96,14,left ? -2 : -3,-.5f,-2.5f,5,3,5,0,0,0,0,0,sign*.1396263f,false);
                box(leg,"panel1",96,7,left ? -2 : 0,2.5f,-2.5f,2,2,5,0,0,0,0,0,sign*.1396263f,!left);
                box(leg,"panel2",114,5,left ? 0 : -2,2.5f,-2.5f,2,3,5,0,0,0,0,0,sign*.1396263f,!left);
                box(leg,"panel3",116,13,left ? 2 : -3,2.5f,-2.5f,1,4,5,0,0,0,0,0,sign*.1396263f,false);
            }
        }
    }
}
