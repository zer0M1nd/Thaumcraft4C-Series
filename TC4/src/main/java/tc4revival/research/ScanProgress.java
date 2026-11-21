package tc4revival.research;

import java.util.Objects;
import tc4revival.research.PlayerKnowledge.ScanMode;

/** One use of the thaumometer, advanced only by consecutive authoritative game ticks. */
public  class ScanProgress {
    public static final int USE_DURATION=25, COMPLETE_REMAINING=5;
    public static final int DURATION = USE_DURATION-COMPLETE_REMAINING;
    private final int duration;
    private final ResearchRules research;
    public ScanProgress() { this(DURATION); }
    public ScanProgress(int duration) { this(duration, null); }
    public ScanProgress(int duration, ResearchRules research) { if(duration<1)throw new IllegalArgumentException("Invalid scan duration");this.duration=duration;this.research=research; }
    public enum State { IDLE, TRACKING, INTERRUPTED, COMPLETED }
    private ScanTarget.Continuity initial;
    private String dimension;
    private long started;
    private long observed;

    public boolean active() { return initial != null; }
    public void cancel() { initial = null; }

    public State begin(long tick, String dimension, ScanTarget target, PlayerKnowledge knowledge) {
        cancel();
        if (target == null || !target.canTrack(knowledge, ScanMode.THAUMOMETER, research)) return State.IDLE;
        this.dimension = Objects.requireNonNull(dimension);
        initial = target.continuity();
        started = observed = tick;
        return State.TRACKING;
    }

    public State observe(long tick, String dimension, ScanTarget target, PlayerKnowledge knowledge) {
        if (!active()) return State.IDLE;
        if (tick < observed || tick - observed > 1 || !this.dimension.equals(dimension)
                || target == null || !initial.equals(target.continuity())
                || !target.canTrack(knowledge, ScanMode.THAUMOMETER, research)) {
            cancel();
            return State.INTERRUPTED;
        }
        observed = tick;
        if (tick - started < duration) return State.TRACKING;
        cancel();
        return State.COMPLETED;
    }
}
