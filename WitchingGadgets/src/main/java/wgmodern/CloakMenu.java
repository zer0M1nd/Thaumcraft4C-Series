package wgmodern;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

public class CloakMenu extends AbstractContainerMenu {
    private final Inventory inv;private final String slotName;private final int slotIndex;private final ItemStack cloak;public final SimpleContainer contents;
    public CloakMenu(int id,Inventory inv,String name,int index){super(Content.CLOAK_MENU.get(),id);this.inv=inv;slotName=name;slotIndex=index;cloak=equipped();contents=new SimpleContainer(27){@Override public void setChanged(){super.setChanged();if(!inv.player.level().isClientSide)save();}};var stored=CloakItem.contents(cloak);for(int i=0;i<27;i++){contents.setItem(i,stored.get(i));addSlot(new Slot(contents,i,8+i%9*18,9+i/9*18){@Override public boolean mayPlace(ItemStack s){return s.getItem().canFitInsideContainerItems();}});}for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+y*9+x,8+x*18,82+y*18));for(int x=0;x<9;x++)addSlot(new Slot(inv,x,8+x*18,140));}
    private ItemStack equipped(){return CuriosApi.getCuriosInventory(inv.player).resolve().flatMap(i->i.getStacksHandler(slotName)).map(h->slotIndex>=0&&slotIndex<h.getStacks().getSlots()?h.getStacks().getStackInSlot(slotIndex):ItemStack.EMPTY).orElse(ItemStack.EMPTY);}
    private void save(){if(contents==null||cloak.isEmpty())return;var data=net.minecraft.core.NonNullList.withSize(27,ItemStack.EMPTY);for(int i=0;i<27;i++)data.set(i,contents.getItem(i));CloakItem.store(cloak,data);}
    @Override public boolean stillValid(Player p){return !cloak.isEmpty()&&equipped()==cloak&&cloak.getItem() instanceof CloakItem item&&item.kind.equals("storage");}
    @Override public void clicked(int slot,int button,ClickType type,Player p){if(!stillValid(p))return;super.clicked(slot,button,type,p);save();}
    @Override public ItemStack quickMoveStack(Player p,int i){if(i<0||i>=slots.size()||!stillValid(p))return ItemStack.EMPTY;var slot=slots.get(i);var s=slot.getItem();if(s.isEmpty())return ItemStack.EMPTY;var old=s.copy();if(i<27?!moveItemStackTo(s,27,63,true):!moveItemStackTo(s,0,27,false))return ItemStack.EMPTY;if(s.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();save();return old;}
    @Override public void removed(Player p){save();super.removed(p);}
}
