package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.*;

public  class ArcaneSpaBlockEntity extends BlockEntity implements WorldlyContainer,MenuProvider {
    private final NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);
    private boolean mix=true;
    private int counter;
    private final FluidTank tank=new FluidTank(5000) { @Override protected void onContentsChanged() { sync(); } };
    private LazyOptional<IFluidHandler> fluid=LazyOptional.of(()->tank);
    private LazyOptional<IItemHandler> sideItems=LazyOptional.of(()->new SidedInvWrapper(this,Direction.DOWN));
    private LazyOptional<IItemHandler> topItems=LazyOptional.of(()->new SidedInvWrapper(this,Direction.UP));
    public ArcaneSpaBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.ARCANE_SPA_ENTITY.get(),pos,state); }
    public FluidTank tank() { return tank; }
    public boolean mix() { return mix; }
    public void toggleMix() { mix=!mix;sync(); }
    public void tick() {
        if (level==null||level.isClientSide||counter++%40!=0||level.hasNeighborSignal(worldPosition)||tank.getFluidAmount()<1000) return;
        if (mix&&(tank.getFluid().getFluid()!=Fluids.WATER||!(getItem(0).getItem() instanceof BathSaltsItem))) return;
        var output=mix ? TCCleansing.PURE_WATER.get().defaultBlockState() : tank.getFluid().getFluid().defaultFluidState().createLegacyBlock();
        if (output.isAir() || output.is(Blocks.WATER)&&level.dimensionType().ultraWarm()) return;
        var center=worldPosition.above();
        if (legacySource(level.getBlockState(center),output.getBlock())) {
            for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
                var target=center.offset(x,0,z);if(valid(target,output.getBlock(),true)) { place(target,output);return; }
            }
        } else if(valid(center,output.getBlock(),false)) place(center,output);
    }
    private static boolean legacySource(BlockState state,Block block) {
        if(!state.is(block)) return false;
        return state.hasProperty(FiniteFluidBlock.QUANTA) ? state.getValue(FiniteFluidBlock.QUANTA)==1 : !state.hasProperty(LiquidBlock.LEVEL)||state.getValue(LiquidBlock.LEVEL)==0;
    }
    private boolean valid(BlockPos pos,Block block,boolean adjacent) {
        if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos)||!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)) return false;
        var current=level.getBlockState(pos);
        if(!current.canBeReplaced()||legacySource(current,block)) return false;
        if(!adjacent) return true;
        for(var face:Direction.values()) if(legacySource(level.getBlockState(pos.relative(face)),block)) return true;
        return false;
    }
    private void place(BlockPos pos,BlockState output) {
        // Old checkQuanta expands a metadata-zero finite fluid into a full block.
        if(output.getBlock() instanceof FiniteFluidBlock finite) output=output.setValue(FiniteFluidBlock.QUANTA,finite.capacity());
        if(!level.setBlock(pos,output,3)) return;
        if(mix) items.get(0).shrink(1);
        tank.drain(1000,IFluidHandler.FluidAction.EXECUTE);sync();
    }
    public void sync() { setChanged();if(level!=null&&!level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2); }
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return items.get(0).isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot==0?items.get(0):ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot,int amount) { var result=ContainerHelper.removeItem(items,slot,amount);if(!result.isEmpty())sync();return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items,slot); }
    @Override public void setItem(int slot,ItemStack stack) { if(slot==0) { items.set(0,stack.copyWithCount(Math.min(64,stack.getCount())));sync(); } }
    @Override public void clearContent() { items.clear();sync(); }
    @Override public boolean stillValid(Player player) { return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64; }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) { return slot==0&&stack.getItem() instanceof BathSaltsItem; }
    @Override public int[] getSlotsForFace(Direction face) { return face==Direction.UP?new int[0]:new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction face) { return face!=Direction.UP&&canPlaceItem(slot,stack); }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction face) { return face!=Direction.UP&&slot==0; }
    @Override public Component getDisplayName() { return Component.translatable("block.thaumcraft.arcane_spa"); }
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player) { return new ArcaneSpaMenu(id,inventory,this); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,items);tag.putBoolean("mix",mix);tag.put("Tank",tank.writeToNBT(new CompoundTag())); }
    @Override public void load(CompoundTag tag) { super.load(tag);items.clear();ContainerHelper.loadAllItems(tag,items);mix=tag.getBoolean("mix");tank.readFromNBT(tag.getCompound("Tank"));counter=0; }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side) {
        if(!isRemoved()) {
            if(capability==ForgeCapabilities.FLUID_HANDLER) return side==Direction.UP ? LazyOptional.empty() : fluid.cast();
            if(capability==ForgeCapabilities.ITEM_HANDLER) return (side==Direction.UP ? topItems : sideItems).cast();
        }
        return super.getCapability(capability,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps();fluid.invalidate();sideItems.invalidate();topItems.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps();fluid=LazyOptional.of(()->tank);sideItems=LazyOptional.of(()->new SidedInvWrapper(this,Direction.DOWN));topItems=LazyOptional.of(()->new SidedInvWrapper(this,Direction.UP)); }
}
