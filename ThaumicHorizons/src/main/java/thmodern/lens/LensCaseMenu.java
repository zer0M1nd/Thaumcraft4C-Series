package thmodern.lens;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import thmodern.Horizons;
public final class LensCaseMenu extends AbstractContainerMenu {
    private final Inventory inventory;private final ItemStack bag;private final InteractionHand hand;public final int locked;
    private final SimpleContainer contents=new SimpleContainer(18);
    public LensCaseMenu(int id,Inventory inv,FriendlyByteBuf buf){this(id,inv,buf.readEnum(InteractionHand.class));}
    public LensCaseMenu(int id,Inventory inv,InteractionHand hand){
        super(Horizons.LENS_CASE_MENU.get(),id);inventory=inv;this.hand=hand;bag=inv.player.getItemInHand(hand);locked=hand==InteractionHand.MAIN_HAND?inv.selected:40;
        for(int i=0;i<18;i++){contents.setItem(i,LensCaseItem.get(bag,i));addSlot(new Slot(contents,i,37+i%6*18,51+i/6*18){@Override public boolean mayPlace(ItemStack s){return s.getItem() instanceof LensItem;}@Override public int getMaxStackSize(){return 1;}});}
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)playerSlot(9+y*9+x,8+x*18,151+y*18);for(int x=0;x<9;x++)playerSlot(x,8+x*18,209);
        contents.addListener(c->{if(!inv.player.level().isClientSide){for(int i=0;i<18;i++)LensCaseItem.set(bag,i,c.getItem(i));inv.setChanged();}});
    }
    private void playerSlot(int i,int x,int y){addSlot(new Slot(inventory,i,x,y){@Override public boolean mayPickup(Player p){return i!=locked;}@Override public boolean mayPlace(ItemStack s){return i!=locked;}});}
    @Override public boolean stillValid(Player p){return p.isAlive()&&p.getItemInHand(hand).getItem() instanceof LensCaseItem&&(p.level().isClientSide||p.getItemInHand(hand)==bag);}
    @Override public void clicked(int slot,int button,ClickType type,Player p){if(!stillValid(p)||type==ClickType.SWAP&&(button==locked||slot<0||slot>=slots.size()))return;if(slot>=0&&slot<slots.size()&&slots.get(slot).container==inventory&&slots.get(slot).getContainerSlot()==locked)return;super.clicked(slot,button,type,p);}
    @Override public ItemStack quickMoveStack(Player p,int index){if(!stillValid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.mayPickup(p)||!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var old=stack.copy();if(index<18){if(!moveItemStackTo(stack,18,54,true))return ItemStack.EMPTY;}else if(!(stack.getItem() instanceof LensItem)||!moveItemStackTo(stack,0,18,false))return ItemStack.EMPTY;if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return old;}
}
