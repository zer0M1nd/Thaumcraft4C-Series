package wgmodern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public class SpinningMenu extends AbstractContainerMenu {
    public final SpinningWheel.Tile tile;public final ContainerData data;
    public SpinningMenu(int id,Inventory inv,BlockPos pos){super(Content.SPINNING_MENU.get(),id);tile=(SpinningWheel.Tile)inv.player.level().getBlockEntity(pos);if(tile==null)throw new IllegalArgumentException("Missing spinning wheel");data=new ContainerData(){public int get(int i){return i==0?tile.progress:tile.duration;}public void set(int i,int v){if(i==0)tile.progress=v;else tile.duration=v;}public int getCount(){return 2;}};addDataSlots(data);
        for(int i=0;i<5;i++)addSlot(new Slot(tile,i,16,16+24*i));addSlot(new Slot(tile,5,142,64){@Override public boolean mayPlace(ItemStack s){return false;}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+y*9+x,16+x*18,151+y*18));for(int x=0;x<9;x++)addSlot(new Slot(inv,x,16+x*18,209));}
    @Override public boolean stillValid(Player p){return tile.stillValid(p);}
    @Override public ItemStack quickMoveStack(Player player,int i){if(i<0||i>=slots.size())return ItemStack.EMPTY;var slot=slots.get(i);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var before=stack.copy();if(i<6?!moveItemStackTo(stack,6,42,true):!moveItemStackTo(stack,0,5,false))return ItemStack.EMPTY;if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return before;}
}
