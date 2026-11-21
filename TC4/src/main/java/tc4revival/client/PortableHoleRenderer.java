package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import tc4revival.content.PortableHoleBlockEntity;
import tc4revival.content.TCArcane;

/** Old sixteen-depth tunnel layers, with camera projection replacing fixed-function OpenGL texture generation. */
public  class PortableHoleRenderer implements BlockEntityRenderer<PortableHoleBlockEntity> {
    private static final ResourceLocation TUNNEL = texture("tunnel"), FIELD = texture("particlefield"), DISTANT = texture("particlefield32");
    public PortableHoleRenderer(BlockEntityRendererProvider.Context context) {}
    private static ResourceLocation texture(String name) { return ResourceLocation.parse("thaumcraft:textures/misc/" + name + ".png"); }
    @Override public void render(PortableHoleBlockEntity hole, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = hole.getLevel(); if (level == null) return;
        var origin = Vec3.atLowerCornerOf(hole.getBlockPos());
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        boolean near = camera.distanceToSqr(origin.add(.5, .5, .5)) < 512;
        for (var face : Direction.values()) {
            var neighbour = hole.getBlockPos().relative(face); var block = level.getBlockState(neighbour);
            if (block.is(TCArcane.PORTABLE_HOLE.get()) || !block.isSolidRender(level, neighbour)) continue;
            var normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
            var up = face.getAxis() == Direction.Axis.Y ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
            var right = up.cross(normal); var center = new Vec3(.5, .5, .5).add(normal.scale(.499));
            var random = new Random(31100);
            for (int layer = 0; layer < (near ? 16 : 1); layer++) {
                float depth = layer == 0 ? 65 : 16 - layer;
                float scale = layer == 0 ? .125f : layer == 1 ? .5f : .0625f;
                float tint = layer == 0 ? .1f : 1 / (17f - layer);
                float red = random.nextFloat() * .5f + .1f, green = random.nextFloat() * .5f + .4f, blue = random.nextFloat() * .5f + .5f;
                if (layer == 0) red = green = blue = 1;
                if (!near) { tint = .5f; red = green = blue = 1; }
                var type = !near ? TCRenderTypes.orb(DISTANT) : layer == 0 ? TCRenderTypes.orb(TUNNEL) : TCRenderTypes.runes(FIELD);
                var out = TCRenderTypes.buffer(buffers,type);
                double angle = Math.toRadians((layer * layer * 4321 + layer * 9) * 2);
                double scroll = (System.currentTimeMillis() % 700000L) / 250000.0;
                for (int corner = 0; corner < 4; corner++) {
                    int sx = corner < 2 ? -1 : 1, sy = corner == 0 || corner == 3 ? -1 : 1;
                    var point = center.add(right.scale(sx * .5)).add(up.scale(sy * .5));
                    double u = (sx + 1) / 2.0, v = (sy + 1) / 2.0;
                    if (near) {
                        var ray = origin.add(point).subtract(camera);
                        double normalDistance = ray.dot(normal);
                        // Avoid unbounded UVs when the camera lies exactly in the surface plane.
                        double ratio = Math.abs(normalDistance) < 1e-5 ? 1 : 1 + depth / Math.abs(normalDistance);
                        var projected = camera.add(ray.scale(ratio));
                        double a = projected.dot(right) - .5, b = projected.dot(up) - .5;
                        u = (a * Math.cos(angle) - b * Math.sin(angle) + .5) * scale;
                        v = (a * Math.sin(angle) + b * Math.cos(angle) + .5) * scale + scroll;
                    }
                    out.vertex(pose.last().pose(), (float) point.x, (float) point.y, (float) point.z).color(red * tint, green * tint, blue * tint, 1)
                            .uv((float) u, (float) v).uv2(180).endVertex();
                }
            }
        }
    }
}
