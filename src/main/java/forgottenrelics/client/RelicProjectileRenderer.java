package forgottenrelics.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;

import forgottenrelics.RelicProjectile;

import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** The original 12 Botania gate sprites and halo, with a billboard for energy spells. */
public final class RelicProjectileRenderer extends EntityRenderer<RelicProjectile> {
    public RelicProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(RelicProjectile entity) {
        return entity.kind() == RelicProjectile.Kind.BABYLON
                ? new ResourceLocation(
                        "botania", "textures/item/gate_weapon_" + entity.variant() + ".png")
                : new ResourceLocation("minecraft", "textures/particle/glow.png");
    }

    @Override
    public void render(
            RelicProjectile entity,
            float yaw,
            float partial,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight) {
        pose.pushPose();
        if (entity.kind() == RelicProjectile.Kind.BABYLON) {
            var movement = entity.getDeltaMovement();
            pose.mulPose(
                    Axis.YP.rotationDegrees(
                            (float) Math.toDegrees(Math.atan2(movement.x, movement.z))));
            pose.mulPose(Axis.ZP.rotationDegrees(45));
            pose.scale(1.5f, 1.5f, 1.5f);
            float charge = Math.min(1, (entity.tickCount + partial) / 10f);
            quad(pose, buffers, getTextureLocation(entity), 0xffffff, charge);
            if (entity.tickCount < 20) {
                pose.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partial) * 9));
                pose.scale(2, 2, 2);
                pose.translate(0, 0, .1);
                quad(
                        pose,
                        buffers,
                        new ResourceLocation("botania", "textures/misc/babylon.png"),
                        0xffffff,
                        charge);
            }
        } else {
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            float size =
                    entity.kind() == RelicProjectile.Kind.CRIMSON
                            ? .6f
                            : entity.kind() == RelicProjectile.Kind.LUNAR ? .8f : .3f;
            pose.scale(size, size, size);
            quad(pose, buffers, getTextureLocation(entity), entity.color(), .9f);
        }
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, packedLight);
    }

    private static void quad(
            PoseStack pose,
            MultiBufferSource buffers,
            ResourceLocation texture,
            int color,
            float alpha) {
        var vertex = buffers.getBuffer(RenderType.entityTranslucent(texture));
        var matrix = pose.last().pose();
        var normal = pose.last().normal();
        float r = ((color >> 16) & 255) / 255f,
                g = ((color >> 8) & 255) / 255f,
                b = (color & 255) / 255f;
        vertex.vertex(matrix, -.5f, -.5f, 0)
                .color(r, g, b, alpha)
                .uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(normal, 0, 0, 1)
                .endVertex();
        vertex.vertex(matrix, .5f, -.5f, 0)
                .color(r, g, b, alpha)
                .uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(normal, 0, 0, 1)
                .endVertex();
        vertex.vertex(matrix, .5f, .5f, 0)
                .color(r, g, b, alpha)
                .uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(normal, 0, 0, 1)
                .endVertex();
        vertex.vertex(matrix, -.5f, .5f, 0)
                .color(r, g, b, alpha)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(normal, 0, 0, 1)
                .endVertex();
    }
}
