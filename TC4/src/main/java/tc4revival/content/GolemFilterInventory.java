package tc4revival.content;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

/** Ghost samples only: these stacks never enter item-handler automation or drop as physical goods. */
public  class GolemFilterInventory {
    private ItemStack[] samples={ItemStack.EMPTY};
    private byte[] colors={(byte)-1};
    private final Runnable changed;
    public GolemFilterInventory(Runnable changed) { this.changed=Objects.requireNonNull(changed); }
    public int size() { return samples.length; }
    public ItemStack sample(int slot) { return valid(slot)?samples[slot].copy():ItemStack.EMPTY; }
    public byte color(int slot) { return valid(slot)?colors[slot]:-1; }
    private boolean valid(int slot) { return slot>=0 && slot<samples.length; }
    public void sample(int slot,ItemStack stack) { if (valid(slot)) { samples[slot]=stack.copy(); changed.run(); } }
    public void color(int slot,int color) { if (valid(slot) && color>=-1 && color<=15) { colors[slot]=(byte)color; changed.run(); } }
    public void cycleColor(int slot,int delta) {
        if (!valid(slot)) return;
        int value=colors[slot]+delta; color(slot,value < -1?15:value>15?-1:value);
    }
    public void configure(GolemConfiguration config) {
        if (config.core()==null || !config.core().hasInventory) return;
        int size=config.core().filterSlots(config.upgrades(2));
        if (size==samples.length) return;
        ItemStack[] resized=new ItemStack[size]; Arrays.fill(resized,ItemStack.EMPTY);
        byte[] resizedColors=new byte[size]; Arrays.fill(resizedColors,(byte)-1);
        // The old unbounded copy could crash when loading a smaller inventory.
        System.arraycopy(samples,0,resized,0,Math.min(size,samples.length));
        System.arraycopy(colors,0,resizedColors,0,Math.min(size,colors.length));
        samples=resized; colors=resizedColors; changed.run();
    }
    public boolean isEmpty() { return Arrays.stream(samples).allMatch(ItemStack::isEmpty); }
    public List<Byte> matchingColors(ItemStack stack,GolemConfiguration config) {
        var result=new ArrayList<Byte>(); boolean empty=isEmpty();
        for (int index=0;index<samples.length;index++)
            if (empty || GolemItemMatching.matches(samples[index],stack,config)) result.add(colors[index]);
        return List.copyOf(result);
    }
    public int amountNeeded(ItemStack stack,boolean fuzzy) {
        int count=0;
        for (var sample:samples) {
            if (sample.isEmpty()) continue;
            boolean same=sample.getItem()==stack.getItem() && sample.getDamageValue()==stack.getDamageValue();
            if (fuzzy ? same || LegacyCraftingCategories.matches(sample,stack) : GolemItemMatching.matches(sample,stack,false,false,false)) count+=sample.getCount();
        }
        return count;
    }
    public List<ItemStack> itemsNeeded(boolean fuzzy) {
        var result=new ArrayList<ItemStack>();
        for (var sample:samples) {
            if (sample.isEmpty()) continue;
            var category=fuzzy?LegacyCraftingCategories.categories().stream().filter(value->sample.is(value.tag())).findFirst():Optional.<LegacyCraftingCategories.Category>empty();
            if (category.isPresent()) {
                var holders=net.minecraftforge.registries.ForgeRegistries.ITEMS.tags().getTag(category.get().tag());
                holders.forEach(item->result.add(new ItemStack(item)));
            } else result.add(sample.copy());
        }
        return result;
    }
    public void save(CompoundTag tag) {
        var list=new ListTag();
        for (int slot=0;slot<samples.length;slot++) if (!samples[slot].isEmpty()) {
            var entry=samples[slot].save(new CompoundTag()); entry.putByte("Slot",(byte)slot); list.add(entry);
        }
        tag.put("Inventory",list); tag.putByteArray("colors",colors);
    }
    public void load(CompoundTag tag,GolemConfiguration config) {
        configure(config); Arrays.fill(samples,ItemStack.EMPTY); Arrays.fill(colors,(byte)-1);
        for (Tag value:tag.getList("Inventory",Tag.TAG_COMPOUND)) {
            var entry=(CompoundTag)value; int slot=entry.getByte("Slot")&255;
            if (valid(slot)) samples[slot]=ItemStack.of(entry);
        }
        byte[] stored=tag.getByteArray("colors");
        for (int slot=0;slot<Math.min(stored.length,colors.length);slot++) if (stored[slot]>=-1 && stored[slot]<=15) colors[slot]=stored[slot];
        changed.run();
    }
}
