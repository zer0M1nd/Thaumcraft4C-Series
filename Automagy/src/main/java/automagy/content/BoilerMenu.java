package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.*;
public final class BoilerMenu extends AbstractContainerMenu {
 public final BoilerBlockEntity tile;
 public BoilerMenu(int id,Inventory inv,BlockPos pos){super(Content.BOILER_MENU.get(),id);tile=inv.player.level().getBlockEntity(pos) instanceof BoilerBlockEntity b?b:null;
  addSlot(new SlotItemHandler(tile==null?new ItemStackHandler(1):tile.fuelInventory(),0,81,53));
  for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inv,9+r*9+c,8+c*18,93+r*18));
  for(int c=0;c<9;c++)addSlot(new Slot(inv,c,8+c*18,151));
 }
 @Override public boolean stillValid(Player p){return tile!=null&&!tile.isRemoved()&&p.mayBuild()&&p.level()==tile.getLevel()&&p.level().getBlockEntity(tile.getBlockPos())==tile&&p.distanceToSqr(tile.getBlockPos().getCenter())<=64;}
 @Override public ItemStack quickMoveStack(Player p,int index){if(!stillValid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var s=slot.getItem();var copy=s.copy();if(!moveItemStackTo(s,index==0?1:0,index==0?slots.size():1,index==0))return ItemStack.EMPTY;if(s.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,s);return copy;}
}
