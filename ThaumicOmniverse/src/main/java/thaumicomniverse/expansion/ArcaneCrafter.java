package thaumicomniverse.expansion;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.registries.*;
import tc4revival.content.*;
import thaumcraft.api.wands.*;
import thaumicomniverse.ThaumicOmniverse;

/** A redstone-edge crafter with Quark's blocked-slot and balanced insertion behavior. */
public class ArcaneCrafter extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED,CRAFTING=BooleanProperty.create("crafting");
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,ThaumicOmniverse.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,ThaumicOmniverse.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,ThaumicOmniverse.MOD_ID);
    public static final RegistryObject<ArcaneCrafter> BLOCK=BLOCKS.register("arcane_crafter",ArcaneCrafter::new);
    public static final RegistryObject<BlockEntityType<Tile>> TILE=TILES.register("arcane_crafter",()->BlockEntityType.Builder.of(Tile::new,BLOCK.get()).build(null));
    public static final RegistryObject<MenuType<Menu>> MENU=MENUS.register("arcane_crafter",()->net.minecraftforge.common.extensions.IForgeMenuType.create((id,inv,buffer)->new Menu(id,inv,buffer.readBlockPos())));
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus){
        ThaumicOmniverse.ITEMS.register("arcane_crafter",()->new BlockItem(BLOCK.get(),new Item.Properties()));
        BLOCKS.register(bus);TILES.register(bus);MENUS.register(bus);
    }
    public ArcaneCrafter(){super(Properties.copy(Blocks.DISPENSER).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(POWERED,false).setValue(CRAFTING,false));}
    public static boolean enabled(){return thaumicomniverse.RevivalFeatures.Group.RESTORATION.enabled();}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,POWERED,CRAFTING);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getNearestLookingDirection().getOpposite());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Tile(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:createTickerHelper(type,TILE.get(),(world,pos,block,tile)->tile.tick());
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(player.isShiftKeyDown())return InteractionResult.PASS;
        if(player instanceof ServerPlayer server&&level.getBlockEntity(pos) instanceof Tile tile)net.minecraftforge.network.NetworkHooks.openScreen(server,tile,pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block source,BlockPos sourcePos,boolean moved){
        if(level.isClientSide)return;
        boolean powered=level.hasNeighborSignal(pos)||level.hasNeighborSignal(pos.above());
        if(powered!=state.getValue(POWERED)){
            level.setBlock(pos,state.setValue(POWERED,powered),2);
            if(powered&&enabled()&&level.getBlockEntity(pos) instanceof Tile tile)tile.pulse();
        }
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return level.getBlockEntity(pos) instanceof Tile tile?tile.comparator():0;}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!next.is(this)&&level.getBlockEntity(pos) instanceof Tile tile){Containers.dropContents(level,pos,tile);level.updateNeighbourForOutputSignal(pos,this);}
        super.onRemove(state,level,pos,next,moving);
    }
    public static class Tile extends VisNetworkNodeBlockEntity implements WorldlyContainer,MenuProvider {
        private final NonNullList<ItemStack> items=NonNullList.withSize(10,ItemStack.EMPTY);
        private int blocked,animation;
        private boolean crafting;
        private LazyOptional<IItemHandler> automation=LazyOptional.of(()->new SidedInvWrapper(this,Direction.UP));
        public Tile(BlockPos pos,BlockState state){super(TILE.get(),pos,state);}
        @Override public int range(){return 8;}
        @Override public boolean source(){return false;}
        @Override public boolean relay(){return false;}
        public void tick(){
            if(level==null||level.isClientSide||!enabled())return;
            tickNetwork();if(VisCharging.charge(getItem(9),this::consumeVis))setChanged();
            if(animation>0&&--animation==0)level.setBlock(worldPosition,getBlockState().setValue(CRAFTING,false),2);
        }
        private record Plan(Recipe<CraftingContainer> recipe,CraftingContainer grid,ItemStack output){}
        private Plan plan(){
            if(!(level instanceof ServerLevel server)||!validWand(getItem(9)))return null;
            var dummy=new AbstractContainerMenu(null,-1){
                @Override public boolean stillValid(Player player){return true;}
                @Override public ItemStack quickMoveStack(Player player,int index){return ItemStack.EMPTY;}
            };
            var grid=new TransientCraftingContainer(dummy,3,3);
            for(int i=0;i<9;i++)grid.setItem(i,getItem(i).copy());
            var core=WandAutomation.core(getItem(9));var identity=(AutomationCore)core.getItem();
            var knowledge=identity.research(core,server);
            for(var recipe:level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()))
                if(recipe.matches(grid,level)&&recipe.unlocked(grid,knowledge::contains)){
                    var output=recipe.assemble(grid,level.registryAccess());
                    if(!output.isEmpty())return new Plan(recipe,grid,output);
                }
            return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level)
                    .map(recipe->new Plan(recipe,grid,recipe.assemble(grid,level.registryAccess()))).orElse(null);
        }
        public ItemStack preview(){var plan=plan();return plan==null?ItemStack.EMPTY:plan.output().copy();}
        public void pulse(){
            if(crafting||!enabled()||!(level instanceof ServerLevel server))return;
            var plan=plan();if(plan==null||plan.output().isEmpty())return;
            var direction=getBlockState().getValue(FACING);var target=worldPosition.relative(direction);
            if(!level.hasChunkAt(target))return;
            var receiver=level.getBlockEntity(target);
            var handler=receiver==null?null:receiver.getCapability(ForgeCapabilities.ITEM_HANDLER,direction.getOpposite()).resolve().orElse(null);
            if(handler!=null&&!ItemHandlerHelper.insertItemStacked(handler,plan.output().copy(),true).isEmpty())return;
            var wand=(CastingWandItem)getItem(9).getItem();
            var fake=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(server);
            if(plan.recipe() instanceof ArcaneRecipe arcane&&!wand.consumeForPlayer(getItem(9),fake,arcane.visCost(plan.grid()),true,true,false))return;
            var remaining=plan.recipe().getRemainingItems(plan.grid());
            crafting=true;
            try{
                if(plan.recipe() instanceof ArcaneRecipe arcane&&!wand.consumeForPlayer(getItem(9),fake,arcane.visCost(plan.grid()),true,true,true))return;
                for(int i=0;i<9;i++){
                    if(!items.get(i).isEmpty())items.get(i).shrink(1);
                    var rest=remaining.get(i).copy();if(rest.isEmpty())continue;
                    if(items.get(i).isEmpty())items.set(i,rest);
                    else if(ItemStack.isSameItemSameTags(items.get(i),rest)&&items.get(i).getCount()+rest.getCount()<=items.get(i).getMaxStackSize())items.get(i).grow(rest.getCount());
                    else eject(rest,direction);
                }
                var leftover=handler==null?plan.output().copy():ItemHandlerHelper.insertItemStacked(handler,plan.output().copy(),false);
                if(!leftover.isEmpty())eject(leftover,direction);
                animation=6;level.setBlock(worldPosition,getBlockState().setValue(CRAFTING,true),2);
                level.levelEvent(1000,worldPosition,0);
            }finally{crafting=false;setChanged();}
        }
        private void eject(ItemStack stack,Direction direction){
            var point=net.minecraft.world.phys.Vec3.atCenterOf(worldPosition).add(direction.getStepX()*.7,direction.getStepY()*.7,direction.getStepZ()*.7);
            var entity=new net.minecraft.world.entity.item.ItemEntity(level,point.x,point.y,point.z,stack);
            entity.setDeltaMovement(direction.getStepX()*.15,direction.getStepY()*.15+.05,direction.getStepZ()*.15);level.addFreshEntity(entity);
        }
        public static boolean validWand(ItemStack stack){
            if(!(stack.getItem() instanceof CastingWandItem wand)||wand.isStaff(stack))return false;
            var core=WandAutomation.core(stack);
            return core.getItem() instanceof AutomationCore identity&&identity.enabled(core)&&identity.owner(core)!=null;
        }
        public boolean blocked(int slot){return slot>=0&&slot<9&&(blocked&(1<<slot))!=0;}
        public boolean toggle(int slot){
            if(slot<0||slot>=9||!getItem(slot).isEmpty())return false;
            blocked^=1<<slot;setChanged();return true;
        }
        public int comparator(){int value=0;for(int i=0;i<9;i++)if(blocked(i)||!getItem(i).isEmpty())value++;return value;}
        @Override public int getContainerSize(){return 10;}
        @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
        @Override public ItemStack getItem(int slot){return items.get(slot);}
        @Override public ItemStack removeItem(int slot,int count){var value=ContainerHelper.removeItem(items,slot,count);if(!value.isEmpty())setChanged();return value;}
        @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(items,slot);}
        @Override public void setItem(int slot,ItemStack stack){items.set(slot,stack.copyWithCount(Math.min(stack.getCount(),slot==9?1:stack.getMaxStackSize())));setChanged();}
        @Override public void clearContent(){items.clear();setChanged();}
        @Override public boolean stillValid(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64;}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==9?validWand(stack):!blocked(slot);}
        @Override public int[] getSlotsForFace(Direction side){return java.util.stream.IntStream.range(0,10).toArray();}
        @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){
            if(!canPlaceItem(slot,stack))return false;
            if(slot==9)return true;
            if(validWand(stack))return false;
            var here=getItem(slot);if(!here.isEmpty()&&!ItemStack.isSameItemSameTags(here,stack))return false;
            for(int i=0;i<9;i++)if(!blocked(i)){
                var other=getItem(i);
                if(other.isEmpty()&&!here.isEmpty()||ItemStack.isSameItemSameTags(other,stack)&&other.getCount()<here.getCount())return false;
            }
            return true;
        }
        @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return slot<9;}
        @Override public void setChanged(){super.setChanged();if(level!=null&&!level.isClientSide&&!crafting){level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());}}
        @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,items);tag.putInt("Blocked",blocked);tag.putInt("Animation",animation);}
        @Override public void load(CompoundTag tag){super.load(tag);items.clear();ContainerHelper.loadAllItems(tag,items);blocked=tag.getInt("Blocked")&511;animation=Math.max(0,Math.min(6,tag.getInt("Animation")));for(int i=0;i<9;i++)if(!items.get(i).isEmpty())blocked&=~(1<<i);}
        @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
        @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
        @Override public Component getDisplayName(){return Component.translatable("block.thaumicomniverse.arcane_crafter");}
        @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new Menu(id,inventory,worldPosition);}
        @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.ITEM_HANDLER&&!isRemoved()?automation.cast():super.getCapability(cap,side);}
        @Override public void invalidateCaps(){super.invalidateCaps();automation.invalidate();}
        @Override public void reviveCaps(){super.reviveCaps();automation=LazyOptional.of(()->new SidedInvWrapper(this,Direction.UP));}
    }
    public static class Menu extends AbstractContainerMenu {
        public final Tile tile;
        private final ResultContainer result=new ResultContainer();
        public Menu(int id,Inventory inventory,BlockPos pos){
            super(MENU.get(),id);tile=(Tile)inventory.player.level().getBlockEntity(pos);
            addSlot(new Slot(result,0,134,35){@Override public boolean mayPlace(ItemStack stack){return false;}@Override public boolean mayPickup(Player player){return false;}});
            for(int row=0;row<3;row++)for(int col=0;col<3;col++){
                int slot=row*3+col;
                addSlot(new Slot(tile,slot,26+col*18,17+row*18){@Override public boolean mayPlace(ItemStack stack){return tile.canPlaceItem(slot,stack);}});
            }
            addSlot(new Slot(tile,9,108,58){@Override public int getMaxStackSize(){return 1;}@Override public boolean mayPlace(ItemStack stack){return Tile.validWand(stack);}});
            for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,84+row*18));
            for(int i=0;i<9;i++)addSlot(new Slot(inventory,i,8+i*18,142));
        }
        @Override public void broadcastChanges(){if(!tile.getLevel().isClientSide)result.setItem(0,tile.preview());super.broadcastChanges();}
        @Override public boolean clickMenuButton(Player player,int id){return stillValid(player)&&tile.toggle(id);}
        @Override public boolean stillValid(Player player){return tile.stillValid(player);}
        @Override public boolean canTakeItemForPickAll(ItemStack stack,Slot slot){return slot.container!=result&&super.canTakeItemForPickAll(stack,slot);}
        @Override public ItemStack quickMoveStack(Player player,int index){
            if(index<=0||index>=slots.size()||!stillValid(player))return ItemStack.EMPTY;
            var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
            var stack=slot.getItem();var before=stack.copy();
            boolean moved=index<=10?moveItemStackTo(stack,11,47,true):Tile.validWand(stack)?moveItemStackTo(stack,10,11,false):moveItemStackTo(stack,1,10,false);
            if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return before;
        }
    }
}
