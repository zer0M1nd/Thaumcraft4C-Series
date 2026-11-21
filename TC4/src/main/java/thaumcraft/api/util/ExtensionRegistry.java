package thaumcraft.api.util;

import java.util.*;

/** Setup-time extension registrations with immutable, deterministically ordered reads. */
public class ExtensionRegistry<T> {
    public record Entry<T>(String id, int priority, T value) {}
    private final Map<String, Entry<T>> entries = new HashMap<>();
    private volatile List<Entry<T>> snapshot = List.of();

    public synchronized void register(String id, int priority, T value) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
            throw new IllegalArgumentException("Namespaced extension ID required: " + id);
        var entry = new Entry<>(id, priority, Objects.requireNonNull(value));
        if (entries.putIfAbsent(id, entry) != null)
            throw new IllegalArgumentException("Duplicate extension " + id);
        var ordered = new ArrayList<>(entries.values());
        ordered.sort(Comparator.<Entry<T>>comparingInt(Entry::priority).reversed().thenComparing(Entry::id));
        snapshot = List.copyOf(ordered);
    }

    public List<Entry<T>> entries() { return snapshot; }
}
