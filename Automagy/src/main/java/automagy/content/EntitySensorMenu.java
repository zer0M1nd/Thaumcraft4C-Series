package automagy.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class EntitySensorMenu extends AbstractContainerMenu {
    private final EntitySensorBlockEntity tile;private final ContainerData data;
    public EntitySensorMenu(int id,Inventory inventory,BlockPos pos){this(id,inventory,inventory.player.level().getBlockEntity(pos) instanceof EntitySensorBlockEntity sensor?sensor:null);}
    public EntitySensorMenu(int id,Inventory inventory,EntitySensorBlockEntity tile){
        super(Content.ENTITY_SENSOR_MENU.get(),id);this.tile=tile;
        data=tile==null||inventory.player.level().isClientSide?new SimpleContainerData(4):new ContainerData(){
            public int get(int index){return switch(index){case 0->tile.adults?1:0;case 1->tile.children?1:0;case 2->tile.specificPlayer?1:0;default->tile.count();};}
            public void set(int index,int value){}public int getCount(){return 4;}
        };addDataSlots(data);
        addSlot(new SlotItemHandler(tile==null?new net.minecraftforge.items.ItemStackHandler(1):tile.eyeInventory(),0,16,30));
        for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inventory,9+r*9+c,8+c*18,84+r*18));
        for(int c=0;c<9;c++)addSlot(new Slot(inventory,c,8+c*18,142));
    }
    public int value(int index){return data.get(index);}
    @Override public boolean stillValid(Player player){return tile!=null&&!tile.isRemoved()&&tile.getLevel()==player.level()&&player.mayBuild()
            &&player.level().getBlockEntity(tile.getBlockPos())==tile&&player.distanceToSqr(tile.getBlockPos().getCenter())<=64;}
    @Override public boolean clickMenuButton(Player player,int button){
        if(player.level().isClientSide||!stillValid(player)||button<0||button>2)return false;
        switch(button){case 0->tile.adults=!tile.adults;case 1->tile.children=!tile.children;case 2->tile.specificPlayer=!tile.specificPlayer;}
        tile.refresh();tile.sync();broadcastChanges();return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
        if(index==0){if(!moveItemStackTo(stack,1,slots.size(),true))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}
