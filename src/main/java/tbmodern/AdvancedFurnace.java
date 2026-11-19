package tbmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import tc4revival.aspect.*;
import tc4revival.content.*;

/** Own furnace controller; the existing container/menu and alembic protocol are reusable public APIs. */
public final class AdvancedFurnace extends BlockEntity implements WorldlyContainer,MenuProvider,thaumcraft.api.aspects.EssentiaSmelter {
    private NonNullList<ItemStack> inventory=NonNullList.withSize(2,ItemStack.EMPTY);
    private AspectAmounts contents=new AspectAmounts();
    private int burn,totalBurn,cook,cookLimit=100,distributionTicks;
    private boolean alumentum;
    private final EnumMap<Direction,LazyOptional<IItemHandlerModifiable>> handlers=new EnumMap<>(Direction.class);
    private LazyOptional<IItemHandlerModifiable> unsided=LazyOptional.of(()->new net.minecraftforge.items.wrapper.InvWrapper(this));
    public AdvancedFurnace(BlockPos pos,BlockState state) {super(Content.ADVANCED_FURNACE.get(),pos,state);initHandlers();}
    @Override public AspectAmounts smeltedAspects(){return contents.copy();}
    @Override public int exportInterval(){return alumentum?20:40;}
    @Override public int extractSmelted(Aspect aspect,int amount,boolean simulate){
        int taken=aspect==null?0:Math.min(Math.max(0,amount),contents.amount(aspect));
        if(taken>0&&!simulate){contents.remove(aspect,taken);setChanged();}
        return taken;
    }
    public int vis() {return (int)contents.total();}
    public final ContainerData data=new ContainerData() {
        public int get(int index) {return switch(index) {case 0->cook;case 1->burn;case 2->totalBurn;case 3->vis();case 4->cookLimit;default->0;};}
        public void set(int index,int value) {}
        public int getCount() {return 5;}
    };
    public static void tick(Level level,BlockPos pos,BlockState state,AdvancedFurnace tile) {
        if(tile.burn>0)tile.burn--;
        int speed=tile.alumentum || !Configuration.ALUMENTUM.get()?Configuration.FURNACE_SPEED.get():1;
        int interval=tile.alumentum?20:40;
        tile.distributionTicks+=Math.min(speed,Math.max(1,20-(tile.distributionTicks%20)));
        if(tile.distributionTicks>=interval) {tile.distributionTicks%=interval;tile.distribute();}
        var input=AlchemyFurnaceBlockEntity.inputAspects(tile.inventory.get(0));
        long amount=input.total();boolean canSmelt=amount>0 && amount<=50-tile.vis();
        if(canSmelt) {
            int bellows=BellowsBlockEntity.count(level,pos,List.of(Direction.values()));
            tile.cookLimit=Math.max(1,(int)(amount*10*(1-.125F*bellows)));
        }
        if(canSmelt && tile.burn<=0) {
            var fuel=tile.inventory.get(1);int duration=AlchemyFurnaceBlockEntity.fuelTime(fuel);
            if(duration>0) {
                tile.burn=tile.totalBurn=duration;tile.alumentum=fuel.is(TCItems.ALUMENTUM.get());
                var remainder=fuel.hasCraftingRemainingItem()?fuel.getCraftingRemainingItem():ItemStack.EMPTY;
                fuel.shrink(1);if(fuel.isEmpty())tile.inventory.set(1,remainder);
            }
        }
        if(canSmelt && tile.burn>0) {
            if(++tile.cook>=tile.cookLimit) {tile.contents.add(input);tile.inventory.get(0).shrink(1);tile.cook=0;}
        } else tile.cook=0;
        var updated=state.setValue(Device.LIT,tile.burn>0).setValue(Device.CONTENTS,tile.vis()>0);
        if(updated!=state)level.setBlock(pos,updated,3);
        tile.setChanged();
    }
    private void distribute() {
        if(contents.total()<=0)return;
        var alembics=new ArrayList<AlembicBlockEntity>();
        for(int i=1;i<=5;i++) {
            BlockPos target=worldPosition.above(i);
            if(!level.hasChunkAt(target) || !(level.getBlockEntity(target) instanceof AlembicBlockEntity vessel))break;
            alembics.add(vessel);
        }
        var selected=new HashSet<Aspect>();
        for(var vessel:alembics)if(vessel.aspect()!=null && vessel.amount()<vessel.capacity() && send(vessel,vessel.aspect()))selected.add(vessel.aspect());
        for(var vessel:alembics) {
            if(vessel.aspect()!=null && vessel.amount()>0)continue;
            Aspect aspect=vessel.filter();
            if(aspect==null) {
                var choices=contents.aspects().stream().filter(a->contents.amount(a)>0 && !selected.contains(a)).toList();
                if(choices.isEmpty())continue;aspect=choices.get(level.random.nextInt(choices.size()));
            }
            if(send(vessel,aspect))break;
        }
    }
    private boolean send(AlembicBlockEntity vessel,Aspect aspect) {
        if(contents.amount(aspect)<1)return false;
        if(vessel.addToContainer(aspect,1)!=0)return false;
        contents.remove(aspect,1);return true;
    }
    @Override public int getContainerSize() {return 2;}
    @Override public boolean isEmpty() {return inventory.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int index) {return index>=0 && index<2?inventory.get(index):ItemStack.EMPTY;}
    @Override public ItemStack removeItem(int index,int count) {var stack=ContainerHelper.removeItem(inventory,index,count);if(!stack.isEmpty())setChanged();return stack;}
    @Override public ItemStack removeItemNoUpdate(int index) {return ContainerHelper.takeItem(inventory,index);}
    @Override public void setItem(int index,ItemStack stack) {inventory.set(index,stack);if(stack.getCount()>stack.getMaxStackSize())stack.setCount(stack.getMaxStackSize());setChanged();}
    @Override public void clearContent() {inventory.clear();setChanged();}
    @Override public boolean stillValid(Player player) {return !isRemoved() && level!=null && level.getBlockEntity(worldPosition)==this && player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
    // Both slots are ordinary container slots on every face; fuel gets first refusal.
    @Override public int[] getSlotsForFace(Direction face) {return new int[]{1,0};}
    @Override public boolean canPlaceItem(int slot,ItemStack stack) {return slot==0?!AlchemyFurnaceBlockEntity.inputAspects(stack).isEmpty():AlchemyFurnaceBlockEntity.fuelTime(stack)>0;}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction face) {return canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction face) {return slot==0 || slot==1;}
    @Override public Component getDisplayName() {return Component.translatable("block.thaumicbases.advanced_alchemical_furnace");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player) {return new AlchemyFurnaceMenu(id,inventory,this,data);}
    private void initHandlers() {for(Direction face:Direction.values())handlers.put(face,LazyOptional.of(()->new SidedInvWrapper(this,face)));}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction face) {
        return !isRemoved() && capability==ForgeCapabilities.ITEM_HANDLER?(face==null?unsided:handlers.get(face)).cast():super.getCapability(capability,face);
    }
    @Override public void invalidateCaps() {super.invalidateCaps();unsided.invalidate();handlers.values().forEach(LazyOptional::invalidate);}
    @Override public void reviveCaps() {super.reviveCaps();unsided=LazyOptional.of(()->new net.minecraftforge.items.wrapper.InvWrapper(this));initHandlers();}
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,inventory);var essence=new CompoundTag();AspectNbt.write(essence,contents);tag.put("Essentia",essence);
        tag.putInt("Burn",burn);tag.putInt("TotalBurn",totalBurn);tag.putInt("Cook",cook);tag.putInt("CookLimit",cookLimit);tag.putBoolean("Alumentum",alumentum);tag.putInt("Distribution",distributionTicks);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);inventory=NonNullList.withSize(2,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,inventory);
        contents=AspectNbt.read(tag.getCompound("Essentia"),TCAspects.catalog()).amounts();
        burn=Math.max(0,tag.getInt("Burn"));totalBurn=Math.max(0,tag.getInt("TotalBurn"));cook=Math.max(0,tag.getInt("Cook"));cookLimit=Math.max(1,tag.getInt("CookLimit"));
        alumentum=tag.getBoolean("Alumentum");distributionTicks=Math.floorMod(tag.getInt("Distribution"),40);
    }
    public static final class Device extends BaseEntityBlock {
        public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
        public static final BooleanProperty LIT=BlockStateProperties.LIT,CONTENTS=BooleanProperty.create("contents");
        public Device() {super(Properties.of().strength(2,3).lightLevel(s->s.getValue(LIT)?12:0));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(LIT,false).setValue(CONTENTS,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {builder.add(FACING,LIT,CONTENTS);}
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new AdvancedFurnace(pos,state);}
        @Override public RenderShape getRenderShape(BlockState state) {return RenderShape.MODEL;}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {return level.isClientSide?null:createTickerHelper(type,Content.ADVANCED_FURNACE.get(),AdvancedFurnace::tick);}
        @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
            if(player.isShiftKeyDown())return InteractionResult.PASS;
            if(player instanceof net.minecraft.server.level.ServerPlayer server && level.getBlockEntity(pos) instanceof AdvancedFurnace tile)net.minecraftforge.network.NetworkHooks.openScreen(server,tile,pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        @Override public boolean hasAnalogOutputSignal(BlockState state) {return true;}
        @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos) {return level.getBlockEntity(pos) instanceof AdvancedFurnace tile?AbstractContainerMenu.getRedstoneSignalFromContainer(tile):0;}
        @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
            if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof AdvancedFurnace tile) {Containers.dropContents(level,pos,tile);level.updateNeighbourForOutputSignal(pos,this);}
            super.onRemove(state,level,pos,next,moving);
        }
    }
}
