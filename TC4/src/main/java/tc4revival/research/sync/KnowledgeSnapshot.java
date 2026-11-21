package tc4revival.research.sync;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tc4revival.aspect.AspectCatalog;
import tc4revival.research.PlayerKnowledge;
import tc4revival.research.PlayerKnowledge.ScanKind;
import tc4revival.research.PlayerKnowledge.WarpKind;

/** Immutable, ordered wire values with nonnegative integer aspect balances. */
public  class KnowledgeSnapshot {
    public enum Field { ASPECT, RESEARCH, OBJECT, ENTITY, PHENOMENON, PERMANENT, TEMPORARY, STICKY, COUNTER, GUIDANCE }
    public record Key(Field field, String name) {
        public Key {
            Objects.requireNonNull(field);
            Objects.requireNonNull(name);
            if (name.length() > 32767) throw new IllegalArgumentException("Knowledge key exceeds wire string length");
            if (field.ordinal() >= Field.PERMANENT.ordinal() && field != Field.GUIDANCE && !name.isEmpty()) throw new IllegalArgumentException("Scalar knowledge key has a name");
        }
    }
    private final Map<Key, Integer> entries;

    public KnowledgeSnapshot(Map<Key, Integer> entries) {
        var copy = new LinkedHashMap<Key, Integer>();
        entries.forEach((key, value) -> {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            switch (key.field()) {
                case ASPECT -> {
                    if (value < 0) throw new IllegalArgumentException("Invalid knowledge pool");
                }
                case RESEARCH, OBJECT, ENTITY, PHENOMENON, GUIDANCE -> {
                    if (value != 0) throw new IllegalArgumentException("Invalid knowledge marker value");
                }
                case PERMANENT, TEMPORARY, STICKY -> {
                    if (value < 0) throw new IllegalArgumentException("Negative warp value");
                }
                case COUNTER -> { }
            }
            copy.put(key, value);
        });
        this.entries = Collections.unmodifiableMap(copy);
    }

    public Map<Key, Integer> entries() { return entries; }
    public List<Key> keys(Field field) { return entries.keySet().stream().filter(key -> key.field() == field).toList(); }

    public static KnowledgeSnapshot capture(PlayerKnowledge knowledge) {
        var entries = new LinkedHashMap<Key, Integer>();
        knowledge.knownAspects().forEach((aspect, amount) -> entries.put(new Key(Field.ASPECT, aspect.tag()), amount));
        knowledge.researchMarkers().forEach(key -> entries.put(new Key(Field.RESEARCH, key), 0));
        for (ScanKind kind : ScanKind.values()) knowledge.scannedKeys(kind).forEach(key -> entries.put(new Key(Field.valueOf(kind.name()), key), 0));
        for (WarpKind kind : WarpKind.values()) entries.put(new Key(Field.valueOf(kind.name()), ""), knowledge.warp(kind));
        entries.put(new Key(Field.COUNTER, ""), knowledge.warpCounter());
        knowledge.guidance().forEach((target, condition) -> entries.put(new Key(Field.GUIDANCE, target + "\n" + condition), 0));
        return new KnowledgeSnapshot(entries);
    }

    public PlayerKnowledge toKnowledge(AspectCatalog catalog) {
        var result = new PlayerKnowledge(catalog);
        entries.forEach((key, value) -> {
            switch (key.field()) {
                case ASPECT -> result.setPool(catalog.require(key.name()), value.intValue());
                case RESEARCH -> result.rememberResearch(key.name());
                case OBJECT, ENTITY, PHENOMENON -> result.rememberScan(ScanKind.valueOf(key.field().name()), key.name());
                case PERMANENT, TEMPORARY, STICKY -> result.setWarp(WarpKind.valueOf(key.field().name()), value);
                case COUNTER -> result.setWarpCounter(value);
                case GUIDANCE -> {
                    int split = key.name().indexOf('\n');
                    if (split <= 0 || split == key.name().length() - 1) throw new IllegalArgumentException("Invalid guidance marker");
                    result.rememberGuidance(key.name().substring(0, split), key.name().substring(split + 1));
                }
            }
        });
        return result;
    }
}
