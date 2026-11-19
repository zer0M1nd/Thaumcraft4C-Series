package tc4revival.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import tc4revival.content.TaintedAnimalEntity;

/** Body and fleece have different head depths and leg lengths in the old model. */
 class TaintedSheepModel extends EntityModel<TaintedAnimalEntity> {
    private final ModelPart root,head;
    private final ModelPart[] legs=new ModelPart[4];
    private float headAngle;
    TaintedSheepModel(boolean fleece) {
        var mesh=new MeshDefinition();var parts=mesh.getRoot();
        parts.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0)
                .addBox(-3,-4,fleece?-4:-6,6,6,fleece?6:8,new CubeDeformation(fleece?.6f:0)),PartPose.offset(0,6,-8));
        parts.addOrReplaceChild("body",CubeListBuilder.create().texOffs(28,8)
                .addBox(-4,-10,-7,8,16,6,new CubeDeformation(fleece?1.75f:0)),PartPose.offsetAndRotation(0,5,2,Mth.HALF_PI,0,0));
        for(int i=0;i<4;i++)parts.addOrReplaceChild("leg"+i,CubeListBuilder.create().texOffs(0,16)
                .addBox(-2,0,-2,4,fleece?6:12,4,new CubeDeformation(fleece?.5f:0)),PartPose.offset(i%2==0?-3:3,12,i<2?7:-5));
        root=LayerDefinition.create(mesh,64,32).bakeRoot();head=root.getChild("head");
        for(int i=0;i<4;i++)legs[i]=root.getChild("leg"+i);
    }
    @Override public void prepareMobModel(TaintedAnimalEntity entity,float swing,float amount,float partial) {
        head.y=6+9*entity.grazingHeadHeight(partial);headAngle=entity.grazingHeadAngle(partial);
    }
    @Override public void setupAnim(TaintedAnimalEntity entity,float swing,float amount,float age,float yaw,float pitch) {
        head.xRot=headAngle;head.yRot=yaw*Mth.DEG_TO_RAD;
        for(int i=0;i<4;i++)legs[i].xRot=Mth.cos(swing*.6662f+((i==1||i==2)?Mth.PI:0))*1.4f*amount;
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer vertices,int light,int overlay,float red,float green,float blue,float alpha) {
        root.render(pose,vertices,light,overlay,red,green,blue,alpha);
    }
}
