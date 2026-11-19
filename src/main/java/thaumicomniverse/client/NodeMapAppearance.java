package thaumicomniverse.client;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import tc4revival.aspect.*;
import tc4revival.content.*;

/** Item-node sprite rows and tint/brightness rules, normalized to a fixed map footprint. */
public final class NodeMapAppearance {
    public static final ResourceLocation TEXTURE = ResourceLocation.parse("thaumcraft:textures/misc/nodes.png");
    public record Layer(int row, int color, float opacity, double size, boolean fading) {
        public float alpha(long millis) {
            return opacity * (fading ? (float)(Math.sin(millis / 150.0) * .25 + .33) : 1);
        }
    }
    public static List<Layer> layers(CompoundTag state) {
        return layers(state, TCAspects.catalog());
    }
    public static List<Layer> layers(CompoundTag state, AspectCatalog catalog) {
        var amounts = AspectNbt.read(state, catalog).amounts();
        if (amounts.isEmpty()) amounts = AspectNbt.read(state, "AspectsBase", catalog).amounts();
        var modifier = state.contains("modifier") ? AuraNodeModifier.fromId(state.getByte("modifier")) : null;
        float alpha = .5f * (modifier == AuraNodeModifier.BRIGHT ? 1.5f : modifier == AuraNodeModifier.PALE ? .66f : 1);
        boolean fading = modifier == AuraNodeModifier.FADING;
        var layers = new ArrayList<Layer>();
        // Every aspect has the same map size. Neither vis capacity nor depletion changes the footprint.
        for (var aspect : amounts.aspects()) layers.add(new Layer(0, aspect.color(),
                alpha / Math.max(1, amounts.size()) * (aspect.blendFactor() == 771 ? 1.5f : 1), 24, fading));
        int row = switch (AuraNodeType.fromId(state.getByte("type"))) {
            case UNSTABLE -> 6; case DARK -> 2; case TAINTED -> 5; case PURE -> 4; case HUNGRY -> 3; default -> 1;
        };
        layers.add(new Layer(row, 0xffffff, alpha, row == 3 ? 10.5 : 14, fading));
        return List.copyOf(layers);
    }
    public static int frame(long millis) { return (int)((millis / 40 + 1) % 32); }
    private static int tile;
    public static int tileSize() {
        if (tile == 0) {
            try (var in = net.minecraft.client.Minecraft.getInstance().getResourceManager().getResourceOrThrow(TEXTURE).open();
                 var image = com.mojang.blaze3d.platform.NativeImage.read(in)) {
                tile = Math.max(1, image.getWidth() / 32);
            } catch (java.io.IOException failure) { tile = 64; }
        }
        return tile;
    }
    public static void invalidate() { tile = 0; }
}
