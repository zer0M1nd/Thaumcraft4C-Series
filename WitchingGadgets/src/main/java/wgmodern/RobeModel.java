package wgmodern;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.*;

/** Robe hood, trailing sleeves and skirt pieces retain the original 128x64 UV geometry. */
public class RobeModel extends HumanoidModel<LivingEntity> {
    private static RobeModel cached;
    public RobeModel(){super(layer().bakeRoot());}
    public static HumanoidModel<?> prepare(LivingEntity entity,HumanoidModel<?> original){if(cached==null)cached=new RobeModel();var model=cached;model.head.copyFrom(original.head);model.hat.copyFrom(original.hat);model.body.copyFrom(original.body);model.rightArm.copyFrom(original.rightArm);model.leftArm.copyFrom(original.leftArm);model.rightLeg.copyFrom(original.rightLeg);model.leftLeg.copyFrom(original.leftLeg);model.young=original.young;model.crouching=original.crouching;model.riding=original.riding;model.hat.visible=false;var helmet=entity.getItemBySlot(EquipmentSlot.HEAD);String name=helmet.getDescriptionId().toLowerCase(java.util.Locale.ROOT);model.head.visible=helmet.isEmpty()||name.contains("goggles")||name.contains("glasses");return model;}
    private static void detail(PartDefinition parent,String id,int u,int v,float x,float y,float z,float sx,float sy,float sz,float grow,float rx,float rz){parent.addOrReplaceChild(id,CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,sx,sy,sz,new CubeDeformation(grow)),PartPose.rotation(rx,0,rz));}
    public static LayerDefinition layer(){var mesh=HumanoidModel.createMesh(new CubeDeformation(.5f),0);var root=mesh.getRoot();var right=root.addOrReplaceChild("right_arm",CubeListBuilder.create().texOffs(40,16).addBox(-3,-2,-2,4,11,4,new CubeDeformation(.5f)),PartPose.offset(-5,2,0));var left=root.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(40,16).mirror().addBox(-1,-2,-2,4,11,4,new CubeDeformation(.5f)),PartPose.offset(5,2,0));
        detail(right,"sleeve1",40,32,-2.5f,4.2f,2,3,5,1,.25f,0,0);detail(right,"sleeve2",48,32,-2,6.4f,2,2,3,1,.25f,.125f,0);detail(left,"sleeve1",40,32,-.5f,4.2f,2,3,5,1,.25f,0,0);detail(left,"sleeve2",48,32,0,6.4f,2,2,3,1,.25f,.125f,0);
        var head=root.getChild("head");detail(head,"hood1",32,0,-4,-8,-4,8,8,7,1,0,0);detail(head,"hood2",64,0,-4,-7.8f,3,8,6,2,.5f,-.0875f,0);detail(head,"hood3",64,8,-3,-7.5f,5,6,6,1,.25f,-.125f,0);
        var rleg=root.getChild("right_leg");var lleg=root.getChild("left_leg");var body=root.getChild("body");detail(rleg,"skirt1",4,32,-.5f,0,1.4f,2,8,1,.25f,.125f,0);detail(rleg,"skirt2",0,32,-2,0,1.4f,1,7,1,.25f,.125f,0);detail(lleg,"skirt1",4,32,-1.5f,0,1.4f,2,8,1,.25f,.125f,0);detail(lleg,"skirt2",0,32,1,0,1.4f,1,7,1,.25f,.125f,0);
        detail(body,"side1",10,32,-2,12.5f,-2,1,6,4,.25f,0,.1875f);detail(body,"side2",10,32,-4.3f,11,-2,1,1,4,.25f,0,0);detail(body,"side3",10,32,1,12.5f,-2,1,6,4,.25f,0,-.1875f);detail(body,"side4",10,32,3.3f,11,-2,1,1,4,.25f,0,0);detail(body,"back",64,8,-3,4,1,6,8,1,.25f,0,0);
        return LayerDefinition.create(mesh,128,64);
    }
}
