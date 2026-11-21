package automagy.logic;

/** Server-owned timer; a flip reverses the amount of sand, not just the direction of the model. */
public final class HourglassTimer {
    private int target = 100, elapsed = 100, pulse;
    private int signalMode = 1, flipMode, inputMode = 1;
    private boolean input, flipped, repeat;

    public int target() { return target; }
    public int elapsed() { return elapsed; }
    public int pulse() { return pulse; }
    public int signalMode() { return signalMode; }
    public int flipMode() { return flipMode; }
    public int inputMode() { return inputMode; }
    public boolean input() { return input; }
    public boolean flipped() { return flipped; }
    public boolean repeat() { return repeat; }
    public boolean running() { return elapsed < target; }
    public boolean gated() { return inputMode == 2 && input || inputMode == 3 && !input; }
    public int remainingSeconds() { return (target - elapsed + 19) / 20; }
    public int comparator() { return running() && elapsed > 0 ? 1 + (int)(14L * elapsed / target) : 0; }
    public int signal() {
        return switch (signalMode) {
            case 1 -> pulse > 0 ? 15 : 0;
            case 2 -> flipped && running() ? 15 : 0;
            case 3 -> running() ? 15 : 0;
            case 4 -> running() && !(repeat && pulse > 0) ? 15 : 0;
            default -> 0;
        };
    }
    public void setSeconds(int seconds) {
        int next = Math.max(1, Math.min(3600, seconds)) * 20;
        elapsed = running() ? Math.min(elapsed, next - 1) : next;
        target = next;
    }
    public void configure(int signal, int flip, int reaction, boolean loop) {
        if (signal < 0 || signal > 4 || flip < 0 || flip > 2 || reaction < 0 || reaction > 3)
            throw new IllegalArgumentException("Invalid hourglass mode");
        signalMode = signal; flipMode = flip; inputMode = reaction; repeat = loop;
        if (gated() && running()) stop();
    }
    public void initialInput(boolean value) { input = value; }
    public void setInput(boolean value) {
        if (value == input) return;
        input = value;
        if (inputMode == 1 && value) flip();
        else if (inputMode == 2 || inputMode == 3) {
            if (gated()) { if (running()) stop(); }
            else flip();
        }
    }
    public boolean flip() {
        if (gated()) return false;
        flipped = !flipped;
        elapsed = switch (flipMode) {
            case 1 -> 0;
            case 2 -> running() ? target : 0;
            default -> target - elapsed;
        };
        return true;
    }
    private void stop() { elapsed = target; }
    public void tick() {
        if (pulse > 0) --pulse;
        if (gated() && running()) stop();
        if (running() && ++elapsed == target) {
            pulse = 5;
            if (repeat) flip();
        }
    }
    public void restore(int ticks, int counted, int pulseTicks, int signal, int flip, int reaction,
                        boolean powered, boolean inverted, boolean loop) {
        target = Math.max(20, Math.min(72000, ticks));
        elapsed = Math.max(0, Math.min(target, counted));
        pulse = Math.max(0, Math.min(5, pulseTicks));
        input = powered; flipped = inverted;
        configure(Math.max(0, Math.min(4, signal)), Math.max(0, Math.min(2, flip)),
                Math.max(0, Math.min(3, reaction)), loop);
    }
}
