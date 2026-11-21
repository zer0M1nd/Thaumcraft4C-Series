package thmodern.client;

import java.util.List;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.horse.Horse;

/** The pre-1.13 layout is required by TH's 128x128 skins, including all armour variants. */
public class LegacySteedModel extends AgeableListModel<Horse> {
    private final ModelPart root,body,neck,head,muzzle,jaw,tail,tailTip,saddle,bridle,reins,leftStirrup,rightStirrup;
    private final ModelPart frontLeft,frontRight,backLeft,backRight;
    private float partial,eating;
    public LegacySteedModel(){
        root=layer().bakeRoot();body=root.getChild("body");neck=root.getChild("neck");head=root.getChild("head");
        muzzle=head.getChild("muzzle");jaw=head.getChild("jaw");bridle=head.getChild("bridle");
        reins=root.getChild("reins");tail=root.getChild("tail");tailTip=tail.getChild("tip");
        saddle=root.getChild("saddle");leftStirrup=root.getChild("left_stirrup");rightStirrup=root.getChild("right_stirrup");
        frontLeft=root.getChild("front_left");frontRight=root.getChild("front_right");backLeft=root.getChild("back_left");backRight=root.getChild("back_right");
    }
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("body",box(0,34,-5,-8,-19,10,10,24),PartPose.offset(0,11,9));
        root.addOrReplaceChild("neck",box(0,12,-2.05f,-9.8f,-2,4,14,8)
                .texOffs(58,0).addBox(-1,-11.5f,5,2,16,4),PartPose.offsetAndRotation(0,4,-10,Mth.PI/6,0,0));
        var head=root.addOrReplaceChild("head",box(0,0,-2.5f,-10,-1.5f,5,5,7)
                .addBox(.45f,-12,4,2,3,1).addBox(-2.45f,-12,4,2,3,1),PartPose.offsetAndRotation(0,4,-10,Mth.PI/6,0,0));
        head.addOrReplaceChild("muzzle",box(24,18,-2,-10,-7,4,3,6),PartPose.offset(0,.02f,.02f));
        head.addOrReplaceChild("jaw",box(24,27,-2,-7,-6.5f,4,2,5),PartPose.ZERO);
        head.addOrReplaceChild("bridle",CubeListBuilder.create().texOffs(80,12).addBox(-2.5f,-10.1f,-7,5,5,12,new CubeDeformation(.2f))
                .texOffs(74,13).addBox(1.5f,-8,-4,1,2,2).addBox(-2.5f,-8,-4,1,2,2),PartPose.ZERO);
        root.addOrReplaceChild("reins",box(44,10,2.6f,-6,-6,0,3,16).texOffs(44,5).addBox(-2.6f,-6,-6,0,3,16),PartPose.offset(0,4,-10));
        var tail=root.addOrReplaceChild("tail",box(44,0,-1,-1,0,2,2,3).texOffs(38,7).addBox(-1.5f,-2,3,3,4,7),PartPose.offsetAndRotation(0,3,14,-1.134464f,0,0));
        tail.addOrReplaceChild("tip",box(24,3,-1.5f,-4.5f,9,3,4,7),PartPose.rotation(-.2618f,0,0));
        leg(root,"front_left",true,true,44);leg(root,"front_right",true,false,60);
        leg(root,"back_left",false,true,78);leg(root,"back_right",false,false,96);
        root.addOrReplaceChild("saddle",box(80,0,-5,0,-3,10,1,8).texOffs(106,9).addBox(-1.5f,-1,-3,3,1,2)
                .texOffs(80,9).addBox(-4,-1,3,8,1,2),PartPose.offset(0,2,2));
        root.addOrReplaceChild("left_stirrup",box(70,0,-.5f,0,-.5f,1,6,1).texOffs(74,0).addBox(-.5f,6,-1,1,2,2),PartPose.offset(5,3,2));
        root.addOrReplaceChild("right_stirrup",box(80,0,-.5f,0,-.5f,1,6,1).texOffs(74,4).addBox(-.5f,6,-1,1,2,2),PartPose.offset(-5,3,2));
        return LayerDefinition.create(mesh,128,128);
    }
    private static CubeListBuilder box(int u,int v,float x,float y,float z,float w,float h,float d){return CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,w,h,d);}
    private static void leg(PartDefinition root,String name,boolean front,boolean left,int u){
        float x=front?(left?-1.9f:-1.1f):(left?-2.5f:-1.5f);
        var thigh=root.addOrReplaceChild(name,box(u,29,x,front?-1:-2,front?-2.1f:-2.5f,front?3:4,front?8:9,front?4:5),PartPose.offset(left?4:-4,9,front?-8:11));
        float shinX=front?x:x+.5f;
        thigh.addOrReplaceChild("shin",box(u,front?41:43,shinX,0,front?-1.6f:-1.5f,3,5,3)
                .texOffs(u,51).addBox(front?x-.5f:x,5.1f,front?-2.1f:-2,4,3,4),PartPose.offset(0,7,0));
    }
    @Override protected Iterable<ModelPart> headParts(){return List.of(head,reins);}
    @Override protected Iterable<ModelPart> bodyParts(){return List.of(body,neck,tail,frontLeft,frontRight,backLeft,backRight,saddle,leftStirrup,rightStirrup);}
    @Override public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,com.mojang.blaze3d.vertex.VertexConsumer out,int light,int overlay,float r,float g,float b,float a){
        if(!young){root.render(pose,out,light,overlay,r,g,b,a);return;}
        // Vanilla's generic baby model leaves an adult-sized neck above half-sized legs.
        // The legacy horse renders legs, torso/neck, and head in three separate proportions.
        pose.pushPose();pose.scale(.5f,.75f,.5f);pose.translate(0,.475,0);
        for(var leg:List.of(frontLeft,frontRight,backLeft,backRight))leg.render(pose,out,light,overlay,r,g,b,a);pose.popPose();
        pose.pushPose();pose.scale(.5f,.5f,.5f);pose.translate(0,.675,0);
        for(var part:List.of(body,neck,tail))part.render(pose,out,light,overlay,r,g,b,a);pose.popPose();
        pose.pushPose();pose.scale(.625f,.625f,.625f);pose.translate(0,.45*eating+.675*(1-eating),.075*eating);
        head.render(pose,out,light,overlay,r,g,b,a);pose.popPose();
    }
    @Override public void prepareMobModel(Horse horse,float swing,float amount,float partial){super.prepareMobModel(horse,swing,amount,partial);this.partial=partial;}
    @Override public void setupAnim(Horse horse,float swing,float amount,float age,float yaw,float pitch){
        root.getAllParts().forEach(ModelPart::resetPose);
        float stand=horse.getStandAnim(partial),eat=horse.getEatAnim(partial),rest=1-stand,normal=1-Math.max(stand,eat);
        eating=eat;
        float look=pitch*Mth.DEG_TO_RAD+(amount>.2f?Mth.cos(swing*.4f)*.15f*amount:0);
        float stride=Mth.cos(swing*.6662f+Mth.PI),walk=stride*.8f*amount,rear=Mth.cos(age*.6f+Mth.PI),mouth=horse.getMouthAnim(partial);
        body.xRot=-stand*Mth.PI/4;
        neck.xRot=stand*(Mth.PI/12+look)+eat*2.18166f+normal*(Mth.PI/6+look);
        neck.yRot=(stand+normal)*Mth.clamp(yaw,-20,20)*Mth.DEG_TO_RAD;
        neck.y=-6*stand+11*eat+4*normal;neck.z=-stand-10*eat-10*normal;
        head.copyFrom(neck);
        muzzle.z=.02f-mouth;muzzle.xRot=-.09424778f*mouth;jaw.z=mouth;jaw.xRot=.15707964f*mouth;
        frontLeft.y=frontRight.y=-2*stand+9*rest;frontLeft.z=frontRight.z=-2*stand-8*rest;
        float fl=(-Mth.PI/3+rear)*stand+walk*rest,fr=(-Mth.PI/3-rear)*stand-walk*rest;
        limb(frontLeft,fl,(fl+Mth.PI*Math.max(0,.2f+rear*.2f))*stand+(walk+Math.max(0,stride*.5f*amount))*rest);
        limb(frontRight,fr,(fr+Mth.PI*Math.max(0,.2f-rear*.2f))*stand+(-walk+Math.max(0,-stride*.5f*amount))*rest);
        limb(backLeft,Mth.PI/12*stand-stride*.5f*amount*rest,-.08726646f*stand+(-stride*.5f*amount-Math.max(0,stride*.5f*amount))*rest);
        limb(backRight,Mth.PI/12*stand+stride*.5f*amount*rest,-.08726646f*stand+(stride*.5f*amount-Math.max(0,-stride*.5f*amount))*rest);
        tail.y=9*stand+3*rest;tail.z=14;tail.xRot=horse.tailCounter>0?0:Math.min(0,-1.3089f+amount*1.5f);tail.yRot=horse.tailCounter>0?Mth.cos(age*.7f):0;
        tailTip.xRot=-.2618f;
        saddle.visible=bridle.visible=leftStirrup.visible=rightStirrup.visible=horse.isSaddled()&&!horse.isBaby();
        reins.visible=saddle.visible&&horse.isVehicle();reins.y=neck.y;reins.z=neck.z;reins.xRot=look;reins.yRot=neck.yRot;
        saddle.y=.5f*stand+2*rest;saddle.z=11*stand+2*rest;saddle.xRot=body.xRot;
        for(var stirrup:List.of(leftStirrup,rightStirrup)){stirrup.y=saddle.y;stirrup.z=saddle.z;stirrup.xRot=horse.isVehicle()?-Mth.PI/3:walk/3;stirrup.zRot=horse.isVehicle()?0:(stirrup==leftStirrup?walk:-walk)/5;}
    }
    private static void limb(ModelPart thigh,float upper,float lower){thigh.xRot=upper;thigh.getChild("shin").xRot=lower-upper;}
}
