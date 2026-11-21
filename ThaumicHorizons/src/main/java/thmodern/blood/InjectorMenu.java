package thmodern.blood;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import thmodern.Horizons;

public final class InjectorMenu extends AbstractContainerMenu {
    private final InteractionHand hand;
    private final ItemStack injector;
    private final Inventory inventory;
    private final SimpleContainer ammunition=new SimpleContainer(7);
    private final int locked;
    public InjectorMenu(int id,Inventory inventory,FriendlyByteBuf buffer){this(id,inventory,buffer.readEnum(InteractionHand.class));}
    public InjectorMenu(int id,Inventory inventory,InteractionHand hand){
        super(Horizons.INJECTOR_MENU.get(),id);this.inventory=inventory;this.hand=hand;injector=inventory.player.getItemInHand(hand);locked=hand==InteractionHand.MAIN_HAND?inventory.selected:40;
        for(int i=0;i<7;i++)ammunition.setItem(i,InjectorItem.ammo(injector,i));
        int[][] positions={{73,10},{99,20},{107,47},{92,70},{64,72},{45,51},{49,24}};
        for(int i=0;i<7;i++)addSlot(new Slot(ammunition,i,positions[i][0],positions[i][1]){
            @Override public boolean mayPlace(ItemStack stack){return InjectorItem.ammunition(stack);}
            @Override public int getMaxStackSize(){return 1;}
        });
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addPlayerSlot(9+y*9+x,8+x*18,108+y*18);
        for(int x=0;x<9;x++)addPlayerSlot(x,8+x*18,166);
        ammunition.addListener(c->{if(!inventory.player.level().isClientSide){var slots=new java.util.ArrayList<ItemStack>();for(int i=0;i<7;i++)slots.add(c.getItem(i));InjectorItem.saveAmmo(injector,slots);inventory.setChanged();}});
    }
    private void addPlayerSlot(int index,int x,int y){addSlot(new Slot(inventory,index,x,y){@Override public boolean mayPickup(Player player){return index!=locked;}@Override public boolean mayPlace(ItemStack stack){return index!=locked;}});}
    @Override public boolean stillValid(Player player){return player.isAlive()&&player.getItemInHand(hand).is(Horizons.INJECTOR.get())&&(player.level().isClientSide||player.getItemInHand(hand)==injector);}
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(!stillValid(player)||type==ClickType.SWAP&&button==locked)return;
        if(slot>=0&&slot<slots.size()&&slots.get(slot).container==inventory&&slots.get(slot).getContainerSlot()==locked)return;
        super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int slot){
        if(!stillValid(player)||slot<0||slot>=slots.size())return ItemStack.EMPTY;
        var from=slots.get(slot);if(!from.mayPickup(player)||!from.hasItem())return ItemStack.EMPTY;
        var stack=from.getItem();var original=stack.copy();
        if(slot<7){if(!moveItemStackTo(stack,7,43,true))return ItemStack.EMPTY;}
        else if(!InjectorItem.ammunition(stack)||!moveItemStackTo(stack,0,7,false))return ItemStack.EMPTY;
        if(stack.isEmpty())from.set(ItemStack.EMPTY);else from.setChanged();from.onTake(player,stack);return original;
    }
}
