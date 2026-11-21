package tmmodern;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KatanaHudStateTest {
    @Test void equipAndUnequipFadeAreClampedAndReset() {
        var state = new KatanaHudState();
        assertEquals(0, state.advance(false, 1));
        assertEquals(.5f, state.advance(true, 15));
        assertEquals(1, state.advance(true, 100));
        assertEquals(.5f, state.advance(false, 15));
        assertEquals(0, state.advance(false, 100));
        state.advance(true, 30);
        state.reset();
        assertEquals(0, state.advance(true, 0));
    }

    @Test void idleRunesBreatheAndFadeWithoutReplacingProgressLayer() {
        assertEquals(.7f, KatanaHudState.backgroundAlpha(0, 0, 1), .00001f);
        assertEquals(.35f, KatanaHudState.backgroundAlpha(0, 0, .5f), .00001f);
        assertNotEquals(KatanaHudState.backgroundAlpha(0, 0, 1), KatanaHudState.backgroundAlpha(0, 1, 1));
        for (int tick = 0; tick < 300; tick++) for (int rune = 0; rune < 16; rune++) {
            float alpha = KatanaHudState.backgroundAlpha(tick, rune, 1);
            assertTrue(alpha >= .39999f && alpha <= 1.00001f);
            assertEquals(0, KatanaHudState.backgroundAlpha(tick, rune, 0));
            assertTrue(KatanaHudState.progressAlpha(tick, rune) <= 1);
        }
    }
}
