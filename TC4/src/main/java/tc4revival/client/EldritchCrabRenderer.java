package tc4revival.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.EldritchCrabEntity;

public  class EldritchCrabRenderer extends MobRenderer<EldritchCrabEntity,EldritchCrabModel> {
    private static final ResourceLocation SKIN=ResourceLocation.parse("thaumcraft:textures/models/crab.png");
    private static final ResourceLocation OVERLAY=ResourceLocation.parse("thaumcraft:textures/models/craboverlay.png");
    public EldritchCrabRenderer(EntityRendererProvider.Context context) {
        super(context,new EldritchCrabModel(),1);
        addLayer(new RenderLayer<EldritchCrabEntity,EldritchCrabModel>(this) {
            @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,EldritchCrabEntity entity,
                    float walk,float amount,float partial,float age,float yaw,float pitch) {
                getParentModel().renderToBuffer(pose,buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(OVERLAY)),200,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,1,1,1,1);
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(EldritchCrabEntity entity) { return SKIN; }
    @Override public net.minecraft.world.phys.Vec3 getRenderOffset(EldritchCrabEntity entity,float partial) {
        return entity.isPassenger()?entity.ridingRenderPosition(partial).subtract(renderPosition(entity,partial))
                :super.getRenderOffset(entity,partial);
    }
    public static net.minecraft.world.phys.Vec3 renderPosition(net.minecraft.world.entity.Entity entity,float partial) {
        return new net.minecraft.world.phys.Vec3(net.minecraft.util.Mth.lerp(partial,entity.xOld,entity.getX()),
                net.minecraft.util.Mth.lerp(partial,entity.yOld,entity.getY()),net.minecraft.util.Mth.lerp(partial,entity.zOld,entity.getZ()));
    }
    @Override public boolean shouldRender(EldritchCrabEntity entity,net.minecraft.client.renderer.culling.Frustum frustum,double x,double y,double z) {
        if(entity.isPassenger())return entity.getVehicle().shouldRenderAtSqrDistance(entity.getVehicle().distanceToSqr(x,y,z));
        return super.shouldRender(entity,frustum,x,y,z);
    }
}
