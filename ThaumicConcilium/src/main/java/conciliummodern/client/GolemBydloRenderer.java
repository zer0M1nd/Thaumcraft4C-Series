package conciliummodern.client;

import conciliummodern.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class GolemBydloRenderer extends MobRenderer<GolemBydlo,GolemBydloRenderer.Model> {
    public GolemBydloRenderer(EntityRendererProvider.Context context){super(context,new Model(),1.5f);}
    @Override public ResourceLocation getTextureLocation(GolemBydlo entity){return ResourceLocation.parse("thaumcraft:textures/models/golem_thaumium.png");}
    public static final class Model extends EntityModel<GolemBydlo> {
        private final ModelPart root,head,rightArm,leftArm,rightLeg,leftLeg;private float partial;
        Model(){root=ConciliumBydloGeometry.createBodyLayer().bakeRoot();head=root.getChild("head");rightArm=root.getChild("right_arm");leftArm=root.getChild("left_arm");rightLeg=root.getChild("right_leg");leftLeg=root.getChild("left_leg");}
        @Override public void prepareMobModel(GolemBydlo entity,float limb,float amount,float partial){this.partial=partial;}
        @Override public void setupAnim(GolemBydlo entity,float limb,float amount,float age,float yaw,float pitch){
            head.yRot=yaw*Mth.DEG_TO_RAD;head.xRot=entity.inactive()?1:pitch*Mth.DEG_TO_RAD;
            rightLeg.xRot=-1.5f*wave(limb,13)*amount;leftLeg.xRot=1.5f*wave(limb,13)*amount;
            if(entity.action()>0)rightArm.xRot=leftArm.xRot=-2+1.5f*wave(entity.action()-partial,5);
            else{rightArm.xRot=(-.2f+1.5f*wave(limb,13))*amount;leftArm.xRot=(-.2f-1.5f*wave(limb,13))*amount;}
        }
        private static float wave(float time,float period){return (Math.abs(time%period-period*.5f)-period*.25f)/(period*.25f);}
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,float r,float g,float b,float a){pose.pushPose();pose.scale(1.3f,1.3f,1.4f);root.render(pose,buffer,light,overlay,r,g,b,a);pose.popPose();}
    }
}
