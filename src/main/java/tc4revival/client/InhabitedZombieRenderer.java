package tc4revival.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.InhabitedZombieEntity;

public  class InhabitedZombieRenderer extends HumanoidMobRenderer<InhabitedZombieEntity,HumanoidModel<InhabitedZombieEntity>> {
    private static final ResourceLocation SKIN=ResourceLocation.parse("thaumcraft:textures/models/czombie.png");
    public InhabitedZombieRenderer(EntityRendererProvider.Context context) {
        super(context,new HumanoidModel<InhabitedZombieEntity>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE,0),64,64).bakeRoot()) {
            @Override public void setupAnim(InhabitedZombieEntity entity,float walk,float amount,float age,float yaw,float pitch) {
                super.setupAnim(entity,walk,amount,age,yaw,pitch); AnimationUtils.animateZombieArms(leftArm,rightArm,false,attackTime,age);
            }
        },.5f);
        addLayer(new HumanoidArmorLayer<>(this,CultistRenderer.model(.5f),CultistRenderer.model(1),context.getModelManager()));
    }
    @Override public ResourceLocation getTextureLocation(InhabitedZombieEntity entity) { return SKIN; }
}
