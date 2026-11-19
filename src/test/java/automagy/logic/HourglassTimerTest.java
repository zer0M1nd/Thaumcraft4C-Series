package automagy.logic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HourglassTimerTest {
    private static void advance(HourglassTimer timer, int ticks) { for (int i=0; i<ticks; i++) timer.tick(); }
    @Test void ordinaryGlassReversesElapsedSandAndPulsesForFiveTicks() {
        var timer = new HourglassTimer();
        assertFalse(timer.running()); timer.flip(); advance(timer, 30); timer.flip();
        assertEquals(70, timer.elapsed()); advance(timer, 30);
        assertEquals(15, timer.signal()); assertEquals(0, timer.comparator());
        advance(timer, 4); assertEquals(15, timer.signal()); timer.tick(); assertEquals(0, timer.signal());
    }
    @Test void shorteningRunningTimerDoesNotSilentlyLoseCompletionPulse() {
        var timer = new HourglassTimer(); timer.flip(); advance(timer, 60); timer.setSeconds(1);
        assertEquals(19, timer.elapsed()); timer.tick(); assertEquals(15, timer.signal());
        timer.setSeconds(3601); assertEquals(72000, timer.elapsed());
    }
    @Test void negativePulseLeavesFiveTickGapBetweenCycles() {
        var timer = new HourglassTimer(); timer.setSeconds(1); timer.configure(4, 0, 1, true); timer.flip();
        assertEquals(15, timer.signal()); advance(timer, 20); assertTrue(timer.running()); assertEquals(0, timer.signal());
        advance(timer, 4); assertEquals(0, timer.signal()); timer.tick(); assertEquals(15, timer.signal());
    }
    @Test void gateStopsWithoutPulseAndOnlyRestartsOnRequiredEdge() {
        var timer = new HourglassTimer(); timer.configure(1, 0, 3, true);
        assertFalse(timer.flip()); timer.setInput(true); advance(timer, 30);
        timer.setInput(false); assertFalse(timer.running()); assertEquals(0, timer.signal());
        advance(timer, 100); assertFalse(timer.running()); timer.setInput(true); assertEquals(0, timer.elapsed());
    }
    @Test void stopModeDoesNotCompleteOrAutoRepeat() {
        var timer = new HourglassTimer(); timer.configure(1, 2, 1, true); timer.flip(); advance(timer, 10);
        timer.flip(); advance(timer, 1000); assertFalse(timer.running()); assertEquals(0, timer.signal());
    }
    @Test void alternatingOutputAndInputIgnoreRemainIndependent() {
        var timer = new HourglassTimer(); timer.configure(2, 1, 0, false); timer.flip();
        assertEquals(15, timer.signal()); advance(timer, 10); timer.setInput(true); assertEquals(10, timer.elapsed());
        timer.flip(); assertEquals(0, timer.signal()); assertTrue(timer.running());
    }
    @Test void corruptSaveIsClampedAndModesRejectInvalidPackets() {
        var timer = new HourglassTimer(); timer.restore(-1, Integer.MAX_VALUE, 123, 123, -4, 999, false, true, true);
        assertEquals(20, timer.target()); assertEquals(20, timer.elapsed()); assertEquals(5, timer.pulse());
        assertThrows(IllegalArgumentException.class, () -> timer.configure(5, 0, 1, false));
    }
}
