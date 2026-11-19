package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.*;
public final class GolemWorkbenchMenu extends AbstractContainerMenu {
 private final GolemWorkbenchBlockEntity tile;private final SimpleContainer ghosts=new SimpleContainer(10);private final ContainerData data;private int selected;
 public GolemWorkbenchMenu(int id,Inventory inv,BlockPos pos){super(Content.GOLEM_WORKBENCH_MENU.get(),id);tile=inv.player.level().getBlockEntity(pos) instanceof GolemWorkbenchBlockEntity t?t:null;
  // Keep the reserved slot index for transfer compatibility; the legacy GUI has no result slot.
  for(int i=0;i<10;i++){int x=i==9?25:59+i%3*21,y=i==9?25:5+i/3*20;addSlot(new Slot(ghosts,i,x,y){public boolean isActive(){return getContainerSlot()<9;}public boolean mayPickup(Player p){return false;}public boolean mayPlace(ItemStack s){return false;}});}
  var handler=tile==null?new ItemStackHandler(9):tile.materials();for(int i=0;i<9;i++)addSlot(new SlotItemHandler(handler,i,8+i*18,86));
  for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inv,9+r*9+c,8+c*18,123+r*18));for(int c=0;c<9;c++)addSlot(new Slot(inv,c,8+c*18,181));
  data=tile==null||inv.player.level().isClientSide?new SimpleContainerData(4):new ContainerData(){public int get(int i){return i==0?selected:i==1?tile.recipes():i==2?tile.process().heat():tile.usingPlaceholders()?1:0;}public void set(int i,int v){}public int getCount(){return 4;}};addDataSlots(data);refresh();
 }
 public int value(int i){return data.get(i);}
 public void setGhost(Player player,int page,int slot,ItemStack value){
  if(player.level().isClientSide||!stillValid(player)||page!=selected||slot<0||slot>=9)return;
  tile.template(selected,slot,value.copyWithCount(1));broadcastChanges();
 }
 public void applyRecipe(Player player,net.minecraft.resources.ResourceLocation id,int page){
  if(player.level().isClientSide||!stillValid(player)||page!=selected||page<0||page>=tile.recipes())return;
  var found=player.level().getRecipeManager().byKey(id);
  if(found.isEmpty()||!(found.get() instanceof net.minecraft.world.item.crafting.CraftingRecipe recipe)||!recipe.canCraftInDimensions(3,3))return;
  var ingredients=recipe.getIngredients();if(ingredients.size()>9)return;
  var template=new java.util.ArrayList<ItemStack>(java.util.Collections.nCopies(9,ItemStack.EMPTY));
  int width=recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped?shaped.getWidth():3;
  if(width<1||width>3)return;
  for(int i=0;i<ingredients.size();i++){
   var ingredient=ingredients.get(i);var choices=ingredient.getItems();
   if(!ingredient.isEmpty()&&choices.length==0)return;
   int slot=i/width*3+i%width;if(slot>=9)return;
   if(choices.length>0)template.set(slot,choices[0].copyWithCount(1));
  }
  tile.templateRecipe(selected,template);broadcastChanges();
 }
 private void refresh(){if(tile==null||tile.getLevel().isClientSide)return;selected=Math.min(selected,tile.recipes()-1);for(int i=0;i<9;i++)ghosts.setItem(i,tile.template(selected,i));}
 @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
 @Override public boolean stillValid(Player p){return tile!=null&&!tile.isRemoved()&&p.mayBuild()&&p.level()==tile.getLevel()&&p.level().getBlockEntity(tile.getBlockPos())==tile&&p.distanceToSqr(tile.getBlockPos().getCenter())<=64;}
 @Override public boolean clickMenuButton(Player p,int button){if(!stillValid(p)||p.level().isClientSide||button<0||button>=tile.recipes())return false;selected=button;broadcastChanges();return true;}
 @Override public void clicked(int slot,int button,ClickType type,Player p){if(!stillValid(p))return;if(slot>=0&&slot<10){if(slot<9&&!p.level().isClientSide&&(type==ClickType.PICKUP||type==ClickType.QUICK_MOVE)){tile.template(selected,slot,type==ClickType.QUICK_MOVE?ItemStack.EMPTY:getCarried());broadcastChanges();}return;}super.clicked(slot,button,type,p);}
 @Override public ItemStack quickMoveStack(Player p,int index){if(!stillValid(p)||index<10||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();if(!moveItemStackTo(stack,index<19?19:10,index<19?slots.size():19,index<19))return ItemStack.EMPTY;if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return copy;}
}
