package emtmodern.energy;
import emtmodern.ElectroMagicTools;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.*;
public class EtherealProcessorEntity extends BlockEntity implements WorldlyContainer,MenuProvider {
    protected NonNullList<ItemStack> items=NonNullList.withSize(3,ItemStack.EMPTY);
    protected final java.util.Map<Direction,LazyOptional<IItemHandler>> handlers=new java.util.EnumMap<>(Direction.class);
    private boolean capabilitiesValid=true;
    private LazyOptional<IItemHandler> unsided=LazyOptional.of(()->new InvWrapper(this));
    private final RecipeManager.CachedCheck<Container,SmeltingRecipe> recipe=RecipeManager.createCheck(RecipeType.SMELTING);
    public int cookTime;
    public EtherealProcessorEntity(BlockPos pos,BlockState state){super(ElectroMagicTools.PROCESSOR_BE.get(),pos,state);}
    public static void tick(Level level,BlockPos pos,BlockState state,EtherealProcessorEntity tile){
        if(level.isClientSide)return;boolean active=tile.canSmelt()&&!tile.overLimit(1)&&!tile.overLimit(2)&&!blocked(tile.getItem(0));
        if(active){if(++tile.cookTime>=ProcessorConfig.SPEED.get()){tile.cookTime=0;tile.smelt();}}else tile.cookTime=0;
        if(state.getValue(EtherealProcessorBlock.ACTIVE)!=active)level.setBlockAndUpdate(pos,state.setValue(EtherealProcessorBlock.ACTIVE,active));tile.setChanged();
    }
    public ItemStack result(){return level==null?ItemStack.EMPTY:recipe.getRecipeFor(this,level).map(r->r.assemble(this,level.registryAccess())).orElse(ItemStack.EMPTY);}
    public boolean canSmelt(){if(getItem(0).isEmpty())return false;var result=result();if(result.isEmpty())return false;var output=getItem(2);return output.isEmpty()||ItemStack.isSameItem(output,result)&&output.getCount()+result.getCount()*2<=Math.min(64,result.getMaxStackSize());}
    public boolean overLimit(int slot){return !getItem(slot).isEmpty()&&getItem(slot).getCount()>=63;}
    public void smelt(){if(!canSmelt())return;var output=result();if(getItem(2).isEmpty())setItem(2,output.copyWithCount(output.getCount()*2));else getItem(2).grow(output.getCount()*2);getItem(0).shrink(1);bonus();setChanged();}
    public void bonus(){
        // Both original and GTNH call canSmelt AFTER input consumption: no bonus on the last item.
        if(!canSmelt()||level.random.nextInt(ProcessorConfig.BONUS.get())!=0)return;var thaumium=tc4revival.content.TCItems.nugget(tc4revival.content.NuggetType.THAUMIUM);
        if(getItem(1).isEmpty())setItem(1,new ItemStack(thaumium));else if(getItem(1).is(thaumium)&&!overLimit(1)&&!overLimit(2))setItem(1,new ItemStack(ElectroMagicTools.CONTENT.get("legacy_tin_nugget").get(),getItem(1).getCount()+1));
    }
    public static boolean blocked(ItemStack input){
        if(input.isEmpty())return true;var item=input.getItem();if(item==Items.SAND||item==Items.RED_SAND||item==Items.POTATO||item==Items.PORKCHOP||item==Items.BEEF||item==Items.CHICKEN||item==Items.COD||item==Items.SALMON||item==Items.TROPICAL_FISH||item==Items.PUFFERFISH)return true;
        if(input.getTags().anyMatch(tag->{String name=tag.location().getPath();return name.equals("cobblestone")||name.startsWith("cobblestone/")||name.contains("dust");}))return true;
        String key=item.getDescriptionId(input).toLowerCase(java.util.Locale.ROOT);for(String part:java.util.List.of("armour","armor","helm","legging","boot","chestp","pick","shovel","axe"))if(key.contains(part))return true;return false;
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,items);tag.putInt("CookTime",cookTime);}
    @Override public void load(CompoundTag tag){super.load(tag);items=NonNullList.withSize(3,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,items);cookTime=Math.max(0,tag.getInt("CookTime"));}
    @Override public int getContainerSize(){return 3;}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return slot>=0&&slot<3?items.get(slot):ItemStack.EMPTY;}
    @Override public ItemStack removeItem(int slot,int amount){var removed=ContainerHelper.removeItem(items,slot,amount);if(!removed.isEmpty())setChanged();return removed;}
    @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(items,slot);}
    @Override public void setItem(int slot,ItemStack stack){items.set(slot,stack);if(stack.getCount()>64)stack.setCount(64);setChanged();}
    @Override public void clearContent(){for(int i=0;i<3;i++)items.set(i,ItemStack.EMPTY);setChanged();}
    @Override public boolean stillValid(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0&&level!=null&&level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SimpleContainer(stack),level).isPresent();}
    @Override public int[] getSlotsForFace(Direction side){return side==Direction.UP?new int[]{0}:side==Direction.DOWN?new int[]{2,1}:new int[]{1};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return true;}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){if(cap==ForgeCapabilities.ITEM_HANDLER)return isRemoved()||!capabilitiesValid?LazyOptional.empty():(side==null?unsided:handlers.computeIfAbsent(side,key->LazyOptional.of(()->new SidedInvWrapper(this,key)))).cast();return super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();capabilitiesValid=false;unsided.invalidate();handlers.values().forEach(LazyOptional::invalidate);handlers.clear();}
    @Override public void reviveCaps(){super.reviveCaps();capabilitiesValid=true;unsided=LazyOptional.of(()->new InvWrapper(this));}
    @Override public Component getDisplayName(){return Component.translatable("gui.EMT.etherealMacerator.title");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new EtherealProcessorMenu(id,inventory,this);}
}
