package thaumicfutures.compat.bees;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;

/** Builds bee artwork from the archive currently being prepared by Thaumcraft. */
public final class LegacyBeeResources {
    private static final String ASSETS = "assets/thaumicfutures/";
    private static final String INPUT = ASSETS + "legacy_bees/";
    private static final String MATERIAL = "mtllib vis_aura_provider.mtl\nusemtl provider\n";
    private record Palette(List<String> layers, Map<String, List<String>> items) {}

    private LegacyBeeResources() {}

    private static final class Palettes {
        private static final Palette[] ALL = load();
        private static Palette[] load() {
            try (var stream = Objects.requireNonNull(LegacyBeeResources.class.getResourceAsStream("/thaumicfutures/bee_sprite_colors.json"));
                 var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return new Gson().fromJson(reader, Palette[].class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static void prepare(Map<String, byte[]> resources) {
        if (resources.keySet().stream().noneMatch(path -> path.startsWith(INPUT))) return;
        byte[] mesh = resources.remove(INPUT + "vis_aura.obj");
        if (mesh != null) prepareModel(resources, mesh);
        try {
            for (var palette : Palettes.ALL) {
                if (resources.containsKey(INPUT + palette.layers().get(0) + ".png")) {
                    prepareSprites(resources, palette);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot prepare bee textures", e);
        }
    }

    private static void prepareModel(Map<String, byte[]> resources, byte[] mesh) {
        String original = new String(mesh, StandardCharsets.UTF_8);
        String directory = ASSETS + "models/block/vis_aura_";
        resources.put(directory + "provider.obj", (MATERIAL + original).getBytes(StandardCharsets.UTF_8));
        // Both animated parts use the original global vertex and UV indices.
        for (String part : List.of("frame", "centre")) {
            var result = new StringBuilder(MATERIAL);
            String object = "";
            for (String line : original.lines().toList()) {
                if (line.startsWith("o ")) object = line.substring(2).strip();
                if (part.equals(object) || !(line.startsWith("o ") || line.startsWith("f "))) {
                    result.append(line).append('\n');
                }
            }
            resources.put(directory + part + ".obj", result.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void prepareSprites(Map<String, byte[]> resources, Palette palette) throws IOException {
        var layers = new ArrayList<BufferedImage>();
        byte[] animation = null;
        for (String layer : palette.layers()) {
            String path = INPUT + layer + ".png";
            byte[] bytes = resources.remove(path);
            if (bytes == null) throw new IOException("Missing bee sprite layer " + path);
            var image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) throw new IOException("Invalid bee sprite layer " + path);
            layers.add(image);
            byte[] metadata = resources.remove(path + ".mcmeta");
            if (metadata != null) animation = animationMetadata(metadata, image.getWidth(), image.getHeight());
        }
        for (var entry : palette.items().entrySet()) {
            var colors = entry.getValue();
            BufferedImage sprite = tint(layers.get(0), colors.get(0));
            for (int i = 1; i < layers.size(); i++) {
                var overlay = tint(layers.get(i), colors.get(i));
                if (overlay.getWidth() != sprite.getWidth() || overlay.getHeight() != sprite.getHeight()) {
                    throw new IOException("Mismatched layers for " + entry.getKey());
                }
                var graphics = sprite.createGraphics();
                try {
                    graphics.setComposite(AlphaComposite.SrcOver);
                    graphics.drawImage(overlay, 0, 0, null);
                } finally {
                    graphics.dispose();
                }
            }
            String target = ASSETS + "textures/item/" + entry.getKey() + ".png";
            var encoded = new ByteArrayOutputStream();
            ImageIO.write(sprite, "png", encoded);
            resources.put(target, encoded.toByteArray());
            if (animation != null) resources.put(target + ".mcmeta", animation);
        }
    }

    private static BufferedImage tint(BufferedImage layer, String color) {
        int rgb = Integer.parseInt(color, 16);
        var result = new BufferedImage(layer.getWidth(), layer.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < layer.getHeight(); y++) {
            for (int x = 0; x < layer.getWidth(); x++) {
                int pixel = layer.getRGB(x, y);
                int red = (pixel >>> 16 & 255) * (rgb >>> 16 & 255) / 255;
                int green = (pixel >>> 8 & 255) * (rgb >>> 8 & 255) / 255;
                int blue = (pixel & 255) * (rgb & 255) / 255;
                result.setRGB(x, y, (pixel & 0xff000000) | red << 16 | green << 8 | blue);
            }
        }
        return result;
    }

    private static byte[] animationMetadata(byte[] metadata, int width, int height) {
        var root = JsonParser.parseString(new String(metadata, StandardCharsets.UTF_8)).getAsJsonObject();
        var animation = root.getAsJsonObject("animation");
        // Magic Bees describes its vertical wax strip in cells. Modern Minecraft expects pixels.
        if (animation != null && animation.has("width") && animation.has("height")
                && animation.get("width").getAsInt() == 1 && animation.get("height").getAsInt() * width == height) {
            animation.addProperty("width", width);
            animation.addProperty("height", width);
            return root.toString().getBytes(StandardCharsets.UTF_8);
        }
        return metadata;
    }
}
