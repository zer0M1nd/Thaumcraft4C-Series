package tc4revival.client;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NegativeAspectRenderingTest {
    @Test void shaderExplicitlyBlendsTextureAlphaInsteadOfDefaultingToOpaque() throws Exception {
        var json=JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/thaumcraft/shaders/core/research_negative.json"))).getAsJsonObject();
        var blend=json.getAsJsonObject("blend");assertNotNull(blend);
        assertEquals("srcalpha",blend.get("srcrgb").getAsString());
        assertEquals("1-srcalpha",blend.get("dstrgb").getAsString());
        assertEquals("1",blend.get("srcalpha").getAsString());
    }
    @Test void filledHexHasSameFlatTopBoundsAsOriginalSlotArtwork() {
        var outline=ResearchTableView.cellOutline();assertEquals(6,outline.size());
        double top=outline.stream().mapToDouble(ResearchTableView.Point::y).min().orElseThrow();
        double bottom=outline.stream().mapToDouble(ResearchTableView.Point::y).max().orElseThrow();
        assertEquals(2,outline.stream().filter(p->p.y()==top).count());
        assertEquals(2,outline.stream().filter(p->p.y()==bottom).count());
        assertEquals(1.5,top);assertEquals(14.5,bottom);
        assertEquals(16,outline.stream().mapToDouble(ResearchTableView.Point::x).min().orElseThrow()
                +outline.stream().mapToDouble(ResearchTableView.Point::x).max().orElseThrow());
    }
}
