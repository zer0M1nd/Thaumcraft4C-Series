package gadomancymodern.claw;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public class InfusionClawMenu extends AbstractContainerMenu {
    public final InfusionClawEntity claw;
    public InfusionClawMenu(int id,Inventory inventory,InfusionClawEntity claw){super(gadomancymodern.Gadomancy.CLAW_MENU.get(),id);this.claw=claw;addSlot(new Slot(claw,0,80,32){@Override public boolean mayPlace(ItemStack stack){return claw.canPlaceItem(0,stack);}@Override public boolean mayPickup(Player player){return !claw.running();}});for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,84+row*18));for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));}
    @Override public boolean stillValid(Player player){return claw.stillValid(player);}
    @Override public boolean clickMenuButton(Player player,int id){if(!stillValid(player)||id<0||id>1)return false;claw.locked(id==1);return true;}
    @Override public ItemStack quickMoveStack(Player player,int index){if(claw.running()||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;var original=slot.getItem();var copy=original.copy();if(index==0?!moveItemStackTo(original,1,slots.size(),true):!slots.get(0).mayPlace(original)||!moveItemStackTo(original,0,1,false))return ItemStack.EMPTY;if(original.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();return copy;}
}
