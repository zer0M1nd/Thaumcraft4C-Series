package temodern.essentia;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongUnaryOperator;

/** Conserves extracted resources even when a destination changes after simulation.
 * The owner persists outstanding entries and flushes them before taking more.
 */
public final class TransferLedger<K> {
    private final Map<K, Long> pending = new LinkedHashMap<>();
    public Map<K, Long> snapshot() { return Map.copyOf(pending); }
    public long amount(K key) { return pending.getOrDefault(key, 0L); }
    public void retain(K key, long count) {
        if (count < 0) throw new IllegalArgumentException("Negative transfer");
        if (count > 0) pending.put(key, Math.addExact(amount(key), count));
    }
    public long deliver(K key, LongUnaryOperator destination) {
        long available = amount(key);
        if (available == 0) return 0;
        long moved = destination.applyAsLong(available);
        if (moved < 0 || moved > available) throw new IllegalStateException("Invalid destination result");
        if (moved == available) pending.remove(key); else pending.put(key, available - moved);
        return moved;
    }
}
