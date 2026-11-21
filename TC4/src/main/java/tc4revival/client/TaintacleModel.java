package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import tc4revival.content.TaintacleEntity;

/** The taper scales the next joint's translation as well as its cube, as in the old recursive renderer. */
public  class TaintacleModel<T extends TaintacleEntity> extends EntityModel<T> {
    private final ModelPart base,segment,orb,bulb;
    private final float[] pitch,roll;
    private float height,buried;
    public TaintacleModel(int length) {
        pitch=new float[length-1]; roll=new float[length-1]; var mesh=new MeshDefinition(); var root=mesh.getRoot();
        root.addOrReplaceChild("base",CubeListBuilder.create().texOffs(0,0).addBox(-4,-4,-4,8,8,8),PartPose.ZERO);
        root.addOrReplaceChild("segment",CubeListBuilder.create().texOffs(0,16).addBox(-4,-4,-4,8,8,8),PartPose.ZERO);
        root.addOrReplaceChild("orb",CubeListBuilder.create().texOffs(0,56).addBox(-2,-2,-2,4,4,4),PartPose.ZERO);
        root.addOrReplaceChild("bulb",CubeListBuilder.create().texOffs(0,32).addBox(-6,-6,-6,12,12,12),PartPose.ZERO);
        var model=LayerDefinition.create(mesh,64,64).bakeRoot(); base=model.getChild("base"); segment=model.getChild("segment"); orb=model.getChild("orb"); bulb=model.getChild("bulb");
    }
    @Override public void setupAnim(T entity,float walk,float amount,float age,float yaw,float headPitch) {
        height=entity.getBbHeight(); float duration=height*10;
        buried=entity.tickCount<duration ? (duration-entity.tickCount)/duration*height : 0;
        float mod=.0625f*.2f,speed=entity.agitated() ? 3 : 1-mod;
        float intensity=entity.flailIntensity()+(entity.hurtTime>0 || entity.attackCooldown()>0 ? mod : -mod);
        for (int joint=0;joint<pitch.length;joint++) {
            pitch[joint]=.15f*intensity*Mth.sin(age*.1f*speed-joint/2f);
            roll[joint]=.1f/intensity*Mth.sin(age*.15f-joint/2f);
        }
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,float r,float g,float b,float a) {
        pose.pushPose(); pose.translate(0,(height==3 ? .6 : 1.2)+buried,0); pose.scale(height/3,height/3,height/3); pose.translate(0,.75,0);
        base.render(pose,out,light,overlay,r,g,b,a);
        for (int joint=0;joint<pitch.length;joint++) {
            pose.scale(.88f,.88f,.88f); pose.translate(0,-.5,0);
            pose.mulPose(Axis.ZP.rotation(roll[joint])); pose.mulPose(Axis.XP.rotation(pitch[joint])); segment.render(pose,out,light,overlay,r,g,b,a);
        }
        pose.scale(.88f,.88f,.88f); pose.translate(0,-.5,0);
        orb.render(pose,out,LightTexture.FULL_BRIGHT,overlay,r,g,b,a); bulb.render(pose,out,LightTexture.FULL_BRIGHT,overlay,r,g,b,a);
        pose.popPose();
    }
}
