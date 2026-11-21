package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import tc4revival.content.FrostShardEntity;

public  class FrostShardRenderer extends EntityRenderer<FrostShardEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.parse("thaumcraft:textures/models/orb.obj");
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("thaumcraft:textures/block/frostshard.png");
    private static GroupedObjMesh mesh;
    public FrostShardRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0; }
    public static void reload(ResourceManager resources) { mesh = null; }
    @Override public ResourceLocation getTextureLocation(FrostShardEntity entity) { return TEXTURE; }
    @Override public void render(FrostShardEntity shard, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (mesh == null) try (var stream = Minecraft.getInstance().getResourceManager().getResourceOrThrow(MODEL).open()) {
            mesh = GroupedObjMesh.read(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (java.io.IOException error) { throw new IllegalStateException("Unable to load frost shard mesh", error); }
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial, shard.yRotO, shard.getYRot())));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial, shard.xRotO, shard.getXRot())));
        var random = new Random(shard.getId()); float base = shard.damage() * .1f;
        pose.scale(base + random.nextFloat() * .1f, base + random.nextFloat() * .1f, base + random.nextFloat() * .1f);
        var frame = pose.last(); var out = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        for (var group : mesh.groups().values()) for (var v : group) out.vertex(frame.pose(), v.x(), v.y(), v.z()).color(1f, 1f, 1f, 1f)
                .uv(v.u(), v.v()).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(frame.normal(), v.nx(), v.ny(), v.nz()).endVertex();
        pose.popPose();
    }
}
