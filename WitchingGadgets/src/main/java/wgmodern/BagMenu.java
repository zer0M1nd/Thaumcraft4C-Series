package wgmodern;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** The original 18 slots; owner identity and held-slot guards prevent duplication during GUI swaps. */
public class BagMenu extends AbstractContainerMenu {
    private final Inventory inventory;private final ItemStack bag;private final int boundSlot;public final boolean filter;
    public final SimpleContainer contents;
    public BagMenu(int id,Inventory inventory,boolean offhand){super(Content.BAG_MENU.get(),id);this.inventory=inventory;boundSlot=offhand?40:inventory.selected;bag=inventory.getItem(boundSlot);if(!(bag.getItem() instanceof BagItem item))throw new IllegalArgumentException("Missing held bag");filter=item.kind.equals("void");contents=new SimpleContainer(18){@Override public void setChanged(){super.setChanged();if(!inventory.player.level().isClientSide)save();}};var stored=BagItem.contents(bag);for(int i=0;i<18;i++)contents.setItem(i,stored.get(i));
        for(int i=0;i<18;i++)addSlot(new Slot(contents,i,(filter?29:35)+i%6*(filter?20:18),(filter?7:9)+i/6*(filter?20:18)){@Override public boolean mayPlace(ItemStack s){return !filter&&s.getItem().canFitInsideContainerItems();}@Override public boolean mayPickup(Player p){return !filter;}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)playerSlot(9+y*9+x,8+x*18,82+y*18);for(int x=0;x<9;x++)playerSlot(x,8+x*18,140);
    }
    private void playerSlot(int slot,int x,int y){addSlot(new Slot(inventory,slot,x,y){@Override public boolean mayPickup(Player p){return slot!=boundSlot;}@Override public boolean mayPlace(ItemStack s){return slot!=boundSlot;}});}
    private void save(){if(contents==null||inventory.getItem(boundSlot)!=bag)return;var stored=net.minecraft.core.NonNullList.withSize(18,ItemStack.EMPTY);for(int i=0;i<18;i++)stored.set(i,contents.getItem(i));BagItem.store(bag,stored);inventory.setChanged();}
    public void refreshPickup(ItemStack changed){if(changed!=bag||inventory.getItem(boundSlot)!=bag)return;var saved=BagItem.contents(bag);for(int i=0;i<18;i++)contents.setItem(i,saved.get(i));broadcastChanges();}
    @Override public boolean stillValid(Player p){return inventory.getItem(boundSlot)==bag&&(filter||BagItem.permitted(bag,p));}
    @Override public void clicked(int slot,int button,ClickType type,Player p){if(!stillValid(p))return;if(slot>=18&&slot<slots.size()&&slots.get(slot).getSlotIndex()==boundSlot)return;if(type==ClickType.SWAP&&(button==boundSlot||button==40&&boundSlot==40))return;if(filter&&slot>=0&&slot<18){if(type==ClickType.PICKUP){var stack=button==1?ItemStack.EMPTY:getCarried().copy();if(!stack.isEmpty())stack.setCount(1);contents.setItem(slot,stack);broadcastChanges();}else if(type==ClickType.QUICK_MOVE){contents.setItem(slot,ItemStack.EMPTY);broadcastChanges();}return;}super.clicked(slot,button,type,p);save();}
    @Override public ItemStack quickMoveStack(Player p,int i){if(i<0||i>=slots.size()||!stillValid(p))return ItemStack.EMPTY;var slot=slots.get(i);if(!slot.mayPickup(p)||!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();if(filter){if(i>=18)for(int f=0;f<18;f++)if(contents.getItem(f).isEmpty()){var copy=stack.copy();copy.setCount(1);contents.setItem(f,copy);break;}return ItemStack.EMPTY;}var original=stack.copy();if(i<18?!moveItemStackTo(stack,18,54,true):!moveItemStackTo(stack,0,18,false))return ItemStack.EMPTY;if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();save();return original;}
    @Override public void removed(Player p){save();super.removed(p);}
}
