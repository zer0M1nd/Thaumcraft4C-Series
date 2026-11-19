package tc4revival.content;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public  class ArcaneSpaMenu extends AbstractContainerMenu {
    private final ArcaneSpaBlockEntity spa;
    public ArcaneSpaMenu(int id,Inventory inventory,net.minecraft.network.FriendlyByteBuf data) {
        this(id,inventory,MenuBlockEntities.resolve(inventory,data,ArcaneSpaBlockEntity.class,TCArcane.ARCANE_SPA.get(),ArcaneSpaBlockEntity::new));
    }
    public ArcaneSpaMenu(int id,Inventory inventory,ArcaneSpaBlockEntity spa) {
        super(TCArcane.ARCANE_SPA_MENU.get(),id);this.spa=spa;
        addSlot(new Slot(spa,0,65,31) { @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof BathSaltsItem; } });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));
    }
    public ArcaneSpaBlockEntity spa() { return spa; }
    @Override public boolean stillValid(Player player) { return spa.stillValid(player); }
    @Override public boolean clickMenuButton(Player player,int button) { if(button!=1||player.level().isClientSide||player.containerMenu!=this||!stillValid(player))return false;spa.toggleMix();return true; }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);var stack=slot.getItem();if(stack.isEmpty()||!(stack.getItem() instanceof BathSaltsItem))return ItemStack.EMPTY;
        var original=stack.copy();if(!(index==0?moveItemStackTo(stack,1,37,true):moveItemStackTo(stack,0,1,false)))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();return original;
    }
}
