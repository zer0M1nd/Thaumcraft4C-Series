package tc4revival.research.sync;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import tc4revival.research.sync.KnowledgeSnapshot.Field;
import tc4revival.research.sync.KnowledgeSnapshot.Key;

/** Paged snapshots/deltas; one complete revision becomes visible atomically. No total entry cap. */
public  class KnowledgeTransfer {
    public static final int MAX_EDITS = 128;
    public static final int PAGE_BYTES = 32768;
    public record Edit(Key key, Integer value) {
        public Edit { Objects.requireNonNull(key); }
        // Conservative allowance for enum, UTF length, removal flag and integer.
        public int wireBudget() { return 12 + key.name().getBytes(StandardCharsets.UTF_8).length; }
    }
    public record Frame(UUID owner, long revision, int index, boolean reset, boolean last, List<Edit> edits) {
        public Frame {
            Objects.requireNonNull(owner);
            edits = List.copyOf(edits);
            if (revision < 1 || index < 0 || edits.size() > MAX_EDITS) throw new IllegalArgumentException("Invalid knowledge frame");
            // One legal long UTF key may occupy a page alone. It still fits the vanilla packet limit.
            if (edits.size() > 1 && edits.stream().mapToInt(Edit::wireBudget).sum() > PAGE_BYTES) throw new IllegalArgumentException("Oversized knowledge page");
        }
    }
    private KnowledgeTransfer() { }

    public static List<Frame> create(UUID owner, long revision, KnowledgeSnapshot previous, KnowledgeSnapshot next) {
        boolean reset = previous == null || orderChanged(previous, next);
        var edits = new ArrayList<Edit>();
        if (!reset) previous.entries().keySet().stream().filter(key -> !next.entries().containsKey(key)).forEach(key -> edits.add(new Edit(key, null)));
        next.entries().forEach((key, value) -> {
            if (reset || !Objects.equals(previous.entries().get(key), value)) edits.add(new Edit(key, value));
        });
        if (edits.isEmpty() && !reset) return List.of();
        var pages = new ArrayList<List<Edit>>();
        var page = new ArrayList<Edit>();
        int bytes = 0;
        for (var edit : edits) {
            if (!page.isEmpty() && (page.size() == MAX_EDITS || bytes + edit.wireBudget() > PAGE_BYTES)) {
                pages.add(List.copyOf(page)); page.clear(); bytes = 0;
            }
            page.add(edit); bytes += edit.wireBudget();
        }
        if (!page.isEmpty() || pages.isEmpty()) pages.add(List.copyOf(page));
        var result = new ArrayList<Frame>();
        for (int i = 0; i < pages.size(); i++) result.add(new Frame(owner, revision, i, reset, i == pages.size() - 1, pages.get(i)));
        return List.copyOf(result);
    }

    private static boolean orderChanged(KnowledgeSnapshot previous, KnowledgeSnapshot next) {
        for (Field field : Field.values()) {
            var expected = new ArrayList<>(previous.keys(field).stream().filter(next.entries()::containsKey).toList());
            next.keys(field).stream().filter(key -> !previous.entries().containsKey(key)).forEach(expected::add);
            if (!expected.equals(next.keys(field))) return true;
        }
        return false;
    }

    /** Main-thread client cache, also usable by headless protocol tests. */
    public static  class Receiver {
        private final UUID owner;
        private KnowledgeSnapshot committed;
        private long revision;
        private LinkedHashMap<Key, Integer> pending;
        private long pendingRevision;
        private int expectedIndex;
        private boolean pendingReset;
        public Receiver(UUID owner) { this.owner = Objects.requireNonNull(owner); }
        public Optional<KnowledgeSnapshot> snapshot() { return Optional.ofNullable(committed); }
        public long revision() { return revision; }
        public boolean accept(Frame frame) {
            try {
                if (!owner.equals(frame.owner()) || frame.revision() <= revision) throw new IllegalArgumentException("Stale or misaddressed knowledge frame");
                if (frame.index() == 0) {
                    if (pending != null || (!frame.reset() && (committed == null || frame.revision() != revision + 1))) {
                        throw new IllegalArgumentException("Knowledge revision was interrupted");
                    }
                    pending = new LinkedHashMap<>();
                    if (!frame.reset()) pending.putAll(committed.entries());
                    pendingRevision = frame.revision(); expectedIndex = 0; pendingReset = frame.reset();
                }
                if (pending == null || pendingRevision != frame.revision() || expectedIndex != frame.index() || pendingReset != frame.reset()) {
                    throw new IllegalArgumentException("Out-of-order knowledge page");
                }
                for (var edit : frame.edits()) {
                    if (edit.value() == null) pending.remove(edit.key());
                    else pending.put(edit.key(), edit.value());
                }
                expectedIndex++;
                if (!frame.last()) return false;
                var completed = new KnowledgeSnapshot(pending);
                committed = completed; revision = pendingRevision; pending = null;
                return true;
            } catch (RuntimeException failure) {
                pending = null;
                throw failure;
            }
        }
    }
}
