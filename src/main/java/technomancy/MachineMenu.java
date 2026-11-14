package technomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class MachineMenu extends AbstractContainerMenu {
    public final MachineEntity machine;
    public final int machineSlots;
    public MachineMenu(int id,Inventory inv,BlockPos pos){this(id,inv,(MachineEntity)inv.player.level().getBlockEntity(pos));}
    public MachineMenu(int id,Inventory inv,MachineEntity machine) {
        super(TechRegistry.MENU.get(),id);this.machine=java.util.Objects.requireNonNull(machine);
        machineSlots=machine.kind==MachineKind.EXISTENCE_CROP_HARVESTER?machine.inventory.getSlots():machine.kind.processor()||machine.kind==MachineKind.ADVANCED_DECONSTRUCTION_TABLE?2:0;
        for(int slot=0;slot<machineSlots;slot++) {
            int index=slot;boolean harvest=machine.kind==MachineKind.EXISTENCE_CROP_HARVESTER;
            addSlot(new SlotItemHandler(machine.inventory,slot,harvest?8+slot*18:slot==0?50:107,machine.kind.processor()?27:69) {
                @Override public boolean mayPlace(ItemStack stack){return !harvest&&index==0&&machine.acceptsItem(stack);}
            });
        }
        int inventoryY=machine.kind.processor()?84:112;
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,col+row*9+9,8+col*18,inventoryY+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,inventoryY+58));
    }
    @Override public boolean stillValid(Player p){return !machine.isRemoved()&&machine.getLevel()==p.level()&&p.distanceToSqr(machine.getBlockPos().getX()+.5,machine.getBlockPos().getY()+.5,machine.getBlockPos().getZ()+.5)<=64;}
    @Override public boolean clickMenuButton(Player player,int button) {
        if(!stillValid(player)||player.level().isClientSide)return false;
        if(button==1&&(machine.kind==MachineKind.ECOLOGIC_TRANSMUTER||machine.kind==MachineKind.ELDRITCH_CONSUMER||machine.kind==MachineKind.MANA_EXCHANGER))machine.mode=(machine.mode+1)%(machine.kind==MachineKind.ECOLOGIC_TRANSMUTER?3:machine.kind==MachineKind.ELDRITCH_CONSUMER?6:2);
        else if(button==2&&(machine.kind==MachineKind.NODE_FABRICATOR||machine.kind.catalyst()))machine.activate(player);
        else return false;
        machine.changed();return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var copy=stack.copy();
        if(index<machineSlots){if(!moveItemStackTo(stack,machineSlots,slots.size(),true))return ItemStack.EMPTY;}
        else if(machineSlots>0&&machine.kind!=MachineKind.EXISTENCE_CROP_HARVESTER&&machine.acceptsItem(stack)){if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;}
        else if(index<machineSlots+27){if(!moveItemStackTo(stack,machineSlots+27,slots.size(),false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,machineSlots,machineSlots+27,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}
