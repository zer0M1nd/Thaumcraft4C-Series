package forgottenrelics;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExperienceMathTest {
    @Test
    void vanillaBreakpoints() {
        assertEquals(352, ExperienceMath.atLevel(16));
        assertEquals(394, ExperienceMath.atLevel(17));
        assertEquals(1507, ExperienceMath.atLevel(31));
        assertEquals(1628, ExperienceMath.atLevel(32));
    }

    @Test
    void roundTripsWholeAndPartialLevels() {
        for (int level = 0; level < 1000; level++) {
            long start = ExperienceMath.atLevel(level), end = ExperienceMath.atLevel(level + 1);
            assertEquals(end - start, ExperienceMath.toNext(level));
            assertEquals(level, ExperienceMath.level(start));
            assertEquals(level, ExperienceMath.level(end - 1));
            assertEquals(level + 1, ExperienceMath.level(end));
        }
    }

    @Test
    void negativeAndLargeTotals() {
        assertEquals(0, ExperienceMath.level(-20));
        assertEquals(0, ExperienceMath.atLevel(-1));
        assertTrue(ExperienceMath.level(Integer.MAX_VALUE) > 20000);
        assertTrue(ExperienceMath.atLevel(1_000_000) > Integer.MAX_VALUE);
    }
}
