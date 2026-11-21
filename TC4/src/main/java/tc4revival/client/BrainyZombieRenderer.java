package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.*;

public  class BrainyZombieRenderer<T extends BrainyZombieEntity> extends HumanoidMobRenderer<T,HumanoidModel<T>> {
    public BrainyZombieRenderer(EntityRendererProvider.Context context){
        super(context,new HumanoidModel<T>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE,0),64,64).bakeRoot()){
            @Override public void setupAnim(T entity,float walk,float amount,float age,float yaw,float pitch){super.setupAnim(entity,walk,amount,age,yaw,pitch);AnimationUtils.animateZombieArms(leftArm,rightArm,false,attackTime,age);}
        },.5f);
        addLayer(new HumanoidArmorLayer<>(this,CultistRenderer.model(.5f),CultistRenderer.model(1),context.getModelManager()));
    }
    private HumanoidModel<T> normal,villagerModel;
    @Override public void render(T entity,float yaw,float partial,PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
        if(normal==null){normal=model;var mesh=HumanoidModel.createMesh(CubeDeformation.NONE,0);var root=mesh.getRoot();
            var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-10,-4,8,10,8),net.minecraft.client.model.geom.PartPose.ZERO);
            head.addOrReplaceChild("nose",CubeListBuilder.create().texOffs(24,0).addBox(-1,-1,-6,2,4,2),net.minecraft.client.model.geom.PartPose.offset(0,-2,0));
            villagerModel=new HumanoidModel<T>(LayerDefinition.create(mesh,64,64).bakeRoot()){
                @Override public void setupAnim(T zombie,float walk,float amount,float age,float headYaw,float pitch){super.setupAnim(zombie,walk,amount,age,headYaw,pitch);AnimationUtils.animateZombieArms(leftArm,rightArm,false,attackTime,age);}
            };
        }model=entity.villager()?villagerModel:normal;super.render(entity,yaw,partial,pose,buffers,light);
    }
    @Override protected boolean isShaking(T entity){return entity.curing()||super.isShaking(entity);}
    @Override protected void scale(T entity,PoseStack pose,float partial){if(entity instanceof GiantBrainyZombieEntity giant)pose.scale(giant.anger(),giant.anger(),giant.anger());}
    @Override public ResourceLocation getTextureLocation(T entity){return ResourceLocation.parse(entity.villager()?"thaumcraft:textures/models/bzombievil.png":"thaumcraft:textures/models/bzombie.png");}
}
