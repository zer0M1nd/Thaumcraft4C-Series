package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.GolemEntity;

public  class GolemRenderer extends MobRenderer<GolemEntity,GolemModel> {
    private static final ResourceLocation DAMAGE=ResourceLocation.parse("thaumcraft:textures/models/golem_damage.png");
    public GolemRenderer(EntityRendererProvider.Context context) {
        super(context,new GolemModel(),.25f);
        addLayer(new RenderLayer<GolemEntity,GolemModel>(this) {
            private final GolemAccessories accessories=new GolemAccessories();
            @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,GolemEntity entity,float walk,float amount,float partial,float age,float yaw,float pitch) {
                if (entity.isInvisible()) return;
                GolemBadges.render(entity,pose,buffers,light);
                accessories.render(entity,getParentModel(),pose,buffers,light,walk,amount,partial);
                GolemBucketRenderer.render(entity,pose,buffers,light);
                GolemFishingRodRenderer.render(entity,getParentModel(),pose,buffers,light);
                GolemCarriedItemRenderer.render(entity,pose,buffers,light);
                float damage=1-entity.getHealth()/entity.getMaxHealth();
                if (damage>0) {
                    pose.pushPose(); pose.scale(.4f,.4f,.4f);
                    getParentModel().root().render(pose,buffers.getBuffer(RenderType.entityTranslucent(DAMAGE)),light,OverlayTexture.NO_OVERLAY,1,1,1,damage);
                    pose.popPose();
                }
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(GolemEntity entity) { return entity.configuration().blockAppearance()!=null?net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS:entity.configuration().properties().texture(); }

    @Override protected void setupRotations(GolemEntity entity,PoseStack pose,float age,float yaw,float partial) {
        super.setupRotations(entity,pose,age,yaw,partial);
        if (entity.walkAnimation.speed()>=.01f) {
            float walk=entity.walkAnimation.position()-entity.walkAnimation.speed()*(1-partial)+6;
            float triangle=(Math.abs(walk%13-6.5f)-3.25f)/3.25f;
            pose.mulPose(Axis.ZP.rotationDegrees(6.5f*triangle));
        }
    }
}
