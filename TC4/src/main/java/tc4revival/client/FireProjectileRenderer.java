package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import tc4revival.content.EmberEntity;

public  class FireProjectileRenderer<T extends Entity> extends EntityRenderer<T> {
    private static final ResourceLocation EMBER = ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private static final ResourceLocation ORB = ResourceLocation.parse("thaumcraft:textures/misc/particles2.png");
    public FireProjectileRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0; }
    @Override public ResourceLocation getTextureLocation(T entity) { return entity instanceof EmberEntity ? EMBER : ORB; }
    @Override public void render(T entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        boolean ember = entity instanceof EmberEntity;
        float age = ember ? entity.tickCount / (float) ((EmberEntity) entity).duration() : 0;
        float scale = ember ? .25f + age : .7f;
        float u = ember ? (7 + (int) (8 * age)) / 16f : entity.tickCount % 4 / 16f;
        float v = ember ? .5625f : .8125f;
        pose.pushPose(); pose.mulPose(entityRenderDispatcher.cameraOrientation()); pose.scale(scale, scale, scale);
        var type = ember ? TCRenderTypes.runes(EMBER) : TCRenderTypes.softTranslucent(ORB);
        ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,type), pose.last().pose(), .5f, u, v, u + .0625f, v + .0625f, 1, 1, 1, ember ? .9f : .8f, 220);
        pose.popPose();
    }
}
