package automagy.content;
import automagy.logic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandlerModifiable;
import tc4revival.content.*;
public final class GolemWorkbenchBlockEntity extends InventoryBlockEntity {
 private final ItemStack[][] templates=new ItemStack[3][9];private int[] upgrades={-1,-1,-1,-1};
 private final int[] replacementSlots={1,1,1};
 private final WorkbenchCycle cycle=new WorkbenchCycle();private WorkbenchUpgrades rules=WorkbenchUpgrades.from(upgrades);
 private GolemEntity worker;private ItemStack activeOutput=ItemStack.EMPTY;private net.minecraft.resources.ResourceLocation activeRecipe;private int recipeIndex,ticks;
 public GolemWorkbenchBlockEntity(BlockPos pos,BlockState state){super(Content.GOLEM_WORKBENCH_TILE.get(),pos,state,9);for(var row:templates)Arrays.fill(row,ItemStack.EMPTY);}
 public IItemHandlerModifiable materials(){return inventory;}
 public ItemStack template(int recipe,int slot){return templates[recipe][slot].copy();}
 public void templateRecipe(int recipe,java.util.List<ItemStack> values){if(recipe<0||recipe>=rules.recipes()||values.size()!=9)return;for(int i=0;i<9;i++)templates[recipe][i]=values.get(i).isEmpty()?ItemStack.EMPTY:values.get(i).copyWithCount(1);reset();sync();}
 public void template(int recipe,int slot,ItemStack value){if(recipe<0||recipe>=rules.recipes()||slot<0||slot>=9)return;var copy=value.copy();copy.setCount(copy.isEmpty()?0:1);templates[recipe][slot]=copy;reset();sync();}
 public ItemStack preview(int index){return WorkbenchCraftPlan.preview(level,Arrays.asList(templates[index]));}
 private boolean protectedMaterial(int slot){if(rules.fuzzy()&&slot==0)return true;var item=stored(slot);if(item.isEmpty())return false;for(int r=0;r<rules.recipes();r++){for(var value:templates[r])if(ItemStack.isSameItemSameTags(value,item))return true;if(rules.fuzzy()&&!stored(0).isEmpty()){var replaced=new ArrayList<ItemStack>();boolean changed=false;for(var value:templates[r]){boolean replace=ItemStack.isSameItemSameTags(value,stored(0));replaced.add(replace?item:value);changed|=replace;}if(changed&&!WorkbenchCraftPlan.preview(level,replaced).isEmpty())return true;}}return false;}
 private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> automation=makeAutomation();
 private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> makeAutomation(){return net.minecraftforge.common.util.LazyOptional.of(()->new net.minecraftforge.items.IItemHandler(){
  public int getSlots(){return 9;}public ItemStack getStackInSlot(int slot){return stored(slot).copy();}public int getSlotLimit(int slot){return inventory.getSlotLimit(slot);}
  public boolean isItemValid(int slot,ItemStack stack){return !(rules.fuzzy()&&slot==0)&&inventory.isItemValid(slot,stack);}
  public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return isItemValid(slot,stack)?inventory.insertItem(slot,stack,simulate):stack;}
  public ItemStack extractItem(int slot,int amount,boolean simulate){return protectedMaterial(slot)?ItemStack.EMPTY:inventory.extractItem(slot,amount,simulate);}
 });}
 @Override public void reviveCaps(){super.reviveCaps();automation=makeAutomation();}
 @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap,Direction side){return cap==net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER&&!isRemoved()?automation.cast():super.getCapability(cap,side);}
 @Override public void invalidateCaps(){super.invalidateCaps();automation.invalidate();}
 public int recipes(){return rules.recipes();}
 public boolean usingPlaceholders(){return rules.fuzzy();}
 public WorkbenchCycle.State process(){return cycle.state();}
 public boolean installUpgrade(int id,int socket){if((id!=0&&id!=1&&id!=3&&id!=5)||socket<0||socket>=4||upgrades[socket]>=0)return false;upgrades[socket]=id;refresh();sync();return true;}
 public int upgrade(int socket){return upgrades[socket];}
 @Override public void dropContents(){super.dropContents();if(level==null||level.isClientSide)return;for(int i=0;i<4;i++){int id=upgrades[i];upgrades[i]=-1;if(Content.WORKBENCH_UPGRADES.containsKey(id))net.minecraft.world.level.block.Block.popResource(level,worldPosition,new ItemStack(Content.WORKBENCH_UPGRADES.get(id).get()));}refresh();}
 public int removeUpgrade(int slot){if(slot<0||slot>=4)return -1;int old=upgrades[slot];upgrades[slot]=-1;refresh();sync();return old;}
 private void refresh(){rules=WorkbenchUpgrades.from(upgrades);for(int i=rules.recipes();i<3;i++)Arrays.fill(templates[i],ItemStack.EMPTY);recipeIndex=Math.min(recipeIndex,rules.recipes()-1);reset();}
 private void reset(){worker=null;activeOutput=ItemStack.EMPTY;activeRecipe=null;cycle.resetProgress();}
 private Optional<WorkbenchCraftPlan> plan(int index){
  var base=Arrays.asList(templates[index]);if(!rules.fuzzy())return WorkbenchCraftPlan.prepare(level,base,inventory);
  var placeholder=stored(0);if(placeholder.isEmpty())return WorkbenchCraftPlan.prepare(level,base,inventory,true);
  int first=cycle.state().progress()>0?replacementSlots[index]:replacementSlots[index]%8+1;int attempts=cycle.state().progress()>0?1:8;
  for(int offset=0;offset<attempts;offset++){int slot=(first-1+offset)%8+1;var substitute=stored(slot);if(substitute.isEmpty())continue;var resolved=new ArrayList<ItemStack>();for(var value:base)resolved.add(ItemStack.isSameItemSameTags(value,placeholder)?substitute:value);var plan=WorkbenchCraftPlan.prepare(level,resolved,inventory,true);if(plan.isPresent()){replacementSlots[index]=slot;return plan;}}return Optional.empty();
 }
 private boolean eligible(GolemEntity golem,boolean advanced){return golem.isAlive()&&golem.level()==level&&golem.workEnabled()&&golem.configuration().customTask()==null&&golem.configuration().core()==GolemCoreType.EMPTY&&golem.carried().isEmpty()&&(!advanced||golem.configuration().advanced())&&golem.home().equals(worldPosition.relative(golem.homeFacing()))&&golem.distanceToSqr(worldPosition.getX(),worldPosition.getY(),worldPosition.getZ())<=9;}
 /** Legacy workbenches require an explicit output sample, even when an empty core normally accepts all items. */
 public static boolean acceptsOutput(GolemEntity golem,ItemStack output){
  if(output.isEmpty())return false;
  for(int slot=0;slot<golem.filters().size();slot++)
   if(GolemItemMatching.matches(golem.filters().sample(slot),output,golem.configuration()))return true;
  return false;
 }
 private static int speed(GolemEntity golem){var c=golem.configuration();int base=automagy.api.GolemWorkbenchSpeeds.base(c.customMaterial()==null?null:c.customMaterial().toString(),c.material().name());return base+(c.decorated('B')?1:0)-(c.decorated('P')?1:0)+c.upgrades(0)+(c.advanced()?1:0);}
 public static void tick(Level level,BlockPos pos,BlockState state,GolemWorkbenchBlockEntity tile){
  if(level.isClientSide)return;tile.ticks++;tile.cycle.passive(tile.worker!=null);tile.setChanged();var below=level.getBlockState(pos.below());if(below.is(TCArcane.GOLEM_FETTER.get())&&below.getValue(GolemFetterBlock.POWERED))return;
  Optional<WorkbenchCraftPlan> found=tile.cycle.state().progress()>0?tile.plan(tile.recipeIndex):Optional.empty();if(found.isEmpty()||tile.cycle.state().progress()==0){for(int i=1;i<=tile.rules.recipes();i++){int index=(tile.recipeIndex+i)%tile.rules.recipes();var candidate=tile.plan(index);if(candidate.isPresent()){tile.recipeIndex=index;found=candidate;break;}}}
  if(found.isEmpty()){tile.reset();return;}var plan=found.get();var output=plan.output();if(!Objects.equals(tile.activeRecipe,plan.recipeId())||!ItemStack.matches(tile.activeOutput,output)){tile.reset();tile.activeRecipe=plan.recipeId();tile.activeOutput=output;}
  if(tile.worker!=null&&(!tile.eligible(tile.worker,plan.advanced())||!acceptsOutput(tile.worker,output))){tile.reset();return;}
  if(tile.worker==null){var nearby=level.getEntitiesOfClass(GolemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(3),g->tile.eligible(g,plan.advanced())&&acceptsOutput(g,output));if(nearby.isEmpty())return;tile.worker=nearby.get(0);tile.cycle.resetProgress();}
  var worker=tile.worker;boolean stationary=worker.getDeltaMovement().horizontalDistanceSqr()<1.e-8&&Math.abs(worker.getDeltaMovement().y)<.1;
  boolean done=tile.cycle.work(tile.rules,speed(worker),worker.configuration().strength(),stationary,()->tc4revival.aspect.TCAspects.catalog().find("fabrico").map(a->EssentiaSources.drain(tile,a,null,8,false).isPresent()).orElse(false));
  if(stationary&&tile.ticks%6==0){if(level.random.nextBoolean())worker.leftArm();else worker.rightArm();}
  if(tile.cycle.slowsGolem(tile.rules))worker.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,200,2));
  if(done){var result=plan.commit(level,tile.inventory,worker.configuration().upgrades(4)>0,pos);if(result.isPresent()){for(var extra:plan.notifyCrafted((net.minecraft.server.level.ServerLevel)level,pos,tile.inventory))net.minecraft.world.level.block.Block.popResource(level,pos,extra);var made=result.get().output();int held=Math.min(made.getCount(),Math.max(1,worker.configuration().carryLimit()));var carry=made.copy();carry.setCount(held);worker.carried(carry);made.shrink(held);if(!made.isEmpty())net.minecraft.world.level.block.Block.popResource(level,pos,made);for(var rest:result.get().spills())net.minecraft.world.level.block.Block.popResource(level,pos,rest);level.playSound(null,pos,net.minecraft.sounds.SoundEvents.ITEM_PICKUP,net.minecraft.sounds.SoundSource.BLOCKS,.1f,1.5f);}tile.reset();tile.sync();}
  else if(tile.ticks%10==0)tile.sync();
 }
 @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putIntArray("Upgrades",upgrades);tag.putIntArray("ReplacementSlots",replacementSlots);var list=new ListTag();for(int r=0;r<3;r++)for(int s=0;s<9;s++)if(!templates[r][s].isEmpty()){var entry=templates[r][s].save(new CompoundTag());entry.putInt("Recipe",r);entry.putInt("Cell",s);list.add(entry);}tag.put("Templates",list);tag.putInt("Heat",cycle.state().heat());tag.putInt("RecipeIndex",recipeIndex);}
 @Override public void load(CompoundTag tag){super.load(tag);var replacements=tag.getIntArray("ReplacementSlots");for(int i=0;i<3;i++)replacementSlots[i]=i<replacements.length?Math.max(1,Math.min(8,replacements[i])):1;for(var row:templates)Arrays.fill(row,ItemStack.EMPTY);for(var raw:tag.getList("Templates",Tag.TAG_COMPOUND)){var entry=(CompoundTag)raw;int r=entry.getInt("Recipe"),s=entry.getInt("Cell");if(r>=0&&r<3&&s>=0&&s<9){var stack=ItemStack.of(entry);if(!stack.isEmpty())stack.setCount(1);templates[r][s]=stack;}}var saved=tag.getIntArray("Upgrades");upgrades=new int[]{-1,-1,-1,-1};for(int i=0;i<Math.min(4,saved.length);i++)if(saved[i]==0||saved[i]==1||saved[i]==3||saved[i]==5)upgrades[i]=saved[i];refresh();recipeIndex=Math.max(0,Math.min(rules.recipes()-1,tag.getInt("RecipeIndex")));cycle.restore(new WorkbenchCycle.State(0,tag.getInt("Heat"),0));}
}
