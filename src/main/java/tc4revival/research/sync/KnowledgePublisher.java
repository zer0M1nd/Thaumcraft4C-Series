package tc4revival.research.sync;

import java.util.UUID;
import java.util.function.Consumer;
import tc4revival.research.PlayerKnowledge;
import tc4revival.research.sync.KnowledgeTransfer.Frame;

/** One connection's last sent state. A failed send never advances the local cursor. */
public  class KnowledgePublisher {
    private KnowledgeSnapshot previous;
    private long revision;
    public long revision() { return revision; }
    public int publish(UUID owner, PlayerKnowledge knowledge, boolean reset, Consumer<Frame> send) {
        var next = KnowledgeSnapshot.capture(knowledge);
        long nextRevision = Math.incrementExact(revision);
        var frames = KnowledgeTransfer.create(owner, nextRevision, reset ? null : previous, next);
        for (var frame : frames) send.accept(frame);
        previous = next;
        if (!frames.isEmpty()) revision = nextRevision;
        return frames.size();
    }
}
