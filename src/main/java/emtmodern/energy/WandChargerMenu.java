package emtmodern.energy;
import emtmodern.ElectroMagicTools;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public class WandChargerMenu extends AbstractContainerMenu {
    public final WandChargerEntity machine;
    public WandChargerMenu(int id,Inventory inventory,WandChargerEntity machine){super(ElectroMagicTools.WAND_CHARGER_MENU.get(),id);this.machine=machine;
        for(int x=0;x<9;x++)addSlot(new Slot(inventory,x,8+x*18,142));
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inventory,9+x+y*9,8+x*18,84+y*18));
        addSlot(new Slot(machine,0,80,35));
    }
    @Override public boolean stillValid(Player player){return machine.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),before=stack.copy();
        if(index==36){if(!moveItemStackTo(stack,0,35,false))return ItemStack.EMPTY;}else if(!moveItemStackTo(stack,36,37,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();if(stack.getCount()==before.getCount())return ItemStack.EMPTY;slot.onTake(player,stack);return before;
    }
}
