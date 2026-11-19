package ttmodern;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class TabletMenu extends AbstractContainerMenu {
    public final TabletEntity entity;
    public TabletMenu(int id,Inventory inventory,FriendlyByteBuf data){this(id,inventory,(TabletEntity)inventory.player.level().getBlockEntity(data.readBlockPos()));}
    public TabletMenu(int id,Inventory inventory,TabletEntity entity){
        super(Tinkerer.TABLET_MENU.get(),id);this.entity=java.util.Objects.requireNonNull(entity);
        addSlot(new SlotItemHandler(entity.items,0,80,15));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));
        addDataSlot(new DataSlot(){public int get(){return entity.leftClick()?1:0;}public void set(int value){entity.configure(value!=0,entity.redstone());}});
        addDataSlot(new DataSlot(){public int get(){return entity.redstone()?1:0;}public void set(int value){entity.configure(entity.leftClick(),value!=0);}});
    }
    @Override public boolean stillValid(Player player){return entity.usable(player);}
    @Override public boolean clickMenuButton(Player player,int id){if(!stillValid(player)||id<0||id>2)return false;entity.configure(id==0?true:id==1?false:entity.leftClick(),id==2?!entity.redstone():entity.redstone());return true;}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);var stack=slot.getItem();if(stack.isEmpty())return ItemStack.EMPTY;
        var result=stack.copy();if(!(index==0?moveItemStackTo(stack,1,37,false):moveItemStackTo(stack,0,1,false)))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return result;
    }
}
