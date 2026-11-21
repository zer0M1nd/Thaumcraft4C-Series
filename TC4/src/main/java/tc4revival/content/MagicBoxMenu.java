package tc4revival.content;

import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public  class MagicBoxMenu extends AbstractContainerMenu {
    private final MagicBoxBlockEntity box;
    private final boolean client;
    private final ContainerData pages=new SimpleContainerData(2);
    public int page() { return pages.get(0); }
    public int pageCount() { return Math.max(1,pages.get(1)); }
    public MagicBoxMenu(int id,Inventory inventory,net.minecraft.network.FriendlyByteBuf data) {
        this(id,inventory,MenuBlockEntities.resolve(inventory,data,MagicBoxBlockEntity.class,TCArcane.MAGIC_BOX.get(),MagicBoxBlockEntity::new));
    }
    public MagicBoxMenu(int id,Inventory inventory,MagicBoxBlockEntity box) {
        super(TCArcane.MAGIC_BOX_MENU.get(),id);this.box=box;this.client=inventory.player.level().isClientSide;
        pages.set(1,Math.max(1,(box.getContainerSize()+26)/27));addDataSlots(pages);
        var window=client?new net.minecraft.world.SimpleContainer(27):new net.minecraft.world.SimpleContainer(27) {
            private int actual(int slot) { return page()*27+slot; }
            @Override public ItemStack getItem(int slot) { return box.getItem(actual(slot)); }
            @Override public ItemStack removeItem(int slot,int count) { return box.removeItem(actual(slot),count); }
            @Override public ItemStack removeItemNoUpdate(int slot) { return box.removeItemNoUpdate(actual(slot)); }
            @Override public void setItem(int slot,ItemStack stack) { box.setItem(actual(slot),stack); }
            @Override public void setChanged() { box.setChanged(); }
            @Override public boolean stillValid(Player player) { return box.stillValid(player); }
        };
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(window,row*9+col,8+col*18,18+row*18));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));
    }
    @Override public boolean stillValid(Player player) { return box.stillValid(player); }
    @Override public void broadcastChanges() {
        if(!client) {pages.set(1,Math.max(1,(box.getContainerSize()+26)/27));
        pages.set(0,Math.min(page(),pageCount()-1));}super.broadcastChanges();
    }
    @Override public boolean clickMenuButton(Player player,int button) {
        if(player.level().isClientSide || player.containerMenu!=this || !stillValid(player) || button<0 || button>1)return false;
        int next=page()+(button==0?-1:1);
        if(next<0 || next>=pageCount())return false;
        pages.set(0,next);broadcastChanges();return true;
    }
    @Override public void removed(Player player) { super.removed(player);box.sortContents(); }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);var stack=slot.getItem();if(stack.isEmpty())return ItemStack.EMPTY;
        var original=stack.copy();
        // The 27 visible slots are a window over the current storage page.
        if(!(index<27?moveItemStackTo(stack,27,63,true):moveItemStackTo(stack,0,27,false)))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();return original;
    }
}
