package tc4revival.content;

import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

public  class ArcaneBoreMenu extends AbstractContainerMenu {
    private final ArcaneBoreBlockEntity bore;
    public ArcaneBoreMenu(int id,Inventory inventory,net.minecraft.network.FriendlyByteBuf data) {
        this(id,inventory,MenuBlockEntities.resolve(inventory,data,ArcaneBoreBlockEntity.class,TCArcane.ARCANE_BORE.get(),ArcaneBoreBlockEntity::new));
    }
    public ArcaneBoreMenu(int id,Inventory inventory,ArcaneBoreBlockEntity bore) {
        super(TCArcane.ARCANE_BORE_MENU.get(),id); this.bore=bore;
        addSlot(new Slot(bore,0,26,18) { @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof ExcavationFocusItem; } });
        addSlot(new Slot(bore,1,74,18) { @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof PickaxeItem; } });
        for (int row=0;row<3;row++) for (int col=0;col<9;col++) addSlot(new Slot(inventory,col+row*9+9,8+col*18,59+row*18));
        for (int col=0;col<9;col++) addSlot(new Slot(inventory,col,8+col*18,117));
    }
    public ArcaneBoreBlockEntity bore() { return bore; }
    @Override public boolean stillValid(Player player) { return bore.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if (!stillValid(player) || index<0 || index>=slots.size()) return ItemStack.EMPTY;
        var slot=slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack=slot.getItem(); var original=stack.copy();
        if (index<2) { if (!moveItemStackTo(stack,2,38,true)) return ItemStack.EMPTY; }
        else if (stack.getItem() instanceof ExcavationFocusItem) { if (!moveItemStackTo(stack,0,1,false)) return ItemStack.EMPTY; }
        else if (stack.getItem() instanceof PickaxeItem) { if (!moveItemStackTo(stack,1,2,false)) return ItemStack.EMPTY; }
        else return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount()==original.getCount()) return ItemStack.EMPTY;
        return original;
    }
}
