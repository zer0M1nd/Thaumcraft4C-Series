package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tc4revival.content.EldritchWardenEntity;

public  class EldritchWardenRenderer extends MobRenderer<EldritchWardenEntity,EldritchGuardianModel<EldritchWardenEntity>> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/eldritch_warden.png");
    public EldritchWardenRenderer(EntityRendererProvider.Context context) {
        super(context,new EldritchGuardianModel<>(),.5f);
        addLayer(new RenderLayer<>(this) {
            @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,EldritchWardenEntity entity,float walk,float amount,float partial,float age,float yaw,float pitch) {
                int pulse=(int)(210+Mth.sin(entity.tickCount/3f)*15);
                getParentModel().renderEye(pose,TCRenderTypes.buffer(buffers,TCRenderTypes.eldritchEye(TEXTURE)),pulse);
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(EldritchWardenEntity entity) { return TEXTURE; }
    @Override protected RenderType getRenderType(EldritchWardenEntity entity,boolean visible,boolean translucent,boolean glowing) {
        return visible || translucent ? RenderType.entityTranslucent(TEXTURE) : glowing ? RenderType.outline(TEXTURE) : null;
    }
    @Override protected void scale(EldritchWardenEntity entity,PoseStack pose,float partial) { pose.scale(1.5f,1.5f,1.5f); }
    @Override public void render(EldritchWardenEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        pose.pushPose(); pose.translate(0,-entity.getBbHeight()*entity.spawnTimer()/150f,0);
        super.render(entity,yaw,partial,pose,buffers,light); pose.popPose();
    }
}
