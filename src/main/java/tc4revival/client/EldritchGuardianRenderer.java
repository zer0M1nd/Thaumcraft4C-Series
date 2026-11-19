package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import tc4revival.content.EldritchGuardianEntity;

public  class EldritchGuardianRenderer extends MobRenderer<EldritchGuardianEntity,EldritchGuardianModel<EldritchGuardianEntity>> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/eldritch_guardian.png");
    public EldritchGuardianRenderer(EntityRendererProvider.Context context) { super(context,new EldritchGuardianModel<>(),.5f); }
    @Override public ResourceLocation getTextureLocation(EldritchGuardianEntity entity) { return TEXTURE; }
    @Override protected RenderType getRenderType(EldritchGuardianEntity entity,boolean visible,boolean translucent,boolean glowing) {
        return visible || translucent ? RenderType.entityTranslucent(TEXTURE) : glowing ? RenderType.outline(TEXTURE) : null;
    }
    @Override public void render(EldritchGuardianEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        var camera=Minecraft.getInstance().getCameraEntity(); model.opacity=1;
        if (!entity.inOuterLands() && camera!=null) {
            float limit=entity.level().getDifficulty()==Difficulty.HARD ? 576 : 1024;
            double distance=entity.distanceToSqr(camera);
            model.opacity=distance<256 ? .6f : (float)(1-Math.min(limit-256,distance-256)/(limit-256))*.6f;
        }
        super.render(entity,yaw,partial,pose,buffers,light);
    }
}
