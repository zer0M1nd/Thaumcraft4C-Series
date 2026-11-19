package ttmodern;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class IchorPouchMenu extends AbstractContainerMenu {
    private final Player owner;
    private final InteractionHand hand;
    private final int selected;
    private final ItemStack pouch;
    private final SimpleContainer contents=new SimpleContainer(IchorPouchItem.SIZE);
    public IchorPouchMenu(int id,Inventory inventory,FriendlyByteBuf data){this(id,inventory,data.readEnum(InteractionHand.class));}
    public IchorPouchMenu(int id,Inventory inventory,InteractionHand hand){
        super(Tinkerer.ICHOR_POUCH_MENU.get(),id);owner=inventory.player;this.hand=hand;selected=inventory.selected;pouch=owner.getItemInHand(hand);
        if(!owner.level().isClientSide){var saved=IchorPouchItem.contents(pouch);for(int i=0;i<IchorPouchItem.SIZE;i++)contents.setItem(i,saved.get(i));}
        contents.addListener(ignored->persist());
        for(int row=0;row<9;row++)for(int col=0;col<13;col++)addSlot(new Slot(contents,row*13+col,12+col*18,8+row*18){@Override public boolean mayPlace(ItemStack stack){return IchorPouchItem.accepts(stack);}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,row*9+col+9,48+col*18,177+row*18));
        for(int col=0;col<9;col++){boolean locked=hand==InteractionHand.MAIN_HAND&&col==selected;addSlot(new Slot(inventory,col,48+col*18,235){@Override public boolean mayPickup(Player player){return !locked;}@Override public boolean mayPlace(ItemStack stack){return !locked;}});}
    }
    private int lockedSlot(){return hand==InteractionHand.MAIN_HAND?IchorPouchItem.SIZE+27+selected:-1;}
    @Override public boolean stillValid(Player player){return player==owner&&player.isAlive()&&!player.isSpectator()&&player.getItemInHand(hand)==pouch&&pouch.getItem() instanceof IchorPouchItem&&(hand!=InteractionHand.MAIN_HAND||player.getInventory().selected==selected);}
    private void persist(){if(owner.level().isClientSide||!stillValid(owner))return;IchorPouchItem.save(pouch,java.util.stream.IntStream.range(0,IchorPouchItem.SIZE).mapToObj(contents::getItem).toList());owner.getInventory().setChanged();}
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(!stillValid(player)||slot>=0&&slot==lockedSlot()||type==ClickType.SWAP&&button==(hand==InteractionHand.MAIN_HAND?selected:40))return;
        super.clicked(slot,button,type,player);persist();
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size()||index==lockedSlot())return ItemStack.EMPTY;
        var slot=slots.get(index);var stack=slot.getItem();if(stack.isEmpty())return ItemStack.EMPTY;var original=stack.copy();
        if(index<IchorPouchItem.SIZE){if(!moveItemStackTo(stack,IchorPouchItem.SIZE,slots.size(),true))return ItemStack.EMPTY;}
        else if(!IchorPouchItem.accepts(stack)||!moveItemStackTo(stack,0,IchorPouchItem.SIZE,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);persist();return original;
    }
    @Override public void removed(Player player){persist();super.removed(player);}
}
