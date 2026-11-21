package automagy.content;

import automagy.logic.ReaderChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class VisReaderMenu extends AbstractContainerMenu {
    private final VisReaderBlockEntity tile;
    private final ContainerData data;
    private static final int[] DIRECTIONS={6,2,5,3,4,0};
    public VisReaderMenu(int id,Inventory inventory,BlockPos pos){this(id,inventory,inventory.player.level().getBlockEntity(pos) instanceof VisReaderBlockEntity reader?reader:null);}
    public VisReaderMenu(int id,Inventory inventory,VisReaderBlockEntity tile){
        super(Content.VIS_READER_MENU.get(),id);this.tile=tile;
        data=tile==null||inventory.player.level().isClientSide?new SimpleContainerData(20):new ContainerData(){
            public int get(int index){var c=tile.channels[index/5];return switch(index%5){case 0->c.mode();case 1->c.capacity();case 2->c.direction();case 3->c.inverted()?1:0;default->tile.channelSignal(index/5);};}
            public void set(int index,int value){}public int getCount(){return 20;}
        };addDataSlots(data);
        var labels=tile==null?new net.minecraftforge.items.ItemStackHandler(4):tile.labels();
        for(int i=0;i<4;i++)addSlot(new SlotItemHandler(labels,i,6,10+i*19));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,96+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,154));
    }
    public int value(int row,int field){return data.get(row*5+field);}
    @Override public boolean stillValid(Player player){return tile!=null&&!tile.isRemoved()&&player.mayBuild()&&tile.getLevel()==player.level()
            &&player.level().getBlockEntity(tile.getBlockPos())==tile&&player.distanceToSqr(tile.getBlockPos().getCenter())<=64;}
    @Override public boolean clickMenuButton(Player player,int button){
        if(player.level().isClientSide||!stillValid(player)||button<0||button>=32)return false;
        int step=button>=16?-1:1;button%=16;int row=button/4,operation=button%4;var current=tile.channels[row];
        int mode=current.mode(),capacity=current.capacity(),direction=current.direction();boolean inverse=current.inverted();
        switch(operation){case 0->mode=Math.floorMod(mode+step,5);case 1->capacity=next(ReaderChannel.CAPACITIES,capacity,step);case 2->direction=next(DIRECTIONS,direction,step);case 3->inverse=!inverse;}
        tile.channels[row]=new ReaderChannel(mode,capacity,direction,inverse);tile.refresh();tile.sync();broadcastChanges();return true;
    }
    private static int next(int[] choices,int value,int step){for(int i=0;i<choices.length;i++)if(choices[i]==value)return choices[Math.floorMod(i+step,choices.length)];return choices[0];}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size()||!stillValid(player))return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var copy=stack.copy();
        if(index<4){if(!moveItemStackTo(stack,4,slots.size(),true))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,0,4,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}
