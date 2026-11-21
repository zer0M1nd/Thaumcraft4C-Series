package tc4revival.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchPopupMotionTest {
    @Test void entersAndLeavesSymmetricallyAndHoldsStillInBetween() {
        assertEquals(-36, ResearchPopupMotion.offset(0));
        assertEquals(-36, ResearchPopupMotion.offset(3000));
        assertEquals(-2, ResearchPopupMotion.offset(187));
        assertEquals(-2, ResearchPopupMotion.offset(2813));
        for (int elapsed = 375; elapsed <= 2625; elapsed++) assertEquals(0, ResearchPopupMotion.offset(elapsed));
        for (int elapsed = 0; elapsed <= 3000; elapsed++) {
            assertTrue(ResearchPopupMotion.offset(elapsed) >= -36);
            assertTrue(ResearchPopupMotion.offset(elapsed) <= 0);
        }
    }
}
