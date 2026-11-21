package thaumicexploration;

import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.items.SlotItemHandler;

public class ThinkTankMenu extends AbstractContainerMenu {
    public final MachineEntity machine;
    public int progress;
    public ThinkTankMenu(int id,Inventory inventory,FriendlyByteBuf buffer){this(id,inventory,(MachineEntity)inventory.player.level().getBlockEntity(buffer.readBlockPos()));}
    public ThinkTankMenu(int id,Inventory inventory,MachineEntity machine){
        super(ExplorationMod.THINK_MENU.get(),id);this.machine=machine;
        addSlot(new SlotItemHandler(machine.items,0,64,16));
        addSlot(new SlotItemHandler(machine.items,1,64,48){@Override public boolean mayPlace(ItemStack stack){return false;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));
        addDataSlot(new DataSlot(){public int get(){return machine.progress;}public void set(int value){progress=value;}});
    }
    @Override public boolean stillValid(Player player){return machine.getLevel()!=null&&!machine.isRemoved()&&machine.getLevel().getBlockEntity(machine.getBlockPos())==machine&&player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(machine.getBlockPos()))<=64;}
    @Override public ItemStack quickMoveStack(Player player,int index){
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index<2){if(!moveItemStackTo(stack,2,38,true))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
}
