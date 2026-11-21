package tc4revival.research;

import java.util.*;
import java.util.function.ToIntFunction;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectCatalog;

/** Aggregate demand from compound to primal before reserving stock, including repeated branches. */
public class AspectSynthesis {
    public record Plan(Map<Aspect,Integer> costs, Set<Aspect> missing, boolean valid) {
        public Plan { costs = Collections.unmodifiableMap(new LinkedHashMap<>(costs)); missing = Set.copyOf(missing); }
        public boolean affordable() { return valid && missing.isEmpty(); }
    }
    public static Plan plan(Aspect target, AspectCatalog catalog, ToIntFunction<Aspect> pool, Set<String> bonuses) {
        if (target == null || target.isPrimal()) return new Plan(Map.of(), Set.of(), false);
        var costs = new LinkedHashMap<Aspect,Integer>();
        var missing = new LinkedHashSet<Aspect>();
        try {
            var depth = new HashMap<Aspect,Integer>();
            depth(target, catalog, depth, new HashSet<>());
            var order = new ArrayList<>(depth.keySet());
            order.sort(Comparator.<Aspect>comparingInt(depth::get).reversed().thenComparing(Aspect::tag));
            var demand = new HashMap<Aspect,Integer>();
            target.components().forEach(tag -> demand.merge(catalog.require(tag), 1, Math::addExact));
            for (var aspect : order) {
                int amount = demand.getOrDefault(aspect, 0);
                if (amount == 0) continue;
                long stock = Math.max(0, pool.applyAsInt(aspect)) + (bonuses.contains(aspect.tag()) ? 1L : 0L);
                int taken = (int)Math.min(amount, stock), remaining = amount - taken;
                if (aspect.isPrimal()) {
                    costs.put(aspect, amount);
                    if (remaining > 0) missing.add(aspect);
                } else {
                    if (taken > 0) costs.put(aspect, taken);
                    if (remaining > 0) for (String tag : aspect.components())
                        demand.merge(catalog.require(tag), remaining, Math::addExact);
                }
            }
            return new Plan(costs, missing, true);
        } catch (ArithmeticException | IllegalArgumentException invalid) {
            return new Plan(Map.of(), Set.of(), false);
        }
    }
    private static int depth(Aspect aspect, AspectCatalog catalog, Map<Aspect,Integer> depths, Set<Aspect> path) {
        if (depths.containsKey(aspect)) return depths.get(aspect);
        if (!path.add(aspect)) throw new IllegalArgumentException("Cyclic aspect recipe");
        int result = 0;
        for (String tag : aspect.components()) result = Math.max(result, Math.addExact(1, depth(catalog.require(tag), catalog, depths, path)));
        path.remove(aspect); depths.put(aspect, result); return result;
    }
}
