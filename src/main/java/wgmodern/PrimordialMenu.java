package wgmodern;

import java.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.*;

public class PrimordialMenu extends AbstractContainerMenu {
 public final ItemStack glove;public final SimpleContainer gems;private final Inventory inventory;private final int boundSlot;private boolean loading;
 public PrimordialMenu(int id,Inventory inventory,boolean offhand){super(PrimordialContent.MENU.get(),id);this.inventory=inventory;boundSlot=offhand?40:inventory.selected;glove=inventory.getItem(boundSlot);loading=true;gems=new SimpleContainer(5);var stored=PrimordialGlove.gems(glove);for(int i=0;i<5;i++)gems.setItem(i,stored.get(i));loading=false;gems.addListener(c->save());int[][] coords={{60,6},{100,6},{57,42},{80,53},{103,42}};for(int i=0;i<5;i++){final int n=i;addSlot(new Slot(gems,i,coords[i][0],coords[i][1]){@Override public int getMaxStackSize(){return 1;}@Override public boolean mayPlace(ItemStack s){return s.getItem() instanceof InfusedGem;}@Override public void set(ItemStack s){if(!s.isEmpty())s.getOrCreateTag().putInt("BraceletSlot",n);super.set(s);}@Override public void onTake(Player p,ItemStack s){if(!s.isEmpty())s.getOrCreateTag().putInt("BraceletSlot",-1);super.onTake(p,s);}});}for(int y=0;y<3;y++)for(int x=0;x<9;x++)playerSlot(9+y*9+x,8+x*18,84+y*18);for(int x=0;x<9;x++)playerSlot(x,8+x*18,142);}
 private void playerSlot(int index,int x,int y){addSlot(new Slot(inventory,index,x,y){@Override public boolean mayPickup(Player p){return index!=boundSlot;}@Override public boolean mayPlace(ItemStack s){return index!=boundSlot;}});}
 private void save(){if(loading||inventory.player.level().isClientSide||inventory.getItem(boundSlot)!=glove)return;var list=new ArrayList<ItemStack>();for(int i=0;i<5;i++)list.add(gems.getItem(i));PrimordialGlove.gems(glove,list);inventory.setChanged();}
 public void recharge(AspectAmounts amounts){for(int i=0;i<5;i++){var s=gems.getItem(i);if(s.isDamaged())s.setDamageValue(Math.max(0,s.getDamageValue()-TCAspects.catalog().find(InfusedGem.aspect(s)).map(amounts::amount).orElse(0)));}gems.setChanged();broadcastChanges();}
 @Override public boolean stillValid(Player p){return p.isAlive()&&inventory.getItem(boundSlot)==glove&&glove.getItem() instanceof PrimordialGlove;}
 @Override public void clicked(int slot,int button,ClickType type,Player p){if(!stillValid(p)||slot>=5&&slot<slots.size()&&slots.get(slot).getSlotIndex()==boundSlot||type==ClickType.SWAP&&button==boundSlot)return;super.clicked(slot,button,type,p);save();}
 @Override public ItemStack quickMoveStack(Player p,int index){if(index<0||index>=slots.size()||!stillValid(p))return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;var s=slot.getItem();var copy=s.copy();if(index<5)s.getOrCreateTag().putInt("BraceletSlot",-1);if(index<5?!moveItemStackTo(s,5,41,true):!moveItemStackTo(s,0,5,false)){if(index<5)s.getOrCreateTag().putInt("BraceletSlot",index);return ItemStack.EMPTY;}if(s.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,s);save();return copy;}
 @Override public void removed(Player p){save();super.removed(p);}
}
