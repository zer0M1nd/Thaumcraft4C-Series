package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;

public  class ConnectedFaces {
    private static volatile int[] indices;
    private ConnectedFaces() {}
    public static void reload(ResourceManager resources) { indices = null; }
    static int texture(int mask) {
        int[] resolved=indices;
        if (resolved == null) try (var stream = Minecraft.getInstance().getResourceManager().getResourceOrThrow(ResourceLocation.parse("thaumcraft:connected_textures.json")).open()) {
            var array = com.google.gson.JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("indices");
            if (array.size() != 256) throw new java.io.IOException("Expected 256 connected-texture entries");
            resolved = new int[256]; for (int i = 0; i < resolved.length; i++) resolved[i] = array.get(i).getAsInt();
            indices=resolved;
        } catch (java.io.IOException | RuntimeException error) { throw new IllegalStateException("Unable to load connected textures", error); }
        return resolved[mask];
    }
    /** Pose origin is the selected block's lower corner. */
    public static void render(BlockPos pos, Predicate<BlockPos> connected, Predicate<BlockPos> hidden, PoseStack pose, MultiBufferSource buffers,
            float red, float green, float blue, float alpha, boolean through) {
        for (var face : Direction.values()) {
            if (hidden.test(pos.relative(face))) continue;
            Direction right = switch (face) { case NORTH -> Direction.WEST; case SOUTH, UP, DOWN -> Direction.EAST; case WEST -> Direction.SOUTH; case EAST -> Direction.NORTH; };
            Direction down = face.getAxis() == Direction.Axis.Y ? Direction.SOUTH : Direction.DOWN;
            int mask = 0, bit = 0;
            for (int row = -1; row <= 1; row++) for (int col = -1; col <= 1; col++) {
                if (row == 0 && col == 0) continue;
                if (connected.test(pos.relative(right, col).relative(down, row))) mask |= 1 << bit;
                bit++;
            }
            var asset = ResourceLocation.parse("thaumcraft:textures/block/warded_glass_" + (texture(mask) + 1) + ".png");
            var out = TCRenderTypes.buffer(buffers,through ? TCRenderTypes.revealedNode(asset, false) : TCRenderTypes.runes(asset));
            var n = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
            var horizontal = new Vec3(right.getStepX(), right.getStepY(), right.getStepZ());
            var vertical = new Vec3(down.getStepX(), down.getStepY(), down.getStepZ());
            var center = new Vec3(.5, .5, .5).add(n.scale(.501));
            for (int i = 0; i < 4; i++) {
                int x = i < 2 ? -1 : 1, y = i == 0 || i == 3 ? -1 : 1;
                var point = center.add(horizontal.scale(x * .501)).add(vertical.scale(y * .501));
                out.vertex(pose.last().pose(), (float) point.x, (float) point.y, (float) point.z).color(red, green, blue, alpha)
                        .uv((x + 1) / 2f, (y + 1) / 2f).uv2(200).endVertex();
            }
        }
    }
}
