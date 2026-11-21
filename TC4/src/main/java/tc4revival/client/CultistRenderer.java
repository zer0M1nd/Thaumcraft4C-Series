package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Monster;
import tc4revival.content.CultistClericEntity;
import tc4revival.content.CultistLeaderEntity;

public  class CultistRenderer<T extends Monster> extends HumanoidMobRenderer<T,HumanoidModel<T>> {
    private static final ResourceLocation SKIN=ResourceLocation.parse("thaumcraft:textures/models/cultist.png");
    public CultistRenderer(EntityRendererProvider.Context context) {
        super(context,model(0),.5f);
        addLayer(new HumanoidArmorLayer<>(this,model(.5f),model(1),context.getModelManager()));
    }
    static <T extends net.minecraft.world.entity.LivingEntity> HumanoidModel<T> model(float dilation) {
        return new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(dilation),0),64,32).bakeRoot());
    }
    @Override public ResourceLocation getTextureLocation(T entity) { return SKIN; }
    @Override protected void scale(T entity,PoseStack pose,float partial) { if (entity instanceof CultistLeaderEntity) pose.scale(1.25f,1.25f,1.25f); }
    @Override public void render(T entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        float bob=entity instanceof CultistClericEntity cleric && cleric.ritualist()
                ? Mth.sin((entity.tickCount+partial+new java.util.Random(entity.getId()).nextInt(1000))/9)*.1f+.21f : 0;
        pose.pushPose(); pose.translate(0,bob,0); super.render(entity,yaw,partial,pose,buffers,light); pose.popPose();
        if (bob!=0 && entity instanceof CultistClericEntity cleric) CultistRitualBeam.render(cleric,bob,partial,pose,buffers,light);
    }
}
