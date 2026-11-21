package automagy.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.*;

public final class GreedyChestMenu extends AbstractContainerMenu {
    private final GreedyChestBlockEntity tile;
    private final ContainerData data;
    public final boolean configuration;
    public GreedyChestMenu(int id,Inventory inventory,BlockPos pos){this(id,inventory,pos,false);}
    public GreedyChestMenu(int id,Inventory inventory,BlockPos pos,boolean config){this(id,inventory,inventory.player.level().getBlockEntity(pos) instanceof GreedyChestBlockEntity tile?tile:null,config);}
    public GreedyChestMenu(int id,Inventory inventory,GreedyChestBlockEntity tile){this(id,inventory,tile,false);}
    public GreedyChestMenu(int id,Inventory inventory,GreedyChestBlockEntity tile,boolean config){
        super(Content.GREEDY_MENU.get(),id); this.tile=tile;this.configuration=config;
        IItemHandler handler=tile==null?new ItemStackHandler(28):tile.menuInventory();
        for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(visibleSlot(handler,r*9+c,8+c*18,18+r*18,!config));
        addSlot(visibleSlot(handler,27,98,51,config));
        int inventoryY=config?84:85;
        for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inventory,9+r*9+c,8+c*18,inventoryY+r*18));
        for(int c=0;c<9;c++)addSlot(new Slot(inventory,c,8+c*18,inventoryY+58));
        data=tile==null||inventory.player.level().isClientSide?new SimpleContainerData(9):new ContainerData(){
            public int get(int i){return i<6?(tile.directions[i]?1:0):i==6?tile.mode:i==7?(int)Math.round(tile.fraction*10000):tile.amount;}
            public void set(int i,int value){} public int getCount(){return 9;}
        };addDataSlots(data); if(tile!=null)tile.viewer(inventory.player,!config);
    }
    private Slot visibleSlot(IItemHandler items,int index,int x,int y,boolean visible){return new SlotItemHandler(items,index,visible?x:-10000,visible?y:-10000){
        @Override public boolean isActive(){return visible;}
        @Override public boolean mayPlace(ItemStack stack){return visible&&super.mayPlace(stack);}
        @Override public boolean mayPickup(Player player){return visible&&super.mayPickup(player);}
    };}
    public GreedyChestBlockEntity tile(){return tile;}
    public int value(int index){return data.get(index);}
    @Override public boolean stillValid(Player player){return tile!=null&&!tile.isRemoved()&&tile.getLevel()==player.level()&&player.mayBuild()&&player.level().getBlockEntity(tile.getBlockPos())==tile&&player.distanceToSqr(tile.getBlockPos().getCenter())<=64;}
    @Override public boolean clickMenuButton(Player player,int button){if(player.level().isClientSide||!stillValid(player)||button<0||button>14)return false;tile.configure(button);broadcastChanges();return true;}
    public void quantity(Player player,int expectedMode,double value){if(player.level().isClientSide||!configuration||!stillValid(player)||expectedMode!=tile.mode||!Double.isFinite(value))return;tile.setQuantity(value);broadcastChanges();}
    @Override public void removed(Player player){super.removed(player);if(tile!=null)tile.viewer(player,false);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.isActive()||!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
        if(index<28){if(!moveItemStackTo(stack,28,slots.size(),true))return ItemStack.EMPTY;}
        else if(configuration&&stack.getItem() instanceof FilterItem&&slots.get(27).mayPlace(stack)&&!slots.get(27).hasItem()){if(!moveItemStackTo(stack,27,28,false))return ItemStack.EMPTY;}
        else if(configuration||!moveItemStackTo(stack,0,27,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}
