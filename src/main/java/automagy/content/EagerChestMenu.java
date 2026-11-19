package automagy.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.*;

public final class EagerChestMenu extends AbstractContainerMenu {
    private final EagerChestBlockEntity tile;
    private final ContainerData data;
    public final boolean configuration;
    public EagerChestMenu(int id,Inventory inv,BlockPos pos){this(id,inv,pos,false);}
    public EagerChestMenu(int id,Inventory inv,BlockPos pos,boolean config){this(id,inv,inv.player.level().getBlockEntity(pos) instanceof EagerChestBlockEntity tile?tile:null,config);}
    public EagerChestMenu(int id,Inventory inv,EagerChestBlockEntity tile){this(id,inv,tile,false);}
    public EagerChestMenu(int id,Inventory inv,EagerChestBlockEntity tile,boolean config){
        super(Content.EAGER_MENU.get(),id);this.tile=tile;this.configuration=config;IItemHandler items=tile==null?new ItemStackHandler(31):tile.menuInventory();
        for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(visibleSlot(items,r*9+c,8+c*18,18+r*18,!config));
        for(int i=0;i<4;i++)addSlot(visibleSlot(items,27+i,19+(i/2)*87,13+(i%2)*28,config));
        int inventoryY=config?84:85;
        for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inv,9+r*9+c,8+c*18,inventoryY+r*18));
        for(int c=0;c<9;c++)addSlot(new Slot(inv,c,8+c*18,inventoryY+58));
        data=tile==null||inv.player.level().isClientSide?new SimpleContainerData(1):new ContainerData(){public int get(int i){return tile.allowed(inv.player)?1:0;}public void set(int i,int value){}public int getCount(){return 1;}};
        addDataSlots(data);if(tile!=null)tile.viewer(inv.player,!config);
    }
    private Slot visibleSlot(IItemHandler items,int index,int x,int y,boolean visible){return new SlotItemHandler(items,index,visible?x:-10000,visible?y:-10000){
        @Override public boolean isActive(){return visible;}
        @Override public boolean mayPlace(ItemStack stack){return visible&&super.mayPlace(stack);}
        @Override public boolean mayPickup(Player player){return visible&&super.mayPickup(player);}
    };}
    public EagerChestBlockEntity tile(){return tile;}
    public boolean enabled(){return data.get(0)==1;}
    @Override public boolean stillValid(Player player){return tile!=null&&!tile.isRemoved()&&tile.getLevel()==player.level()&&player.mayBuild()&&player.level().getBlockEntity(tile.getBlockPos())==tile&&player.distanceToSqr(tile.getBlockPos().getCenter())<=64;}
    @Override public boolean clickMenuButton(Player player,int button){if(player.level().isClientSide||!stillValid(player)||button!=0)return false;tile.allow(player,!tile.allowed(player));broadcastChanges();return true;}
    @Override public void removed(Player player){super.removed(player);if(tile!=null)tile.viewer(player,false);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.isActive()||!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
        if(index<31){if(!moveItemStackTo(stack,31,slots.size(),true))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,configuration?27:0,configuration?31:27,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}
