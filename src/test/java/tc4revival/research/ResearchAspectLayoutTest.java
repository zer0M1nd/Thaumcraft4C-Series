package tc4revival.research;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchAspectLayoutTest {
    @Test void singleAndTwoAspectsUseLegacyCenteringAndAlphabeticalOrder() {
        assertEquals(List.of(new ResearchAspectLayout.Glyph("aer", 7, 48, 128)),
                ResearchAspectLayout.grid(Map.of("aer", 7), 3, 28, 128));
        var costs = new LinkedHashMap<String, Integer>();
        costs.put("terra", 4); costs.put("aer", 2);
        assertEquals(List.of(new ResearchAspectLayout.Glyph("aer", 2, 38, 128),
                new ResearchAspectLayout.Glyph("terra", 4, 58, 128)),
                ResearchAspectLayout.grid(costs, 3, 28, 128));
        assertEquals(List.of("terra", "aer"), List.copyOf(costs.keySet()));
    }

    @Test void secondPartialRowRetainsOldLeftAlignment() {
        var glyphs = ResearchAspectLayout.grid(costs(4), 3, 28, 128);
        assertEquals(new ResearchAspectLayout.Glyph("a", 1, 28, 118), glyphs.get(0));
        assertEquals(new ResearchAspectLayout.Glyph("d", 4, 28, 138), glyphs.get(3));
    }

    @Test void thirdRowRetainsOldShiftEvenWhenFull() {
        var partial = ResearchAspectLayout.grid(costs(7), 3, 28, 128);
        assertEquals(new ResearchAspectLayout.Glyph("g", 7, 48, 148), partial.get(6));
        var full = ResearchAspectLayout.grid(costs(9), 3, 28, 128);
        assertEquals(new ResearchAspectLayout.Glyph("g", 7, 58, 148), full.get(6));
        assertEquals(new ResearchAspectLayout.Glyph("i", 9, 98, 148), full.get(8));
    }

    @Test void emptyCostsAndInvalidColumnCount() {
        assertTrue(ResearchAspectLayout.grid(Map.of(), 3, 28, 128).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> ResearchAspectLayout.grid(Map.of(), 0, 0, 0));
    }

    private static Map<String, Integer> costs(int count) {
        var result = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < count; i++) result.put(Character.toString((char) ('a' + i)), i + 1);
        return result;
    }
}
