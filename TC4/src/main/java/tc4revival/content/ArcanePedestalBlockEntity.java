package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Ordinary infusion pedestal; insertion is limited to one, infusion outputs may contain more. */
public  class ArcanePedestalBlockEntity extends BlockEntity implements WorldlyContainer,thaumcraft.api.devices.DeviceExtensions.Host {
    private final thaumcraft.api.devices.DeviceExtensions.Store extensions=new thaumcraft.api.devices.DeviceExtensions.Store(this,"Items");
    @Override public thaumcraft.api.devices.DeviceExtensions.Store extensions(){return extensions;}
    private final NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);
    private String customName="";
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandlerModifiable> automation=net.minecraftforge.common.util.LazyOptional.of(()->new net.minecraftforge.items.wrapper.SidedInvWrapper(this,Direction.UP));
    public ArcanePedestalBlockEntity(BlockPos pos,BlockState state) { this(TCArcane.ARCANE_PEDESTAL_ENTITY.get(),pos,state); }
    protected ArcanePedestalBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state) { super(type,pos,state); }
    @Override public int getContainerSize() { return 1; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean isEmpty() { return items.get(0).isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot==0 ? items.get(0) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot,int amount) {
        var result=ContainerHelper.removeItem(items,slot,amount); if (!result.isEmpty()) {extensions.inventoryChanged(thaumcraft.api.devices.DeviceExtensions.InventoryChange.EXTRACTED);sync();} return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { var result=ContainerHelper.takeItem(items,slot);if(!result.isEmpty())extensions.inventoryChanged(thaumcraft.api.devices.DeviceExtensions.InventoryChange.EXTRACTED);return result; }
    @Override public void setItem(int slot,ItemStack stack) { if (slot==0) { extensions.inventoryChanged(thaumcraft.api.devices.DeviceExtensions.InventoryChange.REPLACED); items.set(0,stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1)); sync(); } }
    public void setFromInfusion(ItemStack stack) { items.set(0,stack.copy());extensions.inventoryChanged(thaumcraft.api.devices.DeviceExtensions.InventoryChange.PROCESS_RESULT); sync(); }
    @Override public void clearContent() { extensions.inventoryChanged(thaumcraft.api.devices.DeviceExtensions.InventoryChange.CLEARED); items.clear(); sync(); }
    @Override public boolean stillValid(Player player) { return level!=null && level.getBlockEntity(worldPosition)==this && player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64; }
    @Override public int[] getSlotsForFace(Direction face) { return new int[]{0}; }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) { return slot==0; }
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction face) { return slot==0 && isEmpty(); }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction face) { return slot==0; }
    public void setCustomName(String name) { customName=name; sync(); }
    public net.minecraft.network.chat.Component name() { return customName.isEmpty() ? net.minecraft.network.chat.Component.translatable("container.pedestal") : net.minecraft.network.chat.Component.literal(customName); }
    public void sync() {
        setChanged();
        if (level!=null && !level.isClientSide) { level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2); level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock()); }
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); extensions.save(tag); ContainerHelper.saveAllItems(tag,items); if (!customName.isEmpty()) tag.putString("CustomName",customName); }
    @Override public void load(CompoundTag tag) { super.load(tag); items.clear(); ContainerHelper.loadAllItems(tag,items); customName=tag.getString("CustomName");  extensions.load(tag); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox() { return new net.minecraft.world.phys.AABB(worldPosition).expandTowards(0,1,0); }
    @Override public boolean triggerEvent(int id,int parameter) {
        if (id!=11 && id!=12) return super.triggerEvent(id,parameter);
        if (level!=null && level.isClientSide) tc4revival.client.ArcanePedestalRenderer.sparkles(level,worldPosition,id==12);
        return true;
    }
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability,Direction side) {
        if (capability==net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && !isRemoved()) return automation.cast();
        return super.getCapability(capability,side);
    }
    @Override public void onChunkUnloaded(){extensions.unload();super.onChunkUnloaded();}
    @Override public void invalidateCaps() { super.invalidateCaps(); extensions.invalidate(); automation.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); extensions.revive(); automation=net.minecraftforge.common.util.LazyOptional.of(()->new net.minecraftforge.items.wrapper.SidedInvWrapper(this,Direction.UP)); }
}
