package tc4revival.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchMiddleGestureTest {
    @Test void clickAndJitterSearchButDragEvenBackToStartDoesNot() {
        assertTrue(new ResearchMiddleGesture(10,20).click(10,20));
        var jitter=new ResearchMiddleGesture(10,20);
        assertFalse(jitter.moved(11,21));assertTrue(jitter.click(12,21));
        var drag=new ResearchMiddleGesture(10,20);
        assertTrue(drag.moved(25,20));assertFalse(drag.click(10,20));
        assertFalse(new ResearchMiddleGesture(10,20).click(25,20));
    }
}
