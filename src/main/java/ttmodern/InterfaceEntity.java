package ttmodern;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.energy.IEnergyStorage;
import tc4revival.aspect.*;
import tc4revival.content.*;

/** Same-face remote access. Proxies re-resolve on every operation, including cached pipe handles. */
public final class InterfaceEntity extends BlockEntity implements TransvectorLink, WorldlyContainer, EssentiaTransport, EssentiaVessel, EssentiaSource, RevealedAspectContainer {
    private BlockPos target;
    private BlockState camouflage;
    private BlockEntity observedDestination;
    private EuBridge euBridge;
    private final java.util.List<Runnable> targetInvalidators=new java.util.ArrayList<>();
    /** Optional integrations attach handles to a particular link, never to a mutable target. */
    public void onTargetInvalidated(Runnable listener){targetInvalidators.add(listener);}
    private void invalidateTargetHandles(){if(euBridge!=null)euBridge.detach();var listeners=java.util.List.copyOf(targetInvalidators);targetInvalidators.clear();listeners.forEach(Runnable::run);}
    public void refreshDestination(){var current=destination();if(current!=observedDestination){observedDestination=current;invalidateTargetHandles();if(level!=null)level.updateNeighborsAt(worldPosition,getBlockState().getBlock());}if(level!=null&&!level.isClientSide&&net.minecraftforge.fml.ModList.get().isLoaded("ic2")){if(euBridge==null)euBridge=new EuBridge(this);euBridge.refresh();}}

    private final java.util.Map<String,LazyOptional<?>> proxies=new java.util.HashMap<>();
    private boolean capabilitiesValid=true;
    public InterfaceEntity(BlockPos pos,BlockState state){super(Tinkerer.INTERFACE_ENTITY.get(),pos,state);}
    public BlockPos targetPosition(){return target;}
    public BlockState camouflage(){return camouflage;}
    public boolean link(BlockPos pos){
        if(level==null || pos.equals(worldPosition) || Math.abs((long)pos.getX()-worldPosition.getX())>4 || Math.abs((long)pos.getY()-worldPosition.getY())>4 || Math.abs((long)pos.getZ()-worldPosition.getZ())>4 || !level.hasChunkAt(pos))return false;
        var entity=level.getBlockEntity(pos);if(entity==null || entity instanceof InterfaceEntity)return false;
        invalidateTargetHandles();target=pos.immutable();observedDestination=destination();changed();return true;
    }
    public BlockEntity destination(){
        if(isRemoved() || level==null || target==null || !level.hasChunkAt(target))return null;
        if(Math.abs((long)target.getX()-worldPosition.getX())>4 || Math.abs((long)target.getY()-worldPosition.getY())>4 || Math.abs((long)target.getZ()-worldPosition.getZ())>4)return null;
        var entity=level.getBlockEntity(target);return entity==null || entity.isRemoved() || entity instanceof InterfaceEntity?null:entity;
    }
    public void camouflage(BlockState state){camouflage=state;changed();if(level!=null)level.setBlock(worldPosition,getBlockState().setValue(InterfaceBlock.CAMOUFLAGED,state!=null),3);}
    private void changed(){setChanged();if(level!=null){level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);level.updateNeighborsAt(worldPosition,getBlockState().getBlock());}}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(target!=null)tag.putLong("Target",target.asLong());if(camouflage!=null)tag.put("Camouflage",NbtUtils.writeBlockState(camouflage));}
    @Override public void load(CompoundTag tag){super.load(tag);invalidateTargetHandles();target=tag.contains("Target",Tag.TAG_LONG)?BlockPos.of(tag.getLong("Target")):null;camouflage=tag.contains("Camouflage")?NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),tag.getCompound("Camouflage")):null;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    private <T> T capability(Capability<T> cap,Direction side){var entity=destination();return entity==null?null:entity.getCapability(cap,side).orElse(null);}
    @SuppressWarnings("unchecked") @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){
        if(isRemoved()||!capabilitiesValid)return LazyOptional.empty();
        String key=(cap==ForgeCapabilities.ITEM_HANDLER?"item":cap==ForgeCapabilities.FLUID_HANDLER?"fluid":cap==ForgeCapabilities.ENERGY?"energy":"")+":"+side;
        if(key.startsWith(":"))return super.getCapability(cap,side);
        return (LazyOptional<T>)proxies.computeIfAbsent(key,k->LazyOptional.of(()->cap==ForgeCapabilities.ITEM_HANDLER?new ItemProxy(side):cap==ForgeCapabilities.FLUID_HANDLER?new FluidProxy(side):new EnergyProxy(side)));
    }
    @Override public void invalidateCaps(){invalidateTargetHandles();observedDestination=null;super.invalidateCaps();capabilitiesValid=false;proxies.values().forEach(LazyOptional::invalidate);proxies.clear();}
    @Override public void reviveCaps(){super.reviveCaps();capabilitiesValid=true;}
    private final class ItemProxy implements IItemHandler {
        private final Direction side;ItemProxy(Direction side){this.side=side;}
        private IItemHandler handler(){return capability(ForgeCapabilities.ITEM_HANDLER,side);}
        public int getSlots(){var h=handler();return h==null?0:h.getSlots();}
        private boolean valid(IItemHandler h,int slot){return h!=null && slot>=0 && slot<h.getSlots();}
        public ItemStack getStackInSlot(int slot){var h=handler();return valid(h,slot)?h.getStackInSlot(slot):ItemStack.EMPTY;}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){var h=handler();return valid(h,slot)?h.insertItem(slot,stack,simulate):stack;}
        public ItemStack extractItem(int slot,int amount,boolean simulate){var h=handler();return valid(h,slot)?h.extractItem(slot,amount,simulate):ItemStack.EMPTY;}
        public int getSlotLimit(int slot){var h=handler();return valid(h,slot)?h.getSlotLimit(slot):0;}
        public boolean isItemValid(int slot,ItemStack stack){var h=handler();return valid(h,slot)&&h.isItemValid(slot,stack);}
    }
    private final class FluidProxy implements IFluidHandler {
        private final Direction side;FluidProxy(Direction side){this.side=side;}
        private IFluidHandler handler(){return capability(ForgeCapabilities.FLUID_HANDLER,side);}
        public int getTanks(){var h=handler();return h==null?0:h.getTanks();}
        private boolean valid(IFluidHandler h,int tank){return h!=null&&tank>=0&&tank<h.getTanks();}
        public FluidStack getFluidInTank(int tank){var h=handler();return valid(h,tank)?h.getFluidInTank(tank):FluidStack.EMPTY;}
        public int getTankCapacity(int tank){var h=handler();return valid(h,tank)?h.getTankCapacity(tank):0;}
        public boolean isFluidValid(int tank,FluidStack fluid){var h=handler();return valid(h,tank)&&h.isFluidValid(tank,fluid);}
        public int fill(FluidStack fluid,FluidAction action){var h=handler();return h==null?0:h.fill(fluid,action);}
        public FluidStack drain(FluidStack fluid,FluidAction action){var h=handler();return h==null?FluidStack.EMPTY:h.drain(fluid,action);}
        public FluidStack drain(int amount,FluidAction action){var h=handler();return h==null?FluidStack.EMPTY:h.drain(amount,action);}
    }
    private final class EnergyProxy implements IEnergyStorage {
        private final Direction side;EnergyProxy(Direction side){this.side=side;}
        private IEnergyStorage handler(){return capability(ForgeCapabilities.ENERGY,side);}
        public int receiveEnergy(int amount,boolean simulate){var h=handler();return h==null?0:h.receiveEnergy(amount,simulate);}
        public int extractEnergy(int amount,boolean simulate){var h=handler();return h==null?0:h.extractEnergy(amount,simulate);}
        public int getEnergyStored(){var h=handler();return h==null?0:h.getEnergyStored();}
        public int getMaxEnergyStored(){var h=handler();return h==null?0:h.getMaxEnergyStored();}
        public boolean canExtract(){var h=handler();return h!=null&&h.canExtract();}
        public boolean canReceive(){var h=handler();return h!=null&&h.canReceive();}
    }
    private Container container(){return destination() instanceof Container c?c:null;}
    public int getContainerSize(){var c=container();return c==null?0:c.getContainerSize();}
    public boolean isEmpty(){var c=container();return c==null||c.isEmpty();}
    @Override public int getMaxStackSize(){var c=container();return c==null?64:c.getMaxStackSize();}
    private boolean valid(Container c,int slot){return c!=null&&slot>=0&&slot<c.getContainerSize();}
    public ItemStack getItem(int slot){var c=container();return valid(c,slot)?c.getItem(slot):ItemStack.EMPTY;}
    public ItemStack removeItem(int slot,int count){var c=container();return valid(c,slot)?c.removeItem(slot,count):ItemStack.EMPTY;}
    public ItemStack removeItemNoUpdate(int slot){var c=container();return valid(c,slot)?c.removeItemNoUpdate(slot):ItemStack.EMPTY;}
    public void setItem(int slot,ItemStack stack){var c=container();if(valid(c,slot))c.setItem(slot,stack);}
    public boolean stillValid(Player player){return !isRemoved()&&player.distanceToSqr(worldPosition.getCenter())<=64;}
    public void clearContent(){var c=container();if(c!=null)c.clearContent();}
    @Override public void setChanged(){super.setChanged();var c=container();if(c!=null)c.setChanged();}
    public boolean canPlaceItem(int slot,ItemStack stack){var c=container();return valid(c,slot)&&c.canPlaceItem(slot,stack);}
    public int[] getSlotsForFace(Direction face){var c=container();return c instanceof WorldlyContainer w?w.getSlotsForFace(face):java.util.stream.IntStream.range(0,getContainerSize()).toArray();}
    public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction face){var c=container();return valid(c,slot)&&(c instanceof WorldlyContainer w?w.canPlaceItemThroughFace(slot,stack,face):c.canPlaceItem(slot,stack));}
    public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction face){var c=container();return valid(c,slot)&&(!(c instanceof WorldlyContainer w)||w.canTakeItemThroughFace(slot,stack,face));}
    private EssentiaTransport transport(){return destination() instanceof EssentiaTransport t?t:null;}
    public boolean connectable(Direction face){var t=transport();return t!=null&&t.connectable(face);}
    public boolean canInput(Direction face){var t=transport();return t!=null&&t.canInput(face);}
    public boolean canOutput(Direction face){var t=transport();return t!=null&&t.canOutput(face);}
    public Aspect suctionType(Direction face){var t=transport();return t==null?null:t.suctionType(face);}
    public int suction(Direction face){var t=transport();return t==null?0:t.suction(face);}
    public int minimumSuction(){var t=transport();return t==null?0:t.minimumSuction();}
    public Aspect essentiaType(Direction face){var t=transport();return t==null?null:t.essentiaType(face);}
    public int essentiaAmount(Direction face){var t=transport();return t==null?0:t.essentiaAmount(face);}
    public int takeEssentia(Aspect aspect,int amount,Direction face){var t=transport();return t==null?0:t.takeEssentia(aspect,amount,face);}
    public int addEssentia(Aspect aspect,int amount,Direction face){var t=transport();return t==null?0:t.addEssentia(aspect,amount,face);}
    private EssentiaVessel vessel(){return destination() instanceof EssentiaVessel v?v:null;}
    public boolean takeFromContainer(Aspect a,int n){var d=destination();return d instanceof EssentiaSource s?s.takeFromContainer(a,n):d instanceof EssentiaVessel v&&v.takeFromContainer(a,n);}
    public boolean contains(Aspect a,int n){var d=destination();return d instanceof EssentiaSource s?s.contains(a,n):d instanceof EssentiaVessel v&&v.contains(a,n);}
    @Override public java.util.List<? extends EssentiaSource> availabilitySources(){var d=destination();return d instanceof EssentiaSource s?java.util.List.of(s):java.util.List.of(this);}
    @Override public Object availabilityIdentity(){var d=destination();return d==null?this:d;}
    public Aspect aspect(){var v=vessel();return v==null?null:v.aspect();}
    public int amount(){var v=vessel();return v==null?0:v.amount();}
    public int capacity(){var v=vessel();return v==null?0:v.capacity();}
    public boolean accepts(Aspect a){var v=vessel();return v!=null&&v.accepts(a);}
    public int addToContainer(Aspect a,int n){var v=vessel();return v==null?n:v.addToContainer(a,n);}
    public boolean acceptsPhials(){var v=vessel();return v!=null&&v.acceptsPhials();}
    public boolean mirror(){return destination() instanceof EssentiaSource s&&s.mirror();}
    public net.minecraft.core.GlobalPos mirrorDestination(){return destination() instanceof EssentiaSource s?s.mirrorDestination():null;}
    public AspectAmounts revealedAspects(){return destination() instanceof RevealedAspectContainer r?r.revealedAspects():new AspectAmounts();}
}
