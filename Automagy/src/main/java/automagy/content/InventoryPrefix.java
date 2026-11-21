package automagy.content;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/** Restricts a handler to slots that fit inside the Inventarium's reading allowance. */
public final class InventoryPrefix implements IItemHandler {
    private final IItemHandler inventory;
    private final int slots;
    public InventoryPrefix(IItemHandler inventory,int slots){this.inventory=inventory;this.slots=Math.max(0,slots);}
    @Override public int getSlots(){return Math.min(slots,inventory.getSlots());}
    private void check(int slot){if(slot<0||slot>=getSlots())throw new IndexOutOfBoundsException(slot);}
    @Override public ItemStack getStackInSlot(int slot){check(slot);return inventory.getStackInSlot(slot);}
    @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){check(slot);return inventory.insertItem(slot,stack,simulate);}
    @Override public ItemStack extractItem(int slot,int amount,boolean simulate){check(slot);return inventory.extractItem(slot,amount,simulate);}
    @Override public int getSlotLimit(int slot){check(slot);return inventory.getSlotLimit(slot);}
    @Override public boolean isItemValid(int slot,ItemStack stack){check(slot);return inventory.isItemValid(slot,stack);}
}
