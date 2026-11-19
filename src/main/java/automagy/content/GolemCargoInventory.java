package automagy.content;

import java.util.function.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import tc4revival.content.GolemEntity;

/** A sided-transfer-compatible view of the golem's real, persisted carried stack. */
public final class GolemCargoInventory implements IItemHandler {
    private final Supplier<ItemStack> read;
    private final Consumer<ItemStack> write;
    private final IntSupplier capacity;
    public GolemCargoInventory(GolemEntity golem){this(golem::carried,golem::carried,()->golem.configuration().carryLimit());}
    public GolemCargoInventory(Supplier<ItemStack> read,Consumer<ItemStack> write,IntSupplier capacity){this.read=read;this.write=write;this.capacity=capacity;}
    @Override public int getSlots(){return 1;}
    private static void check(int slot){if(slot!=0)throw new IndexOutOfBoundsException(slot);}
    @Override public ItemStack getStackInSlot(int slot){check(slot);return read.get().copy();}
    @Override public int getSlotLimit(int slot){check(slot);return Math.max(0,capacity.getAsInt());}
    @Override public boolean isItemValid(int slot,ItemStack stack){check(slot);return !stack.isEmpty();}
    @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){
        check(slot);if(stack.isEmpty())return ItemStack.EMPTY;
        ItemStack held=read.get();if(!held.isEmpty()&&!ItemStack.isSameItemSameTags(held,stack))return stack.copy();
        int limit=Math.min(getSlotLimit(slot),stack.getMaxStackSize());int accepted=Math.min(stack.getCount(),Math.max(0,limit-held.getCount()));
        if(accepted==0)return stack.copy();
        if(!simulate)write.accept(stack.copyWithCount(held.getCount()+accepted));
        return accepted==stack.getCount()?ItemStack.EMPTY:stack.copyWithCount(stack.getCount()-accepted);
    }
    @Override public ItemStack extractItem(int slot,int amount,boolean simulate){
        check(slot);if(amount<=0)return ItemStack.EMPTY;
        ItemStack held=read.get();if(held.isEmpty())return ItemStack.EMPTY;
        int extracted=Math.min(amount,held.getCount());ItemStack result=held.copyWithCount(extracted);
        if(!simulate)write.accept(extracted==held.getCount()?ItemStack.EMPTY:held.copyWithCount(held.getCount()-extracted));
        return result;
    }
}
