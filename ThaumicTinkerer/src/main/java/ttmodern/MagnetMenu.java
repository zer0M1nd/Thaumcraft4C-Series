package ttmodern;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class MagnetMenu extends AbstractContainerMenu {
    public final MagnetEntity entity;
    public MagnetMenu(int id,Inventory inventory,FriendlyByteBuf data){this(id,inventory,(MagnetEntity)inventory.player.level().getBlockEntity(data.readBlockPos()));}
    public MagnetMenu(int id,Inventory inventory,MagnetEntity entity){
        super(Tinkerer.MAGNET_MENU.get(),id);this.entity=java.util.Objects.requireNonNull(entity);
        addSlot(new SlotItemHandler(entity.filter,0,53,37));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));
        addDataSlot(new DataSlot(){public int get(){return entity.adult()?1:0;}public void set(int value){entity.adult(value!=0);}});
    }
    @Override public boolean stillValid(Player player){return entity.usable(player)&&entity.mob();}
    @Override public boolean clickMenuButton(Player player,int id){if(!stillValid(player)||(id!=0&&id!=1))return false;entity.adult(id==0);return true;}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);var stack=slot.getItem();if(stack.isEmpty()||!stack.is(Tinkerer.SOUL_MOULD.get()))return ItemStack.EMPTY;
        var result=stack.copy();if(!(index==0?moveItemStackTo(stack,1,37,false):moveItemStackTo(stack,0,1,false)))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return result;
    }
}
