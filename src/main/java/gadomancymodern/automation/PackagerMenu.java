package gadomancymodern.automation;

import gadomancymodern.Gadomancy;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

public class PackagerMenu extends AbstractContainerMenu {
    public final PackagerBlockEntity machine;
    public PackagerMenu(int id,Inventory player,PackagerBlockEntity machine){
        super(Gadomancy.PACKAGER_MENU.get(),id);this.machine=machine;
        for(int row=0;row<3;row++)for(int col=0;col<3;col++)addSlot(new Slot(machine,row*3+col,16+18*col,42+23*row));
        addSlot(new Slot(machine,9,95,33){@Override public boolean mayPlace(ItemStack stack){return stack.is(Items.LEATHER);}});
        addSlot(new Slot(machine,10,113,33){@Override public boolean mayPlace(ItemStack stack){return stack.is(Items.STRING);}});
        addSlot(new Slot(machine,11,160,64){@Override public boolean mayPlace(ItemStack stack){return false;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(player,9+row*9+col,16+col*18,151+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(player,col,16+col*18,209));
    }
    @Override public boolean stillValid(Player player){return machine.stillValid(player);}
    @Override public boolean clickMenuButton(Player player,int id){
        if(!stillValid(player)||id<0||id>5)return false;
        switch(id/2){case 0->machine.useEssentia=id==0;case 1->machine.autoStart=id==2;case 2->machine.disguise=id==4;}
        machine.changed();return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack source=slot.getItem(),original=source.copy();
        if(index<12){if(!moveItemStackTo(source,12,slots.size(),true))return ItemStack.EMPTY;}
        else {
            int special=source.is(Items.LEATHER)?9:source.is(Items.STRING)?10:-1;
            if(special>=0)moveItemStackTo(source,special,special+1,false);
            if(!source.isEmpty()&&!moveItemStackTo(source,0,9,false)&&source.getCount()==original.getCount())return ItemStack.EMPTY;
        }
        if(source.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,source);return original;
    }
}
