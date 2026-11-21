package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
public class MawMenu extends AbstractContainerMenu {
 private final MawBlockEntity tile;
 public MawMenu(int id,Inventory inv,BlockPos pos){super(Content.MAW_MENU.get(),id);tile=inv.player.level().getBlockEntity(pos) instanceof MawBlockEntity t?t:null;
  addSlot(new SlotItemHandler(tile==null?new net.minecraftforge.items.ItemStackHandler(1):tile.inventory,0,80,28));
  for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inv,9+r*9+c,8+c*18,84+r*18));
  for(int c=0;c<9;c++)addSlot(new Slot(inv,c,8+c*18,142));
 }
 public boolean stillValid(Player p){return tile!=null&&!tile.isRemoved()&&tile.getLevel()==p.level()&&p.mayBuild()&&p.level().getBlockEntity(tile.getBlockPos())==tile&&p.distanceToSqr(tile.getBlockPos().getCenter())<=64;}
 public ItemStack quickMoveStack(Player p,int i){if(!stillValid(p)||i<0||i>=slots.size())return ItemStack.EMPTY;var slot=slots.get(i);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var original=stack.copy();if(i==0?!moveItemStackTo(stack,1,slots.size(),true):!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return original;}
}
