package tc4revival.content;

import java.util.LinkedHashMap;
import java.util.Map;

/** Base enchantment formulas, separated from item eligibility and wand payment. */
public  class LegacyEnchantmentRules {
    public enum Kind {
        HASTE(3, 3, 15, 9), REPAIR(2, 2, 20, 10);
        private final int levels, weight, minimum, step;
        Kind(int levels, int weight, int minimum, int step) {
            this.levels = levels; this.weight = weight; this.minimum = minimum; this.step = step;
        }
        public int maxLevel() { return levels; }
        public int weight() { return weight; }
        public int minCost(int level) { return minimum + (level - 1) * step; }
        public int maxCost(int level) { return minCost(level) + 50; }
    }
    private LegacyEnchantmentRules() {}
    public static float hasteAcceleration(int level, boolean forward, boolean flying, boolean airborne, boolean water) {
        if (level <= 0 || !forward || flying) return 0;
        float bonus = level * .015f;
        if (airborne) bonus /= 2;
        if (water) bonus /= 2;
        return bonus;
    }
    static int primalRepairCost(int amount, int level, int restored) {
        if (level <= 0 || restored <= 0 || amount <= 0) return 0;
        long fullCost = (long) Math.sqrt(amount * 2.0);
        // Vis is stored in hundredths. Round a fractional debit up to one payable unit.
        return (int) ((fullCost * Math.min(level, restored) + level - 1L) / level);
    }
    /** Input must already be reduced to primal aspects by the aspect system. No inventory is debited here. */
    public static Map<String, Integer> repairCost(Map<String, Integer> primals, int enchantmentLevel) {
        if (enchantmentLevel <= 0) return Map.of();
        var costs = new LinkedHashMap<String, Integer>();
        primals.forEach((aspect, amount) -> costs.put(aspect, primalRepairCost(amount, enchantmentLevel, enchantmentLevel)));
        return java.util.Collections.unmodifiableMap(costs);
    }
}
