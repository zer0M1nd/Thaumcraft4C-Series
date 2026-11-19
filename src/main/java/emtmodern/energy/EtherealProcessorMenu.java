package emtmodern.energy;
import emtmodern.ElectroMagicTools;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public class EtherealProcessorMenu extends AbstractContainerMenu {
    public final EtherealProcessorEntity machine;
    public EtherealProcessorMenu(int id,Inventory inventory,EtherealProcessorEntity machine){super(ElectroMagicTools.PROCESSOR_MENU.get(),id);this.machine=machine;addSlot(new Slot(machine,0,56,26));addSlot(new Slot(machine,2,116,26){@Override public boolean mayPlace(ItemStack stack){return false;}});addSlot(new Slot(machine,1,116,53){@Override public boolean mayPlace(ItemStack stack){return false;}});for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inventory,9+x+y*9,8+x*18,84+y*18));for(int x=0;x<9;x++)addSlot(new Slot(inventory,x,8+x*18,142));}
    @Override public boolean stillValid(Player player){return machine.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var moving=slot.getItem();var before=moving.copy();boolean moved;
        // Original legacy bounds exclude the last hotbar slot, retained rather than silently changing behavior.
        if(index==1)moved=moveItemStackTo(moving,2,38,true);
        else if(index==0)moved=moveItemStackTo(moving,3,38,false);
        else if(machine.canPlaceItem(0,moving))moved=moveItemStackTo(moving,0,1,false);
        else if(index>=2&&index<29)moved=moveItemStackTo(moving,29,38,false);
        else if(index>=29&&index<38)moved=moveItemStackTo(moving,2,29,false);else moved=false;
        if(!moved)return ItemStack.EMPTY;if(moving.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();if(moving.getCount()==before.getCount())return ItemStack.EMPTY;slot.onTake(player,moving);return before;
    }
}
