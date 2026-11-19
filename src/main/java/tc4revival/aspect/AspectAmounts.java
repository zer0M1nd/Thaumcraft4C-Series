package tc4revival.aspect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Ordered quantities, including meaningful zero entries such as jar labels and drained nodes. */
public  class AspectAmounts {
    private final LinkedHashMap<Aspect, Integer> values = new LinkedHashMap<>();

    public int size() { return values.size(); }
    public boolean isEmpty() { return values.isEmpty(); }
    public boolean contains(Aspect aspect) { return values.containsKey(aspect); }
    public int amount(Aspect aspect) { return values.getOrDefault(aspect, 0); }
    public long total() { return values.values().stream().mapToLong(Integer::longValue).sum(); }
    public List<Aspect> aspects() { return List.copyOf(values.keySet()); }
    public List<Aspect> primals() { return values.keySet().stream().filter(Aspect::isPrimal).toList(); }
    public Map<Aspect, Integer> view() { return Collections.unmodifiableMap(values); }

    public AspectAmounts copy() {
        var result = new AspectAmounts();
        result.values.putAll(values);
        return result;
    }

    public AspectAmounts add(Aspect aspect, int quantity) {
        Objects.requireNonNull(aspect);
        values.put(aspect, Math.addExact(amount(aspect), quantity));
        return this;
    }

    /** The old merge operation takes a per-aspect maximum, not a sum. */
    public AspectAmounts merge(Aspect aspect, int quantity) {
        Objects.requireNonNull(aspect);
        values.merge(aspect, quantity, Math::max);
        return this;
    }

    public AspectAmounts add(AspectAmounts other) { return combine(other, false); }
    public AspectAmounts merge(AspectAmounts other) { return combine(other, true); }

    private AspectAmounts combine(AspectAmounts other, boolean maximum) {
        // Calculate first so an overflow cannot leave a half-applied transfer.
        var next = copy();
        other.values.forEach((aspect, amount) -> {
            if (maximum) next.merge(aspect, amount); else next.add(aspect, amount);
        });
        values.clear();
        values.putAll(next.values);
        return this;
    }

    /** A successful reduction retains zero; a failed reduction does not change the list. */
    public boolean reduce(Aspect aspect, int quantity) {
        Objects.requireNonNull(aspect);
        int available = amount(aspect);
        if (available < quantity) return false;
        values.put(aspect, Math.subtractExact(available, quantity));
        return true;
    }

    /** Removing a quantity discards its entry when the remainder is zero or negative. */
    public AspectAmounts remove(Aspect aspect, int quantity) {
        Objects.requireNonNull(aspect);
        int remainder = Math.subtractExact(amount(aspect), quantity);
        if (remainder > 0) values.put(aspect, remainder); else values.remove(aspect);
        return this;
    }

    public AspectAmounts remove(Aspect aspect) { values.remove(aspect); return this; }

    public List<Aspect> alphabetical() {
        return values.keySet().stream().sorted(Comparator.comparing(Aspect::tag)).toList();
    }

    public List<Aspect> byAmount() {
        var result = new ArrayList<>(aspects());
        result.sort(Comparator.comparingInt(this::amount).reversed());
        return List.copyOf(result);
    }
}
