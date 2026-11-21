package tc4revival.content;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import net.minecraft.nbt.CompoundTag;
import tc4revival.aspect.Aspect;

/** Wand charge is stored in hundredths under the original primal aspect NBT keys. */
public  class WandVis {
    private WandVis() {}
    public static int capacity(int rodCapacity, boolean sceptre) {
        if (rodCapacity < 0) throw new IllegalArgumentException("Negative wand capacity");
        return Math.multiplyExact(rodCapacity, sceptre ? 150 : 100);
    }
    public static int stored(CompoundTag tag, Aspect aspect) { return tag == null ? 0 : Math.max(0, tag.getInt(aspect.tag())); }
    public static float modifier(float cap, float equipmentDiscount, int frugal, boolean hasFocus, boolean crafting, boolean sceptre) {
        if (!Float.isFinite(cap) || !Float.isFinite(equipmentDiscount) || frugal < 0)
            throw new IllegalArgumentException("Invalid vis discount");
        float result = cap - equipmentDiscount;
        if (hasFocus && !crafting) result -= frugal / 10f;
        if (sceptre) result -= .1f;
        return Math.max(result, .1f);
    }
    public static int add(CompoundTag tag, Aspect aspect, int amount, int capacity, boolean wholeVis, boolean commit) {
        if (amount < 0 || capacity < 0) throw new IllegalArgumentException("Negative vis transfer");
        if (!aspect.isPrimal()) return 0;
        long total = (long) stored(tag, aspect) + (wholeVis ? amount * 100L : amount);
        long remainder = Math.max(0, total - capacity);
        int leftover = Math.toIntExact(wholeVis ? remainder / 100 : remainder);
        if (commit) tag.putInt(aspect.tag(), (int) Math.min(total, capacity));
        return leftover;
    }
    /** Checks every discounted component before any debit; client previews pass commit=false. */
    public static boolean consume(CompoundTag tag, Map<Aspect, Integer> requested, ToDoubleFunction<Aspect> modifiers,
            boolean wholeVis, boolean commit) {
        if (requested.isEmpty()) return false;
        var costs = new LinkedHashMap<Aspect, Integer>();
        requested.forEach((aspect, amount) -> {
            if (!aspect.isPrimal() || amount < 0) throw new IllegalArgumentException("Invalid wand cost");
            int raw = wholeVis ? ArcaneRecipe.hundredths(requested,aspect) : amount;
            float modifier = (float) modifiers.applyAsDouble(aspect);
            if (!Float.isFinite(modifier) || modifier < .1f) throw new IllegalArgumentException("Invalid wand modifier");
            float cost = raw * modifier;
            if (!Float.isFinite(cost) || cost >= 2147483648f) throw new IllegalArgumentException("Vis cost overflow");
            costs.put(aspect, (int) cost);
        });
        for (var entry : costs.entrySet()) if (stored(tag, entry.getKey()) < entry.getValue()) return false;
        if (commit) costs.forEach((aspect, cost) -> tag.putInt(aspect.tag(), stored(tag, aspect) - cost));
        return true;
    }
}
