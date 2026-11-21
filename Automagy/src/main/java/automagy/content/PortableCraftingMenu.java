package automagy.content;

import java.util.*;
import net.minecraft.core.NonNullList;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import tc4revival.content.*;

/** Independent portable workbench; the wand remains in its original inventory slot. */
public final class PortableCraftingMenu extends AbstractContainerMenu {
 private final Player player;private final int ownerSlot;private final ItemStack owner;private ItemStack focus;
 private final TransientCraftingContainer grid=new TransientCraftingContainer(this,3,3);
 private final ResultContainer result=new ResultContainer();private final SimpleContainer wandDisplay=new SimpleContainer(1);
 private final SimpleContainer unavailablePreview=new SimpleContainer(1);
 private static final int PREVIEW_SLOT=47;
 public ItemStack unavailablePreview(){return unavailablePreview.getItem(0).copy();}
 private final ContainerData display=new SimpleContainerData(19);
 public static final String[] PRIMALS={"aer","terra","ignis","aqua","ordo","perditio"};
 public int displayedCost(int i){return (display.get(i*2)&65535)|(display.get(i*2+1)<<16);}
 public boolean affordable(int i){return display.get(12+i)!=0;}
 public boolean usingStaff(){return display.get(18)!=0;}
 public int ownerSlot(){return ownerSlot;}
 private CraftingRecipe normal;private ArcaneRecipe arcane;private boolean busy=true,closed;private final boolean storage;
 public PortableCraftingMenu(int id,Inventory inventory,int slot){
  super(Content.PORTABLE_CRAFTING_MENU.get(),id);player=inventory.player;ownerSlot=slot;
  owner=slot>=0&&slot<9?inventory.getItem(slot):ItemStack.EMPTY;focus=WandFocusItem.attached(owner);storage=focus.getItem() instanceof CraftingFocusItem&&CraftingFocusItem.has(focus,"storage");
  if(storage&&focus.hasTag()){var saved=NonNullList.withSize(9,ItemStack.EMPTY);ContainerHelper.loadAllItems(focus.getTag().getCompound("AutomagyCrafting"),saved);for(int i=0;i<9;i++)grid.setItem(i,saved.get(i));}
  for(int y=0;y<3;y++)for(int x=0;x<3;x++)addSlot(new Slot(grid,y*3+x,40+24*x,29+24*y));
  addSlot(new Slot(result,0,160,53){public boolean mayPlace(ItemStack stack){return false;}public boolean mayPickup(Player p){return stillValid(p)&&!getItem().isEmpty();}public void onTake(Player p,ItemStack stack){finish(stack);}});
  wandDisplay.setItem(0,owner.copy());addSlot(new Slot(wandDisplay,0,160,13){public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return false;}});
  for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inventory,9+y*9+x,16+x*18,130+y*18));
  for(int x=0;x<9;x++){int index=x;addSlot(new Slot(inventory,x,16+x*18,188){public boolean mayPlace(ItemStack s){return index!=ownerSlot;}public boolean mayPickup(Player p){return index!=ownerSlot;}});}
  // Append a non-interactive sync slot: all existing ingredient/inventory indices stay unchanged.
  addSlot(new Slot(unavailablePreview,0,-1000,-1000){public boolean isActive(){return false;}public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return false;}});
  addDataSlots(display);busy=false;refresh();
 }
 @Override public boolean stillValid(Player p){
  if(closed||p!=player||ownerSlot<0||ownerSlot>=9)return false;
  var current=p.getInventory().getItem(ownerSlot);
  if(p.level().isClientSide)return current.getItem() instanceof CastingWandItem&&WandFocusItem.attached(current).getItem() instanceof CraftingFocusItem;
  return current==owner&&owner.getItem() instanceof CastingWandItem&&ItemStack.isSameItemSameTags(WandFocusItem.attached(owner),focus);
 }
 private Set<String> knowledge(){return player instanceof net.minecraft.server.level.ServerPlayer p?tc4revival.research.KnowledgeSavedData.get(p.server).snapshot(p.getUUID()).map(tc4revival.research.PlayerKnowledge::researchMarkers).orElse(Set.of()):Set.of();}
 private boolean pay(boolean commit){
  if(arcane==null)return true;
  if(WandParts.rod(owner.getTag()).staff())return false;
  var costs=arcane.visCost(grid);if(costs.isEmpty())return true;
  var casting=(CastingWandItem)owner.getItem();var simulation=owner.copy();var extra=((CraftingFocusItem)focus.getItem()).costs(focus);
  if(!casting.consumeForPlayer(simulation,player,costs,true,true,true)||!extra.isEmpty()&&!casting.consumeForPlayer(simulation,player,extra,false,false,true))return false;
  if(commit){casting.consumeForPlayer(owner,player,costs,true,true,true);if(!extra.isEmpty())casting.consumeForPlayer(owner,player,extra,false,false,true);}return true;
 }
 private void persist(){
  if(!storage||player.level().isClientSide||!stillValid(player))return;
  var items=NonNullList.withSize(9,ItemStack.EMPTY);for(int i=0;i<9;i++)items.set(i,grid.getItem(i).copy());
  var tag=new net.minecraft.nbt.CompoundTag();ContainerHelper.saveAllItems(tag,items);focus.getOrCreateTag().put("AutomagyCrafting",tag);WandFocusItem.attach(owner,focus);player.getInventory().setChanged();
 }
 private void refresh(){
  if(busy||player.level().isClientSide||!stillValid(player))return;busy=true;
  try{var level=player.level();var known=knowledge();normal=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level).orElse(null);
   arcane=level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()).stream().filter(r->r.matches(grid,level)&&r.unlocked(grid,known::contains)).findFirst().orElse(null);
   boolean staff=WandParts.rod(owner.getTag()).staff();display.set(18,staff?1:0);
   var costs=arcane==null?Map.<tc4revival.aspect.Aspect,Integer>of():arcane.visCost(grid);
   var extra=costs.isEmpty()?Map.<tc4revival.aspect.Aspect,Integer>of():((CraftingFocusItem)focus.getItem()).costs(focus);
   var casting=(CastingWandItem)owner.getItem();
   for(int i=0;i<6;i++){var a=tc4revival.aspect.TCAspects.catalog().require(PRIMALS[i]);
    int amount=automagy.logic.PortableCraftingCosts.centivis(costs.getOrDefault(a,0),extra.getOrDefault(a,0),casting.consumptionModifier(owner,player,a,true),casting.consumptionModifier(owner,player,a,false));
    display.set(i*2,amount&65535);display.set(i*2+1,amount>>>16);display.set(12+i,!staff&&casting.vis(owner,a)>=amount?1:0);
   }
   // A staff is a normal portable workbench, never an arcane power source.
   if(staff)arcane=null;
   ItemStack output=!pay(false)?ItemStack.EMPTY:normal!=null?normal.assemble(grid,level.registryAccess()):arcane!=null?arcane.assemble(grid,level.registryAccess()):ItemStack.EMPTY;
   unavailablePreview.setItem(0,output.isEmpty()&&arcane!=null&&!staff?arcane.assemble(grid,level.registryAccess()):ItemStack.EMPTY);
   result.setItem(0,output);wandDisplay.setItem(0,owner.copy());
  }finally{busy=false;}
 }
 private void finish(ItemStack output){
  if(player.level().isClientSide||busy)return;busy=true;
  try{
   if(arcane!=null&&!pay(true))return;
   net.minecraftforge.common.ForgeHooks.setCraftingPlayer(player);
   var remaining=normal!=null?normal.getRemainingItems(grid):arcane!=null?arcane.getRemainingItems(grid):NonNullList.withSize(9,ItemStack.EMPTY);
   net.minecraftforge.event.ForgeEventFactory.firePlayerCraftingEvent(player,output,grid);output.onCraftedBy(player.level(),player,output.getCount());
   for(int i=0;i<9;i++){
    if(output.is(TCItems.ADVANCED_ALCHEMICAL_CONSTRUCT.get())&&i==4||output.is(TCItems.JAR_LABEL.get())&&output.hasTag()&&grid.getItem(i).getItem() instanceof EssentiaPhialItem)continue;
    if(!grid.getItem(i).isEmpty())grid.removeItem(i,1);var rest=remaining.get(i);if(rest.isEmpty())continue;
    if(rest.isDamageableItem()&&rest.getDamageValue()>rest.getMaxDamage()){net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(player,rest,null);continue;}
    if(player.getInventory().add(rest))continue;if(grid.getItem(i).isEmpty())grid.setItem(i,rest);else player.drop(rest,false);
   }
  }finally{net.minecraftforge.common.ForgeHooks.setCraftingPlayer(null);busy=false;persist();refresh();}
 }
 @Override public void slotsChanged(Container changed){if(!busy){persist();refresh();}}
 @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
 @Override public void clicked(int slot,int button,ClickType type,Player p){
  if(!stillValid(p)||type==ClickType.SWAP||slot==10||slot==PREVIEW_SLOT||slot>=0&&slot<slots.size()&&slots.get(slot).container==p.getInventory()&&slots.get(slot).getContainerSlot()==ownerSlot)return;
  if(slot==9){if(type==ClickType.THROW)button=1;else if(type==ClickType.PICKUP)button=0;}
  refresh();super.clicked(slot,button,type,p);persist();refresh();
 }
 @Override public boolean canTakeItemForPickAll(ItemStack stack,Slot slot){return slot.container==player.getInventory()&&slot.getContainerSlot()!=ownerSlot&&super.canTakeItemForPickAll(stack,slot);}
 @Override public ItemStack quickMoveStack(Player p,int index){
  if(!stillValid(p)||index<0||index>=PREVIEW_SLOT||index==10)return ItemStack.EMPTY;
  var slot=slots.get(index);if(!slot.mayPickup(p)||!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var original=stack.copy();
  if(index==9){int room=0;for(int i=11;i<slots.size();i++){var target=slots.get(i);if(!target.mayPlace(stack))continue;var value=target.getItem();if(value.isEmpty())room+=Math.min(stack.getMaxStackSize(),target.getMaxStackSize(stack));else if(ItemStack.isSameItemSameTags(stack,value))room+=Math.max(0,Math.min(stack.getMaxStackSize(),target.getMaxStackSize(stack))-value.getCount());}if(room<stack.getCount())return ItemStack.EMPTY;}
  if(!moveItemStackTo(stack,index<11?11:0,index<11?slots.size():9,index==9))return ItemStack.EMPTY;
  if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,original);persist();refresh();return original;
 }
 @Override public void removed(Player p){if(closed)return;persist();closed=true;super.removed(p);if(!p.level().isClientSide&&!storage){boolean first=true;for(int i=0;i<9;i++){var stack=grid.removeItemNoUpdate(i);if(!stack.isEmpty()){HoveringItemEntity.toss(p,stack,first);first=false;}}}}
}
