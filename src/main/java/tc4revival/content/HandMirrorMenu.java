package tc4revival.content;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public  class HandMirrorMenu extends AbstractContainerMenu {
    private final Player owner;
    private final ItemStack mirror;
    private final SimpleContainer input=new SimpleContainer(1);
    private boolean processing,closed;
    public HandMirrorMenu(int id,Inventory inventory,FriendlyByteBuf buffer) { this(id,inventory,buffer.readEnum(InteractionHand.class)); }
    public HandMirrorMenu(int id,Inventory inventory,InteractionHand hand) {
        super(TCArcane.HAND_MIRROR_MENU.get(),id); owner=inventory.player; mirror=owner.getItemInHand(hand);
        input.addListener(ignored->slotsChanged(input));
        addSlot(new Slot(input,0,80,24));
        for (int row=0;row<3;row++) for (int col=0;col<9;col++) addSlot(new Slot(inventory,9+row*9+col,8+col*18,84+row*18));
        for (int col=0;col<9;col++) addSlot(new Slot(inventory,col,8+col*18,142));
    }
    @Override public boolean stillValid(Player player) { return !closed && player.isAlive(); }
    @Override public void slotsChanged(Container container) {
        if (processing || closed || owner.level().isClientSide) return;
        var stack=input.getItem(0); if (stack.isEmpty()) return;
        processing=true;
        try {
            if (ItemStack.matches(stack,mirror)) {
                // Close through the real lifecycle so the input is returned instead of abandoning its slot.
                owner.closeContainer();
            } else if (HandMirrorItem.transport(mirror,stack,owner)) input.removeItemNoUpdate(0);
        } finally { processing=false; }
        broadcastChanges();
    }
    @Override public void clicked(int slot,int button,ClickType type,Player player) {
        if (!stillValid(player) || type==ClickType.SWAP) return;
        super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if (!stillValid(player) || index<0 || index>=slots.size()) return ItemStack.EMPTY;
        var slot=slots.get(index); var stack=slot.getItem();
        if (stack.isEmpty() || stack.getItem() instanceof HandMirrorItem) return ItemStack.EMPTY;
        var original=stack.copy();
        if (!(index==0 ? moveItemStackTo(stack,1,37,true) : moveItemStackTo(stack,0,1,false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged(); slot.onTake(player,stack); return original;
    }
    @Override public void removed(Player player) {
        if (closed) return;
        closed=true; super.removed(player);
        if (!player.level().isClientSide) { var stack=input.removeItemNoUpdate(0); if (!stack.isEmpty()) player.drop(stack,false); }
    }
}
