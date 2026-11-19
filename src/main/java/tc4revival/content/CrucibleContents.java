package tc4revival.content;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.AspectNbt;

/** Crucible state transitions; world spill callbacks execute in the original random-consumption order. */
public  class CrucibleContents {
    public static final int WATER_CAPACITY = 1000;
    private final AspectCatalog catalog;
    private AspectAmounts aspects = new AspectAmounts();
    private int water, heat;
    private long counter = -100;
    private boolean decaying;
    public record Change(boolean synchronize, int spillAttempts) {}
    public CrucibleContents(AspectCatalog catalog) { this.catalog = catalog; }
    public int water() { return water; }
    public int heat() { return heat; }
    public boolean boiling() { return heat > 150; }
    public AspectAmounts aspects() { return aspects.copy(); }
    public int fill(int offered, boolean commit) {
        if (offered <= 0) return 0;
        int accepted = Math.min(offered, WATER_CAPACITY - water); if (commit) water += accepted; return accepted;
    }
    public int drain(int requested, boolean commit) {
        if (requested <= 0) return 0;
        int removed = Math.min(requested, water); if (commit) water -= removed; return removed;
    }
    public float fluidHeight() {
        float base = .3f + .5f * water / WATER_CAPACITY;
        float height = base + aspects.total() / 100f * (1 - base);
        return height > 1 ? 1.001f : height == 1 ? .9999f : height;
    }
    public Change tick(boolean heated, int bellows, RandomSource random, Runnable spill) {
        return tick(heated, false, bellows, random, spill);
    }
    public Change tick(boolean heated, boolean soulHeat, int bellows, RandomSource random, Runnable spill) {
        if (bellows < 0 || bellows > 4) throw new IllegalArgumentException("Invalid adjacent bellows count");
        // An unheated crucible may finish its waiting period, but never stores a backlog of decay.
        counter = Math.min(101, counter) + (decaying && soulHeat && boiling() ? 5 : 1);
        int before = heat, spills = 0; boolean synchronize = false;
        if (water > 0 && heated) {
            // TC4 tests before incrementing, so bellows can overshoot 200 by up to eight heat.
            if (heat < 200) heat += (1 + bellows * 2) * (soulHeat ? 2 : 1);
            synchronize = before < 151 && heat >= 151;
        } else if (heat > 0) {
            heat--; synchronize = water > 0 && heat == 149;
        }
        if (aspects.total() > 100 && (soulHeat || counter % 5 == 0)) {
            var entries = aspects.aspects(); aspects.remove(entries.get(random.nextInt(entries.size())), 1);
            spill.run(); spills++; synchronize = true;
        }
        if (counter > 100 && boiling()) {
            counter = decaying && soulHeat ? counter - 101 : 0;
            decaying = true; synchronize = true;
            if (!aspects.isEmpty()) {
                var entries = aspects.aspects(); var aspect = entries.get(random.nextInt(entries.size()));
                if (aspect.isPrimal()) aspect = entries.get(random.nextInt(entries.size()));
                drain(2, true); aspects.remove(aspect, 1);
                if (aspect.isPrimal()) { spill.run(); spills++; }
                else aspects.add(catalog.require(aspect.components().get(random.nextBoolean() ? 0 : 1)), 1);
            }
        }
        return new Change(synchronize, spills);
    }
    /** One dropped item dissolves at a time. The caller debits that item before producing any later output. */
    public boolean dissolve(AspectAmounts item) {
        if (item.isEmpty()) return false;
        var next = aspects.copy();
        for (var entry : item.view().entrySet()) {
            if (entry.getValue() < 0) return false;
            if ((long) next.amount(entry.getKey()) + entry.getValue() > Integer.MAX_VALUE) return false;
            next.add(entry.getKey(), entry.getValue());
        }
        aspects = next; counter = -150; decaying = false; return true;
    }
    /** TC4 accepts any positive remaining water, even less than the nominal 50 mB recipe charge. */
    public boolean pay(CrucibleRecipe recipe) {
        return pay(recipe.costs());
    }
    /** The same atomic payment path is available to processing integrations. */
    public boolean pay(AspectAmounts costs) {
        if (water <= 0) return false;
        if (costs.isEmpty() || costs.view().entrySet().stream().anyMatch(entry -> entry.getValue() < 0 || aspects.amount(entry.getKey()) < entry.getValue())) return false;
        var remainder = aspects.copy(); costs.view().forEach(remainder::remove);
        aspects = remainder; drain(50, true); counter = -250; decaying = false; return true;
    }
    /** The caller schedules these attempts rather than looping over untrusted saved quantities in one tick. */
    public long empty() {
        long spills = aspects.total() / 2; water = 0; aspects = new AspectAmounts(); decaying = false; counter = -100; return spills;
    }
    public void save(CompoundTag tag) {
        tag.putShort("Heat", (short) heat); AspectNbt.write(tag, aspects);
        tag.putLong("DecayTimer", counter); tag.putBoolean("Decaying", decaying);
        if (water == 0) { tag.putString("Empty", "Empty"); tag.remove("FluidName"); tag.remove("Amount"); }
        else { tag.remove("Empty"); tag.putString("FluidName", "water"); tag.putInt("Amount", water); }
    }
    public List<String> load(CompoundTag tag) {
        heat = Math.max(0, Math.min(216, tag.getShort("Heat")));
        String fluid = tag.getString("FluidName");
        water = tag.contains("Empty") || !(fluid.equals("water") || fluid.equals("minecraft:water")) ? 0 : Math.max(0, Math.min(WATER_CAPACITY, tag.getInt("Amount")));
        var read = AspectNbt.read(tag, catalog); aspects = new AspectAmounts();
        read.amounts().view().forEach((aspect, amount) -> { if (amount > 0) aspects.add(aspect, amount); });
        counter = tag.contains("DecayTimer") ? Math.max(-250, Math.min(101, tag.getLong("DecayTimer"))) : -100;
        decaying = tag.getBoolean("Decaying"); return read.problems();
    }
}
