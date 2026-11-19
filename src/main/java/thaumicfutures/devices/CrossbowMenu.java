package thaumicfutures.devices;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class CrossbowMenu extends AbstractContainerMenu {
    public final AutomatedCrossbow turret;
    public CrossbowMenu(int id,Inventory inventory,FriendlyByteBuf buffer) { this(id,inventory,(AutomatedCrossbow)inventory.player.level().getEntity(buffer.readInt())); }
    public CrossbowMenu(int id,Inventory inventory,AutomatedCrossbow turret) {
        super(Devices.CROSSBOW_MENU.get(),id);this.turret=java.util.Objects.requireNonNull(turret);
        addSlot(new Slot(turret,0,80,29) { @Override public boolean mayPlace(ItemStack stack) { return AutomatedCrossbow.accepts(stack); } });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));
    }
    @Override public boolean stillValid(Player player) { return turret.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(!stillValid(player) || index<0 || index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index==0) { if(!moveItemStackTo(stack,1,slots.size(),true))return ItemStack.EMPTY; }
        else if(AutomatedCrossbow.accepts(stack)) { if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY; }
        else if(index<28) { if(!moveItemStackTo(stack,28,37,false))return ItemStack.EMPTY; }
        else if(!moveItemStackTo(stack,1,28,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();
        if(stack.getCount()==original.getCount())return ItemStack.EMPTY;
        slot.onTake(player,stack);return original;
    }
}
