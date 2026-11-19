package thaumicfutures;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import thaumicfutures.compat.bees.LegacyBeeResources;
import static org.junit.jupiter.api.Assertions.*;

class LegacyBeeResourcesTest {
    private static final String ROOT = "assets/thaumicfutures/";
    private static final String RAW = ROOT + "legacy_bees/";
    private static final String ITEMS = ROOT + "textures/item/";

    @Test void theArchiveWithWaxCanBePreparedTwiceWithoutTintingTwice() throws Exception {
        var layer = new BufferedImage(2, 4, BufferedImage.TYPE_INT_ARGB);
        layer.setRGB(1, 3, 0x80ffffff);
        var encoded = new ByteArrayOutputStream();
        ImageIO.write(layer, "png", encoded);
        var pack = new HashMap<String, byte[]>();
        pack.put(RAW + "magic_wax.png", encoded.toByteArray());
        pack.put(RAW + "magic_wax.png.mcmeta", "{\"animation\":{\"width\":1,\"height\":2,\"frametime\":2}}".getBytes(StandardCharsets.UTF_8));
        LegacyBeeResources.prepare(pack);
        byte[] result = pack.get(ITEMS + "bee_wax_magic.png");
        assertEquals(0x80000000 | 13779679, ImageIO.read(new ByteArrayInputStream(result)).getRGB(1, 3));
        assertEquals(2, com.google.gson.JsonParser.parseString(new String(pack.get(ITEMS + "bee_wax_magic.png.mcmeta"),
                StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animation").get("width").getAsInt());
        assertEquals(2, pack.size());
        LegacyBeeResources.prepare(pack);
        assertArrayEquals(result, pack.get(ITEMS + "bee_wax_magic.png"));
    }

    @Test void splitMeshesKeepGlobalIndicesAndOnlyTheirOwnFaces() {
        String mesh = "v 0 0 0\no frame\nf 1 2 3\no centre\nf 3 2 1\n";
        Map<String, byte[]> pack = new HashMap<>();
        pack.put(RAW + "vis_aura.obj", mesh.getBytes(StandardCharsets.UTF_8));
        LegacyBeeResources.prepare(pack);
        String models = ROOT + "models/block/vis_aura_";
        String whole = new String(pack.get(models + "provider.obj"), StandardCharsets.UTF_8);
        String frame = new String(pack.get(models + "frame.obj"), StandardCharsets.UTF_8);
        String centre = new String(pack.get(models + "centre.obj"), StandardCharsets.UTF_8);
        assertTrue(whole.endsWith(mesh));
        assertTrue(frame.endsWith("v 0 0 0\no frame\nf 1 2 3\n"));
        assertTrue(centre.endsWith("v 0 0 0\no centre\nf 3 2 1\n"));
        assertEquals(3, pack.size());
    }
}
