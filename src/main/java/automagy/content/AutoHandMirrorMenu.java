package automagy.content;

import java.util.List;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class AutoHandMirrorMenu extends AbstractContainerMenu {
    private final Player owner;
    private final int heldSlot;
    private final ItemStack mirror;
    private final SimpleContainer input=new SimpleContainer(1),filters=new SimpleContainer(2);
    private boolean processing,closed;
    public AutoHandMirrorMenu(int id,Inventory inventory,int heldSlot){
        super(Content.AUTO_HAND_MIRROR_MENU.get(),id);owner=inventory.player;this.heldSlot=heldSlot;
        mirror=heldSlot>=0&&heldSlot<inventory.getContainerSize()?inventory.getItem(heldSlot):ItemStack.EMPTY;
        var saved=AutoHandMirrorItem.filters(mirror);for(int i=0;i<2;i++)filters.setItem(i,saved.get(i));
        addSlot(new Slot(input,0,80,24){@Override public boolean mayPlace(ItemStack stack){return stack!=mirror;}});
        for(int i=0;i<2;i++)addSlot(new Slot(filters,i,i==0?21:139,24){
            @Override public boolean mayPlace(ItemStack stack){return stack.getItem() instanceof FilterItem&&ItemFilter.load(stack).populated();}
            @Override public int getMaxStackSize(){return 1;}
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)playerSlot(inventory,9+row*9+col,8+col*18,84+row*18);
        for(int col=0;col<9;col++)playerSlot(inventory,col,8+col*18,142);
        input.addListener(this::slotsChanged);
        filters.addListener(ignored->{if(!owner.level().isClientSide&&stillValid(owner)){AutoHandMirrorItem.filters(mirror,List.of(filters.getItem(0),filters.getItem(1)));owner.getInventory().setChanged();}});
    }
    private void playerSlot(Inventory inventory,int index,int x,int y){addSlot(new Slot(inventory,index,x,y){
        @Override public boolean mayPickup(Player player){return index!=heldSlot;}
        @Override public boolean mayPlace(ItemStack stack){return index!=heldSlot;}
    });}
    public ItemStack mirror(){return mirror;}
    public int heldSlot(){return heldSlot;}
    @Override public boolean stillValid(Player player){
        if(closed||player!=owner||!player.isAlive()||heldSlot<0||heldSlot>=player.getInventory().getContainerSize())return false;
        var held=player.getInventory().getItem(heldSlot);
        return held.getItem() instanceof AutoHandMirrorItem&&(player.level().isClientSide||held==mirror)&&tc4revival.content.ItemMirrorItem.destination(held)!=null;
    }
    @Override public void slotsChanged(Container container){
        if(processing||owner.level().isClientSide||!stillValid(owner))return;
        var stack=input.getItem(0);if(stack.isEmpty())return;
        processing=true;
        try{if(AutoHandMirrorItem.transport(mirror,stack,owner))input.removeItemNoUpdate(0);}finally{processing=false;}
    }
    @Override public void broadcastChanges(){slotsChanged(input);super.broadcastChanges();}
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(!stillValid(player)||type==ClickType.SWAP)return;
        if(slot>=0&&slot<slots.size()&&slots.get(slot).container==player.getInventory()&&slots.get(slot).getContainerSlot()==heldSlot)return;
        super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.mayPickup(player)||!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var before=stack.copy();boolean moved;
        if(index<3)moved=moveItemStackTo(stack,3,39,true);
        else if(stack.getItem() instanceof FilterItem)moved=moveItemStackTo(stack,1,3,false);
        else moved=moveItemStackTo(stack,0,1,false);
        if(!moved)return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return before;
    }
    @Override public void removed(Player player){
        if(closed)return;closed=true;super.removed(player);
        if(!player.level().isClientSide){var remainder=input.removeItemNoUpdate(0);if(!remainder.isEmpty())player.drop(remainder,false);}
    }
}
