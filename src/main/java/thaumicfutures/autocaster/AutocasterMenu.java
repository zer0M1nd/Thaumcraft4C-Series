package thaumicfutures.autocaster;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class AutocasterMenu extends AbstractContainerMenu {
    public final AutocasterEntity turret;
    public AutocasterMenu(int id,Inventory inventory,FriendlyByteBuf buffer) {
        this(id,inventory,entity(inventory,buffer.readInt()));
    }
    private static AutocasterEntity entity(Inventory inventory,int id) {
        if(inventory.player.level().getEntity(id) instanceof AutocasterEntity turret)return turret;
        throw new IllegalArgumentException("Missing autocaster entity");
    }
    public AutocasterMenu(int id,Inventory inventory,AutocasterEntity turret) {
        super(Autocasters.MENU.get(),id);this.turret=turret;
        addSlot(new Slot(turret,0,36,29) {
            @Override public boolean mayPlace(ItemStack stack) { return AutocasterFoci.acceptsWand(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(turret,1,65,29) {
            @Override public boolean mayPlace(ItemStack stack) { return AutocasterFoci.acceptsBattery(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));
    }
    @Override public boolean stillValid(Player player) { return turret.stillValid(player); }
    @Override public boolean clickMenuButton(Player player,int button) {
        if(player.containerMenu!=this || !stillValid(player) || button<0 || button>4)return false;
        if(button==4)turret.setEnabled(!turret.enabled());
        else turret.setTargetFlags(turret.targetFlags()^(1<<button));
        return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(!stillValid(player) || index<0 || index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index<2) { if(!moveItemStackTo(stack,2,slots.size(),true))return ItemStack.EMPTY; }
        else if(AutocasterFoci.acceptsWand(stack)) { if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY; }
        else if(AutocasterFoci.acceptsBattery(stack)) { if(!moveItemStackTo(stack,1,2,false))return ItemStack.EMPTY; }
        else if(index<29) { if(!moveItemStackTo(stack,29,38,false))return ItemStack.EMPTY; }
        else if(!moveItemStackTo(stack,2,29,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();
        if(stack.getCount()==original.getCount())return ItemStack.EMPTY;
        slot.onTake(player,stack);return original;
    }
}
