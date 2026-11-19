package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tc4revival.content.ThaumicSlimeEntity;

public  class ThaumicSlimeRenderer extends MobRenderer<ThaumicSlimeEntity,SlimeModel<ThaumicSlimeEntity>> {
    private static final ResourceLocation TEXTURE=new ResourceLocation("thaumcraft","textures/models/tslime.png");
    public ThaumicSlimeRenderer(EntityRendererProvider.Context context) {
        super(context,new SlimeModel<>(context.bakeLayer(ModelLayers.SLIME)),.25f);
        var shell=new SlimeModel<ThaumicSlimeEntity>(context.bakeLayer(ModelLayers.SLIME_OUTER));
        addLayer(new RenderLayer<ThaumicSlimeEntity,SlimeModel<ThaumicSlimeEntity>>(this) {
            @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,ThaumicSlimeEntity entity,float walk,float speed,float partial,float age,float yaw,float pitch) {
                if (entity.isInvisible()) return;
                getParentModel().copyPropertiesTo(shell); shell.prepareMobModel(entity,walk,speed,partial); shell.setupAnim(entity,walk,speed,age,yaw,pitch);
                shell.renderToBuffer(pose,buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)),light,LivingEntityRenderer.getOverlayCoords(entity,0),1,1,1,1);
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(ThaumicSlimeEntity entity) { return TEXTURE; }
    @Override protected void scale(ThaumicSlimeEntity entity,PoseStack pose,float partial) {
        float root=(float)Math.sqrt(entity.slimeSize()), squash=Mth.lerp(partial,entity.previousSquish,entity.squish)/(root*.25f+1);
        float horizontal=1/(squash+1); pose.scale(horizontal*root+.1f,root/horizontal+.1f,horizontal*root+.1f);
    }
}
