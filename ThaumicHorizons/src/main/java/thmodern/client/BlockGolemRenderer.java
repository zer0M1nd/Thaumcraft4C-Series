package thmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import tc4revival.client.*;
import tc4revival.content.GolemEntity;

public final class BlockGolemRenderer extends MobRenderer<GolemEntity,BlockGolemModel> {
    public BlockGolemRenderer(EntityRendererProvider.Context context){
        super(context,new BlockGolemModel(),.25f);
        addLayer(new RenderLayer<GolemEntity,BlockGolemModel>(this){
            private final GolemAccessories accessories=new GolemAccessories();
            @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,GolemEntity entity,float walk,float amount,float partial,float age,float yaw,float pitch){
                if(entity.isInvisible())return;
                var animation=getParentModel().animation;
                GolemBadges.render(entity,pose,buffers,light);accessories.render(entity,animation,pose,buffers,light,walk,amount,partial);
                GolemBucketRenderer.render(entity,pose,buffers,light);GolemFishingRodRenderer.render(entity,animation,pose,buffers,light);GolemCarriedItemRenderer.render(entity,pose,buffers,light);
                float damage=1-entity.getHealth()/entity.getMaxHealth();
                if(damage>0){pose.pushPose();pose.scale(.4f,.4f,.4f);animation.root().render(pose,buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.parse("thaumcraft:textures/models/golem_damage.png"))),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,1,1,1,damage);pose.popPose();}
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(GolemEntity entity){return entity.configuration().blockAppearance()==null?ResourceLocation.parse("thaumichorizons:textures/models/golem_void.png"):net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;}
    @Override protected void setupRotations(GolemEntity entity,PoseStack pose,float age,float yaw,float partial){
        super.setupRotations(entity,pose,age,yaw,partial);
        if(entity.walkAnimation.speed()>=.01f){float walk=entity.walkAnimation.position()-entity.walkAnimation.speed()*(1-partial)+6;pose.mulPose(Axis.ZP.rotationDegrees(6.5f*(Math.abs(walk%13-6.5f)-3.25f)/3.25f));}
    }
}
