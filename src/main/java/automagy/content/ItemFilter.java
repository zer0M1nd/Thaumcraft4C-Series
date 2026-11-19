package automagy.content;

import automagy.logic.FilterRules;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

/** Ghost references, never real stored inventory. Counts are ints, not ItemStack's byte Count. */
public final class ItemFilter {
    public static final int SIZE = 9, MAX_COUNT = 999, MAX_NAME = 128;
    private final ItemStack[] entries = new ItemStack[SIZE];
    private final int[] counts = new int[SIZE];
    public boolean useCount, ignoreDamage, ignoreNbt;
    private String name = "";
    public ItemFilter() { Arrays.fill(entries, ItemStack.EMPTY); Arrays.fill(counts, 1); }
    public String name() { return name; }
    public void name(String text) { name = text.substring(0, Math.min(MAX_NAME, text.length())).trim(); }
    public ItemStack entry(int slot) { return entries[slot].copy(); }
    public int count(int slot) { return counts[slot]; }
    public void count(int slot, int amount) { counts[slot] = Math.max(1, Math.min(MAX_COUNT, amount)); }
    public void entry(int slot, ItemStack stack) {
        entries[slot] = stack.copyWithCount(1);
        // Avoid recursively nesting complete filter inventories in filter inventories.
        if (entries[slot].getItem() instanceof FilterItem && entries[slot].hasTag()) entries[slot].getTag().remove("Filter");
        count(slot, stack.getCount());
    }
    public boolean populated() { return !name.isEmpty() || Arrays.stream(entries).anyMatch(s -> !s.isEmpty()); }
    public boolean matches(ItemStack reference, ItemStack candidate) {
        if (reference.isEmpty() || candidate.isEmpty() || !reference.is(candidate.getItem())) return false;
        if (!ignoreDamage && reference.getDamageValue() != candidate.getDamageValue()) return false;
        if (ignoreNbt) return true;
        CompoundTag a = reference.hasTag() ? reference.getTag().copy() : new CompoundTag();
        CompoundTag b = candidate.hasTag() ? candidate.getTag().copy() : new CompoundTag();
        if (ignoreDamage) { a.remove("Damage"); b.remove("Damage"); }
        if (reference.getItem() instanceof FilterItem) { a.remove("Filter"); b.remove("Filter"); }
        return a.equals(b);
    }
    public boolean accepts(ItemStack candidate, boolean blacklist) {
        boolean found = !name.isEmpty() && FilterRules.nameMatches(name, candidate.getHoverName().getString());
        for (ItemStack entry : entries) found |= matches(entry, candidate);
        return found != blacklist;
    }
    /** First matching reference wins, exactly as in the legacy greedy chest and finical maw. */
    public automagy.logic.PilferRules.Limit transferLimit(ItemStack candidate, boolean blacklist, long source, long destination) {
        for (int slot = 0; slot < SIZE; slot++) if (matches(entries[slot], candidate)) {
            if (!useCount) return new automagy.logic.PilferRules.Limit(blacklist ? 0 : Long.MAX_VALUE, -1);
            return blacklist ? new automagy.logic.PilferRules.Limit(source - counts[slot], 2)
                    : new automagy.logic.PilferRules.Limit(counts[slot] - destination, 3);
        }
        boolean named = !name.isEmpty() && FilterRules.nameMatches(name, candidate.getHoverName().getString());
        return new automagy.logic.PilferRules.Limit(named != blacklist ? Long.MAX_VALUE : 0, -1);
    }
    public boolean tally(List<ItemStack> inventory, boolean blacklist, boolean all) {
        return tallySnapshot(inventory.stream().filter(s->!s.isEmpty()).map(s->new InventariumContents.Entry(s,s.getCount())).toList(),blacklist,all);
    }
    /** Aggregated Inventarium counts remain longs; never construct oversized real ItemStacks. */
    public boolean tallySnapshot(List<InventariumContents.Entry> inventory,boolean blacklist,boolean all){
        List<FilterRules.Count> totals=new ArrayList<>();
        for(int slot=0;slot<SIZE;slot++)if(!entries[slot].isEmpty()){
            long total=0;
            for(var candidate:inventory)if(candidate.amount()>0&&matches(entries[slot],candidate.item()))
                total=Long.MAX_VALUE-total<candidate.amount()?Long.MAX_VALUE:total+candidate.amount();
            totals.add(new FilterRules.Count(total,counts[slot]));
        }
        boolean named=!name.isEmpty()&&inventory.stream().anyMatch(s->s.amount()>0&&!s.item().isEmpty()&&FilterRules.nameMatches(name,s.item().getHoverName().getString()));
        return FilterRules.tally(totals,blacklist,useCount,all,!name.isEmpty(),named);
    }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag(); ListTag list = new ListTag();
        for (int i = 0; i < SIZE; i++) if (!entries[i].isEmpty()) {
            CompoundTag entry = new CompoundTag(); entry.putInt("Slot", i); entry.putInt("Threshold", counts[i]);
            entry.put("Item", entries[i].save(new CompoundTag())); list.add(entry);
        }
        tag.put("Entries", list); tag.putBoolean("Count", useCount); tag.putBoolean("Damage", ignoreDamage);
        tag.putBoolean("Nbt", ignoreNbt); tag.putString("Name", name); return tag;
    }
    public static ItemFilter load(ItemStack stack) { return load(stack.hasTag() ? stack.getTag().getCompound("Filter") : new CompoundTag()); }
    public static ItemFilter load(CompoundTag tag) {
        ItemFilter filter = new ItemFilter(); ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(SIZE, list.size()); i++) {
            CompoundTag entry = list.getCompound(i); int slot = entry.getInt("Slot");
            if (slot < 0 || slot >= SIZE) continue;
            filter.entry(slot, ItemStack.of(entry.getCompound("Item"))); filter.count(slot, entry.getInt("Threshold"));
        }
        filter.useCount = tag.getBoolean("Count"); filter.ignoreDamage = tag.getBoolean("Damage");
        filter.ignoreNbt = tag.getBoolean("Nbt"); filter.name(tag.getString("Name")); return filter;
    }
}
