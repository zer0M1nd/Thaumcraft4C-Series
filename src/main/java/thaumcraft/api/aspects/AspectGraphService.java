package thaumcraft.api.aspects;

import java.util.*;

/** Each path visits an aspect at most once. A closed cycle contributes no primal essence. */
public class AspectGraphService {
    public enum Reduction { SUM, MAXIMUM }
    private final Map<String, List<String>> graph;
    public AspectGraphService(Map<String, List<String>> definitions) {
        var copy = new LinkedHashMap<String, List<String>>();
        definitions.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        copy.forEach((key, children) -> {
            if (key.isBlank() || (!children.isEmpty() && children.size() != 2))
                throw new IllegalArgumentException("Invalid definition " + key);
            for (String child : children) if (!copy.containsKey(child))
                throw new IllegalArgumentException("Unknown component " + child);
        });
        graph = Collections.unmodifiableMap(copy);
    }
    public Map<String, Long> reduce(Map<String, Long> input, Reduction reduction) {
        return reduce(input,reduction,false);
    }
    /** Legacy signed bookkeeping retains zero entries and first-discovery order. */
    public Map<String, Long> reduceSigned(Map<String, Long> input, Reduction reduction) {
        return reduce(input,reduction,true);
    }
    private Map<String, Long> reduce(Map<String, Long> input, Reduction reduction, boolean signed) {
        var result = new LinkedHashMap<String, Long>();
        var budget = new int[]{1_000_000};
        input.forEach((key, amount) -> {
            if (!signed && amount < 0) throw new IllegalArgumentException("Negative aspect amount");
            if (signed || amount > 0) merge(result, expand(key, amount, reduction, new HashSet<>(), budget), reduction);
        });
        return Collections.unmodifiableMap(result);
    }
    private Map<String, Long> expand(String key, long amount, Reduction reduction,
                                    Set<String> path, int[] budget) {
        var children = graph.get(key);
        if (children == null) throw new IllegalArgumentException("Unknown aspect " + key);
        if (--budget[0] < 0 || path.size() > 256) throw new IllegalArgumentException("Aspect expansion exceeds safe work budget");
        if (children.isEmpty()) return Map.of(key, amount);
        if (!path.add(key)) return Map.of();
        try {
            // Preserve the base game's treatment of repeated children before applying MAXIMUM.
            var grouped = new LinkedHashMap<String, Long>();
            children.forEach(child -> grouped.merge(child, amount, Math::addExact));
            var result = new LinkedHashMap<String, Long>();
            grouped.forEach((child, count) -> merge(result, expand(child, count, reduction, path, budget), reduction));
            return result;
        } finally { path.remove(key); }
    }
    /** Weighted fold applies the cap at every compound, not just to the final sum. */
    public double weightedFold(String key, java.util.function.ToDoubleFunction<String> primalWeight, double compoundCap) {
        if (Double.isNaN(compoundCap) || compoundCap < -1) throw new IllegalArgumentException("Invalid compound cap");
        return weighted(key, primalWeight, compoundCap, new HashSet<>(), new int[]{1_000_000});
    }
    private double weighted(String key, java.util.function.ToDoubleFunction<String> weight, double cap,
                            Set<String> path, int[] budget) {
        var children=graph.get(key);
        if(children==null)throw new IllegalArgumentException("Unknown aspect " + key);
        if(--budget[0]<0 || path.size()>256)throw new IllegalArgumentException("Aspect expansion exceeds safe work budget");
        if(children.isEmpty()) {
            double value=weight.applyAsDouble(key);
            if(!Double.isFinite(value)||value<0)throw new IllegalArgumentException("Invalid primal weight " + key);
            return value;
        }
        if(!path.add(key))return 0;
        double total=0;
        try { for(var child:children)total+=weighted(child,weight,cap,path,budget); }
        finally { path.remove(key); }
        return cap<0?total:Math.min(total,cap);
    }
    private static void merge(Map<String, Long> target, Map<String, Long> source, Reduction reduction) {
        source.forEach((key, amount) -> target.merge(key, amount,
                reduction == Reduction.SUM ? Math::addExact : Math::max));
    }
}
