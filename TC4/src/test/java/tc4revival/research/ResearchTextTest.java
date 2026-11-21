package tc4revival.research;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tc4revival.research.ResearchText.*;

class ResearchTextTest {
    @Test void legacyAddonImagesUseMappedModernAssetsAndBadTranslationsDoNotCrashReader() {
        String legacy = "before<IMG>ttinkerer:textures/items/netherShard.png:0:0:255:255:0.0625</IMG>after";
        var mapped = ResearchText.remapPictures(legacy, java.util.Map.of("ttinkerer:textures/items/netherShard.png",
                "thaumic_tinkerer:textures/item/nether_shard.png"));
        assertTrue(ResearchText.parse(mapped).stream().anyMatch(p -> p instanceof Picture pic
                && pic.texture().equals("thaumic_tinkerer:textures/item/nether_shard.png")));
        assertTrue(ResearchText.parse(legacy).stream().anyMatch(p -> p instanceof Picture pic && pic.texture().endsWith("nethershard.png")));
        assertTrue(ResearchText.parseForDisplay("before<IMG>broken</IMG>after").contains(new Text("after")));
        assertDoesNotThrow(() -> ResearchText.parseForDisplay("before<IMG>broken"));
    }
    @Test void breaksAndInsertsPreserveTheirSurroundingLineBoundaries() {
        assertEquals(List.of(new Text("A"), new Break(), new Text("B"), new Break(), new Rule(), new Break(), new Text("C")),
                ResearchText.parse("A<BR>B<LINE/>C"));
        assertEquals(List.of(new Break(), new Break(), new Break()), ResearchText.parse("<BR/><BR>\n"));
        assertEquals(List.of(), ResearchText.parse(""));
    }
    @Test void textKeepsFormattingWhitespaceAndNonMarkupCharacters() {
        assertEquals(List.of(new Text("  §nHeading§r @0@ $ \\ <unknown>")), ResearchText.parse("  §nHeading§r @0@ $ \\ <unknown>"));
        assertEquals(List.of(new Break(), new Rule(), new Break(), new Break(), new Rule(), new Break()), ResearchText.parse("<LINE><LINE/>"));
    }
    @Test void pictureGeometryKeepsOddWidthIntegerDivisionAndOldUvCoordinates() {
        var image = (Picture) ResearchText.parse("<IMG>thaumcraft:textures/items/amber.png:0:0:255:255:0.0625</IMG>").get(1);
        assertEquals(58.0625f, image.xOffset(139));
        assertEquals(115, image.nextY(100, 9));
        var large = new Picture("thaumcraft:textures/misc/eldritchajor1.png", 0, 255, 255, 255, .5f);
        assertEquals(255, large.v()); assertEquals(227, large.nextY(100, 9));
        assertEquals(21, new Rule().xOffset(139)); assertEquals(2, new Rule().yOffset());
    }
    @Test void malformedImagesFailPromptlyWithoutRegexOrUnboundedReplacement() {
        for (String body : List.of("x", "thaumcraft:x:0:0:1:1:NaN", "thaumcraft:x:0:0:1:1:Infinity", "thaumcraft:x:0:0:0:1:1",
                "thaumcraft:../x:0:0:1:1:1", "thaumcraft:x:0:0:1:1:-1")) {
            assertThrows(IllegalArgumentException.class, () -> ResearchText.parse("<IMG>" + body + "</IMG>"));
        }
        assertThrows(IllegalArgumentException.class, () -> ResearchText.parse("before<IMG>missing close"));
    }
    @Test void allShippedTranslationsParseAndTheirPicturesAreExactLegacyResources() throws Exception {
        // Brass/tubes belong to Omniverse; the three research-assistance stages add one page each.
        // B22 supplies the missing Chinese TUBES.4 page; the legacy English page already exists.
        int[] expectedText = {313, 312}; int[] expectedPictures = {29, 28}; int[] expectedRules = {21,25}; int localeIndex = 0;
        var textures = new java.util.HashSet<String>();
        for (String locale : List.of("en_us", "zh_cn")) {
            try (var reader = new InputStreamReader(tc4revival.resources.ExternalTestResources.open("/assets/thaumcraft/lang/" + locale + ".json"), StandardCharsets.UTF_8)) {
                var language = JsonParser.parseReader(reader).getAsJsonObject(); int texts = 0, pictures = 0, rules = 0;
                assertFalse(language.has("tc.research_page.ALCHEMICALBRASS.1"));
                assertFalse(language.has("tc.research_page.CONVENIENTTUBES.1"));
                for (String stage : List.of("REVIVAL_RESEARCH_ASSISTANCE", "REVIVAL_ASPECT_RELATIONS", "REVIVAL_RESEARCH_CONNECTIONS", "REVIVAL_LISTENING_GUIDANCE"))
                    assertTrue(language.has("tc.research_page." + stage + ".1"), stage);
                for (var entry : language.entrySet()) if (entry.getKey().startsWith("tc.research_page.")) {
                    texts++;
                    for (var part : ResearchText.parse(entry.getValue().getAsString())) {
                        if (part instanceof Picture picture) { textures.add(picture.texture()); pictures++; }
                        if (part instanceof Rule) rules++;
                    }
                }
                assertEquals(expectedText[localeIndex], texts); assertEquals(expectedPictures[localeIndex], pictures); assertEquals(expectedRules[localeIndex], rules);
            }
            localeIndex++;
        }
        textures.add("thaumcraft:textures/gui/gui_researchbook.png");
        for (String texture : textures) {
            var resource = "assets/" + texture.replace(':', '/');
            try (var stream = tc4revival.resources.ExternalTestResources.open("/" + resource)) {
                assertNotNull(stream, texture);
                assertArrayEquals(Files.readAllBytes(Path.of("references/resources").resolve(resource)), stream.readAllBytes(), texture);
            }
        }
    }
    @Test void parsedPartsAreImmutableAndDoNotUseSharedInsertionState() {
        var first = ResearchText.parse("one<LINE>two");
        ResearchText.parse("other<IMG>thaumcraft:x.png:0:0:16:16:1</IMG>");
        assertEquals(new Rule(), first.get(2));
        assertThrows(UnsupportedOperationException.class, first::clear);
    }
}
