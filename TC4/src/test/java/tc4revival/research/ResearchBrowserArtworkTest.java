package tc4revival.research;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchBrowserArtworkTest {
    @Test void completeLinesAreSteadyAndSiblingColorsDiffer() {
        var parent = link(ResearchBrowser.LinkKind.PARENT, ResearchBrowser.LinkState.COMPLETE, 48, 0);
        assertEquals(ResearchBrowserLines.sample(parent, 0), ResearchBrowserLines.sample(parent, 57));
        var points = ResearchBrowserLines.sample(parent, 0);
        assertEquals(25, points.size()); assertEquals(.6f, points.get(0).alpha());
        assertTrue(points.stream().allMatch(p -> p.red() == .1f && p.green() == .1f && p.blue() == .1f));
        var sibling = ResearchBrowserLines.sample(link(ResearchBrowser.LinkKind.SIBLING, ResearchBrowser.LinkState.COMPLETE, 0, 48), 0);
        assertEquals(.2f, sibling.get(0).blue());
        assertEquals(points.get(12).x(), sibling.get(12).y());
        assertEquals(points.get(12).y(), sibling.get(12).x());
    }
    @Test void availableAndLockedLinksAnimateFadeAndUseDistinctColors() {
        var green = ResearchBrowserLines.sample(link(ResearchBrowser.LinkKind.PARENT, ResearchBrowser.LinkState.TARGET_COMPLETE, 48, 24), 0);
        var blue = ResearchBrowserLines.sample(link(ResearchBrowser.LinkKind.PARENT, ResearchBrowser.LinkState.INCOMPLETE, 48, 24), 0);
        assertEquals(0, green.get(0).alpha()); assertEquals(1, green.get(0).green()); assertEquals(0, green.get(0).blue());
        assertEquals(1, blue.get(0).blue()); assertEquals(0, blue.get(0).green());
        assertEquals(.6f, green.get(green.size() - 1).alpha()); assertEquals(0, green.get(green.size() - 1).green());
        assertNotEquals(green, ResearchBrowserLines.sample(link(ResearchBrowser.LinkKind.PARENT, ResearchBrowser.LinkState.TARGET_COMPLETE, 48, 24), 13));
        assertTrue(green.stream().allMatch(p -> Float.isFinite(p.x()) && Float.isFinite(p.y())));
    }
    @Test void coincidentOrSubpixelLinksDoNotGenerateInvalidVertices() {
        for (var state : ResearchBrowser.LinkState.values()) {
            assertTrue(ResearchBrowserLines.sample(link(ResearchBrowser.LinkKind.PARENT, state, 0, 0), 2).isEmpty());
            assertTrue(ResearchBrowserLines.sample(link(ResearchBrowser.LinkKind.PARENT, state, 1, 0), 2).isEmpty());
        }
        assertThrows(UnsupportedOperationException.class, () -> ResearchBrowserLines.sample(link(ResearchBrowser.LinkKind.PARENT,
                ResearchBrowser.LinkState.COMPLETE, 48, 24), 0).clear());
    }
    @Test void everyCatalogTextureAndBrowserAtlasMatchesTheOriginalBytes() throws Exception {
        var textures = new TreeSet<String>(List.of("thaumcraft:textures/gui/gui_research.png",
                "thaumcraft:textures/misc/nodes.png", "thaumcraft:textures/misc/particles.png"));
        try (var reader = new InputStreamReader(tc4revival.resources.ExternalTestResources.open("/research-catalog.json"), StandardCharsets.UTF_8)) {
            for (var value : JsonParser.parseReader(reader).getAsJsonArray()) {
                var category = value.getAsJsonObject();
                textures.add(category.get("icon").getAsString()); textures.add(category.get("background").getAsString());
                for (var entry : category.getAsJsonArray("research")) {
                    var icon = entry.getAsJsonObject().get("icon_resource");
                    if (icon != null && !icon.isJsonNull()) textures.add(icon.getAsString());
                }
            }
        }
        assertEquals(37, textures.size());
        for (String texture : textures) {
            String path = "assets/" + texture.replace(':', '/');
            try (var stream = tc4revival.resources.ExternalTestResources.open("/" + path)) {
                assertNotNull(stream, texture);
                assertArrayEquals(Files.readAllBytes(Path.of("references/resources").resolve(path)), stream.readAllBytes(), texture);
            }
        }
    }
    private ResearchBrowser.Link link(ResearchBrowser.LinkKind kind, ResearchBrowser.LinkState state, int x, int y) {
        return new ResearchBrowser.Link("A", "B", x, y, 0, 0, kind, state);
    }
}
