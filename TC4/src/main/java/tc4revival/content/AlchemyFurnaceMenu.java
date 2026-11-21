package tc4revival.content;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public  class AlchemyFurnaceMenu extends AbstractContainerMenu {
    private final Container contents;
    private final ContainerData data;
    public AlchemyFurnaceMenu(int id,Inventory inventory,FriendlyByteBuf buffer) { this(id,inventory,new SimpleContainer(2),new SimpleContainerData(5)); buffer.readBlockPos(); }
    public AlchemyFurnaceMenu(int id,Inventory inventory,Container contents,ContainerData data) {
        super(TCArcane.ALCHEMY_FURNACE_MENU.get(),id); this.contents=contents; this.data=data;
        addSlot(new Slot(contents,0,80,8) { @Override public boolean mayPlace(ItemStack stack) { return inventory.player.level().isClientSide || !AlchemyFurnaceBlockEntity.inputAspects(stack).isEmpty(); } });
        // The old GUI fuel slot was unrestricted; automation still checks actual fuel.
        addSlot(new Slot(contents,1,80,48));
        for (int row=0;row<3;row++) for (int col=0;col<9;col++) addSlot(new Slot(inventory,9+row*9+col,8+col*18,84+row*18));
        for (int col=0;col<9;col++) addSlot(new Slot(inventory,col,8+col*18,142)); addDataSlots(data);
    }
    public int value(int index) { return data.get(index); }
    @Override public boolean stillValid(Player player) { return contents.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        // Aspect data is owned by server reloads. Let its response drive shift-click on a remote client.
        if (player.level().isClientSide) return ItemStack.EMPTY;
        if (index<0 || index>=slots.size()) return ItemStack.EMPTY;
        var slot=slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack=slot.getItem(); var original=stack.copy(); boolean moved;
        if (index<2) moved=moveItemStackTo(stack,2,38,false);
        else if (AlchemyFurnaceBlockEntity.fuelTime(stack)>0) moved=moveItemStackTo(stack,1,2,false) || moveItemStackTo(stack,0,1,false);
        else if (!AlchemyFurnaceBlockEntity.inputAspects(stack).isEmpty()) moved=moveItemStackTo(stack,0,1,false);
        else moved=index<29 ? moveItemStackTo(stack,29,38,false) : moveItemStackTo(stack,2,29,false);
        if (!moved || stack.getCount()==original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged(); slot.onTake(player,stack); return original;
    }
}
