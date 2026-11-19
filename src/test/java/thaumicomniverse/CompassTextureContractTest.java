package thaumicomniverse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The vanilla compass pixels require the vanilla angle convention, not a simple i/32 LUT. */
class CompassTextureContractTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/thaumicomniverse");
    private static final Set<Integer> NEEDLE = Set.of(0x76334D, 0x8C3D5C, 0xB04D70);

    private static JsonObject json(String relative) throws Exception {
        return JsonParser.parseString(Files.readString(ASSETS.resolve(relative))).getAsJsonObject();
    }

    private static BufferedImage image(String name) throws Exception {
        var image = ImageIO.read(ASSETS.resolve("textures/item/" + name + ".png").toFile());
        assertNotNull(image, name);
        return image;
    }

    @Test void vanillaHalfFrameBoundariesAndWraparoundResolveToCorrectDirection() throws Exception {
        for (String name : List.of("sinister_compass", "primal_compass")) {
            var root = json("models/item/" + name + ".json");
            assertEquals("thaumicomniverse:item/" + name + "_16", root.getAsJsonObject("textures").get("layer0").getAsString());
            var overrides = root.getAsJsonArray("overrides");
            assertEquals(33, overrides.size());
            double previous = -1;
            for (var entry : overrides) {
                double threshold = entry.getAsJsonObject().getAsJsonObject("predicate").get("angle").getAsDouble();
                assertTrue(threshold > previous, "Overrides must be strictly ordered");
                previous = threshold;
            }
            // Includes every exact transition and samples immediately either side of it.
            for (int sample = 0; sample <= 4096; sample++) {
                double angle = sample / 4096.0;
                int expected = ((int) Math.floor(angle * 32 + 0.5) + 16) % 32;
                String selected = "";
                for (var entry : overrides) {
                    var override = entry.getAsJsonObject();
                    if (angle >= override.getAsJsonObject("predicate").get("angle").getAsDouble())
                        selected = override.get("model").getAsString();
                }
                assertEquals("thaumicomniverse:item/" + name + "_" + String.format("%02d", expected), selected, name + " angle=" + angle);
            }
        }
    }

    @Test void everyDirectionExistsAndPrimalColorAnimationCannotMoveTheNeedle() throws Exception {
        for (int direction = 0; direction < 32; direction++) {
            String suffix = String.format("_%02d", direction);
            var sinister = image("sinister_compass" + suffix);
            var primal = image("primal_compass" + suffix);
            assertEquals(16, sinister.getWidth());
            assertEquals(16, sinister.getHeight());
            assertEquals(16, primal.getWidth());
            assertEquals(16 * 12, primal.getHeight());
            for (String name : List.of("sinister_compass", "primal_compass")) {
                var child = json("models/item/" + name + suffix + ".json");
                assertEquals("minecraft:item/generated", child.get("parent").getAsString());
                assertEquals("thaumicomniverse:item/" + name + suffix, child.getAsJsonObject("textures").get("layer0").getAsString());
            }
            var animation = json("textures/item/primal_compass" + suffix + ".png.mcmeta").getAsJsonObject("animation");
            assertEquals(3, animation.get("frametime").getAsInt());
            assertFalse(animation.get("interpolate").getAsBoolean());
            int needlePixels = 0;
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                boolean needle = NEEDLE.contains(sinister.getRGB(x, y) & 0xFFFFFF);
                if (needle) needlePixels++;
                for (int frame = 0; frame < 12; frame++) {
                    int value = primal.getRGB(x, y + frame * 16);
                    assertEquals(sinister.getRGB(x, y) >>> 24, value >>> 24);
                    if (frame > 0) assertEquals(needle, value != primal.getRGB(x, y), "Only the needle changes color");
                }
            }
            assertTrue(needlePixels >= 3, "A visible pointer must exist in every orientation");
        }
    }

    @Test void cardinalNeedlesKeepVanillaOrientationAndCertificateKeepsAllFrames() throws Exception {
        int[][] tips = {{0, 8, 9}, {8, 3, 7}, {16, 8, 5}, {24, 12, 7}};
        for (int[] tip : tips) {
            var sprite = image(String.format("sinister_compass_%02d", tip[0]));
            assertTrue(NEEDLE.contains(sprite.getRGB(tip[1], tip[2]) & 0xFFFFFF));
        }
        var certificate = image("thaumaturge_certificate");
        assertEquals(16, certificate.getWidth());
        assertEquals(144, certificate.getHeight());
        var animation = json("textures/item/thaumaturge_certificate.png.mcmeta").getAsJsonObject("animation");
        assertEquals(2, animation.get("frametime").getAsInt());
        var frames = animation.getAsJsonArray("frames");
        assertEquals(9, frames.size());
        assertEquals(0, frames.get(0).getAsJsonObject().get("index").getAsInt());
        assertEquals(40, frames.get(0).getAsJsonObject().get("time").getAsInt());
        for (int i = 1; i < 9; i++) assertEquals(i, frames.get(i).getAsInt());
    }
}
