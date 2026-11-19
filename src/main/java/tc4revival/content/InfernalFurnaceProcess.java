package tc4revival.content;

import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** TC4's 32-slot, countdown-based furnace. World effects belong to the host. */
public  class InfernalFurnaceProcess {
    public record Smelting(ItemStack output,float experience) {}
    public interface Host {
        Optional<Smelting> recipe(ItemStack input);
        int bellows();
        int drainFireVis(int requested);
        void eject(Smelting recipe,ItemStack input);
        void destroyedInput();
        void smelted();
        void changed();
    }
    private final NonNullList<ItemStack> inventory=NonNullList.withSize(32,ItemStack.EMPTY);
    private int cookTime,maxCookTime,speedyTime;
    public int cookTime() { return cookTime; }
    public int maxCookTime() { return maxCookTime; }
    public int speedyTime() { return speedyTime; }
    public ItemStack item(int slot) { return inventory.get(slot).copy(); }
    public int size() { return inventory.size(); }
    public static int duration(boolean accelerated,int bellows) { return (accelerated ? 80 : 140)-20*Math.min(3,Math.max(0,bellows)); }
    public void addFireEssentia(Host host) { speedyTime+=600; host.changed(); }

    /** Commit only when the entire incoming stack fits, using the native stack/NBT contract. */
    public boolean insert(ItemStack incoming,Host host) {
        if (incoming.isEmpty()) return false;
        if (host.recipe(incoming).isEmpty()) { host.destroyedInput(); return true; }
        var handler=new net.minecraftforge.items.ItemStackHandler(inventory);
        if (!net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(handler,incoming.copy(),true).isEmpty()) return false;
        net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(handler,incoming.copy(),false);
        host.changed(); return true;
    }
    public void tick(Host host) {
        boolean cooked=cookTime>0;
        int previousCook=cookTime,previousSpeed=speedyTime;
        if (cooked) cookTime--;
        if (cooked && speedyTime>0) speedyTime--;
        if (speedyTime<=0 && inventory.stream().anyMatch(input->!input.isEmpty() && host.recipe(input).isPresent())) speedyTime=host.drainFireVis(5);
        if (maxCookTime==0) maxCookTime=duration(speedyTime>0,host.bellows());
        if (cookTime>maxCookTime) cookTime=maxCookTime;
        if (cookTime==0 && cooked) {
            for (int slot=0;slot<inventory.size();slot++) {
                var input=inventory.get(slot); if (input.isEmpty()) continue;
                var recipe=host.recipe(input); if (recipe.isEmpty()) continue;
                host.eject(recipe.get(),input.copy()); host.smelted(); input.shrink(1);
                if (input.isEmpty()) inventory.set(slot,ItemStack.EMPTY);
                host.changed(); break;
            }
        } else if (cookTime==0) {
            for (var input:inventory) if (!input.isEmpty() && host.recipe(input).isPresent()) {
                cookTime=maxCookTime=duration(speedyTime>0,host.bellows()); break;
            }
        }
        if (previousCook!=cookTime || previousSpeed!=speedyTime) host.changed();
    }
    public void save(CompoundTag tag) {
        tag.putInt("CookTime",cookTime); tag.putInt("SpeedyTime",speedyTime); tag.putInt("MaxCookTime",maxCookTime);
        var entries=new ListTag();
        for (int slot=0;slot<inventory.size();slot++) if (!inventory.get(slot).isEmpty()) {
            var entry=inventory.get(slot).save(new CompoundTag()); entry.putByte("Slot",(byte)slot); entries.add(entry);
        }
        tag.put("Items",entries);
    }
    public void load(CompoundTag tag) {
        for (int slot=0;slot<inventory.size();slot++) inventory.set(slot,ItemStack.EMPTY);
        var entries=tag.getList("Items",Tag.TAG_COMPOUND);
        for (int i=0;i<entries.size();i++) {
            var entry=entries.getCompound(i); int slot=entry.getByte("Slot");
            if (slot>=0 && slot<inventory.size()) inventory.set(slot,ItemStack.of(entry));
        }
        cookTime=Math.max(0,tag.getInt("CookTime")); speedyTime=Math.max(0,tag.getInt("SpeedyTime"));
        maxCookTime=Math.max(cookTime,tag.getInt("MaxCookTime"));
    }
}
