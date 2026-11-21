package automagy.content;
import automagy.logic.CraftingAllocation;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.*;
/** A prepared craft owns immutable input snapshots and can commit at most once. */
public final class WorkbenchCraftPlan {
 public record Result(ItemStack output,List<ItemStack> spills){}
 private final List<ItemStack> before;private final int[] uses,sources;private final CraftingContainer grid;
 private final int firstMaterial;private final CraftingRecipe recipe;private final ItemStack output;private boolean committed,notified;
 private WorkbenchCraftPlan(List<ItemStack> before,int[] uses,int[] sources,CraftingContainer grid,CraftingRecipe recipe,ItemStack output,boolean reserveFirst){this.firstMaterial=reserveFirst?1:0;this.before=before;this.uses=uses;this.sources=sources;this.grid=grid;this.recipe=recipe;this.output=output.copy();}
 private static CraftingContainer grid(){return new TransientCraftingContainer(new AbstractContainerMenu(null,-1){public boolean stillValid(Player p){return false;}public ItemStack quickMoveStack(Player p,int i){return ItemStack.EMPTY;}},3,3);}
 public static Optional<WorkbenchCraftPlan> prepare(Level level,List<ItemStack> template,IItemHandler inventory){
  return prepare(level,template,inventory,false);
 }
 public static Optional<WorkbenchCraftPlan> prepare(Level level,List<ItemStack> template,IItemHandler inventory,boolean reserveFirst){
  if(template.size()!=9||inventory.getSlots()!=9)throw new IllegalArgumentException("Workbench requires nine cells and slots");
  var before=new ArrayList<ItemStack>();int[] counts=new int[9];for(int i=0;i<9;i++){before.add(inventory.getStackInSlot(i).copy());counts[i]=before.get(i).getCount();}
  if(reserveFirst)counts[0]=0;
  var requested=new ArrayList<ItemStack>();for(var stack:template)requested.add(stack.isEmpty()?null:stack);
  var allocation=CraftingAllocation.allocate(requested,before,counts,ItemStack::isSameItemSameTags);if(allocation.isEmpty())return Optional.empty();
  int[] sources=allocation.get().gridSources();var grid=grid();for(int i=0;i<9;i++)if(sources[i]>=0){var stack=before.get(sources[i]).copy();stack.setCount(1);grid.setItem(i,stack);}
  var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level);if(recipe.isEmpty())return Optional.empty();var output=recipe.get().assemble(grid,level.registryAccess());if(output.isEmpty())return Optional.empty();
  return Optional.of(new WorkbenchCraftPlan(before,allocation.get().slotUse(),sources,grid,recipe.get(),output,reserveFirst));
 }
 public static ItemStack preview(Level level,List<ItemStack> template){var grid=grid();for(int i=0;i<9;i++)grid.setItem(i,template.get(i).copy());return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level).map(r->r.assemble(grid,level.registryAccess())).orElse(ItemStack.EMPTY);}
 public List<ItemStack> notifyCrafted(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos pos,IItemHandler inventory){
  if(!committed||notified)return List.of();notified=true;
  var fake=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.fromString("7489a048-9d57-4354-a951-aa62c42be5f8"),"AutomagyCraftingGolem"));fake.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
  var eventGrid=new net.minecraft.world.SimpleContainer(9);for(int i=0;i<9;i++)eventGrid.setItem(i,grid.getItem(i).copy());
  try{net.minecraftforge.event.ForgeEventFactory.firePlayerCraftingEvent(fake,output.copy(),eventGrid);}catch(RuntimeException error){com.mojang.logging.LogUtils.getLogger().error("Automagy workbench crafting event failed at {}",pos,error);return List.of();}
  var spills=new ArrayList<ItemStack>();for(int i=0;i<9;i++){var extra=eventGrid.getItem(i).copy();if(extra.getCount()<=1)continue;extra.shrink(1);for(int slot=8;slot>=firstMaterial&&!extra.isEmpty();slot--)extra=inventory.insertItem(slot,extra,false);if(!extra.isEmpty())spills.add(extra);}return List.copyOf(spills);
 }
 public ItemStack output(){return output.copy();}
 public ResourceLocation recipeId(){return recipe.getId();}
 public boolean advanced(){int minX=3,minY=3,maxX=-1,maxY=-1;var kinds=new ArrayList<ItemStack>();for(int i=0;i<9;i++){var s=grid.getItem(i);if(s.isEmpty())continue;minX=Math.min(minX,i%3);maxX=Math.max(maxX,i%3);minY=Math.min(minY,i/3);maxY=Math.max(maxY,i/3);if(kinds.stream().noneMatch(k->ItemStack.isSameItemSameTags(k,s)))kinds.add(s);}return kinds.size()>1&&(maxX-minX>=2||maxY-minY>=2);}
 /** Evaluate every recipe callback and stage all writes before changing the real inventory. */
 public Optional<Result> commit(Level level,IItemHandlerModifiable inventory,boolean retainContainers,net.minecraft.core.BlockPos pos){
  if(!(level instanceof net.minecraft.server.level.ServerLevel server))return Optional.empty();
  var fake=net.minecraftforge.common.util.FakePlayerFactory.get(server,new com.mojang.authlib.GameProfile(java.util.UUID.fromString("7489a048-9d57-4354-a951-aa62c42be5f8"),"AutomagyCraftingGolem"));fake.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
  var previous=net.minecraftforge.common.ForgeHooks.getCraftingPlayer();net.minecraftforge.common.ForgeHooks.setCraftingPlayer(fake);
  try{return commitInventory(level,inventory,retainContainers);}finally{net.minecraftforge.common.ForgeHooks.setCraftingPlayer(previous);}
 }
 private Optional<Result> commitInventory(Level level,IItemHandlerModifiable inventory,boolean retainContainers){
  if(committed||inventory.getSlots()!=9)return Optional.empty();for(int i=0;i<9;i++)if(!ItemStack.matches(before.get(i),inventory.getStackInSlot(i)))return Optional.empty();
  var current=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level);if(current.isEmpty()||!current.get().getId().equals(recipe.getId())||!ItemStack.matches(output,current.get().assemble(grid,level.registryAccess())))return Optional.empty();
  var remainders=current.get().getRemainingItems(grid);if(remainders.size()!=9)throw new IllegalStateException("Invalid crafting remainder grid");var staged=new ItemStackHandler(9);for(int i=0;i<9;i++){var stack=before.get(i).copy();stack.shrink(uses[i]);staged.setStackInSlot(i,stack);}
  var spills=new ArrayList<ItemStack>();for(int i=0;i<9;i++){var rest=remainders.get(i).copy();if(rest.isEmpty())continue;// Reusable crafting tools return to their cell; other containers need an order-upgraded golem.
   if(sources[i]>=firstMaterial&&rest.is(grid.getItem(i).getItem()))rest=staged.insertItem(sources[i],rest,false);if(retainContainers)for(int slot=8;slot>=firstMaterial&&!rest.isEmpty();slot--)rest=staged.insertItem(slot,rest,false);if(!rest.isEmpty())spills.add(rest);}
  committed=true;for(int i=0;i<9;i++)inventory.setStackInSlot(i,staged.getStackInSlot(i));return Optional.of(new Result(output.copy(),List.copyOf(spills)));
 }
}
