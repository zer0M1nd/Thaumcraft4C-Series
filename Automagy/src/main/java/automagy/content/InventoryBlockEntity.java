package automagy.content;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;

/** Shared inventory lifecycle; subclasses decide which items automation may insert. */
public abstract class InventoryBlockEntity extends BlockEntity {
    protected final ItemStackHandler inventory;
    private final int slots;
    private LazyOptional<IItemHandler> itemCapability;
    protected InventoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type,pos,state); this.slots=slots;
        inventory=new ItemStackHandler(slots) {
            @Override public int getSlotLimit(int slot) { return InventoryBlockEntity.this.slotLimit(slot); }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return accepts(slot,stack); }
            @Override protected void onContentsChanged(int slot) { inventoryChanged(); }
        };
        itemCapability=LazyOptional.of(()->inventory);
    }
    protected int slotLimit(int slot) { return 64; }
    protected boolean accepts(int slot, ItemStack stack) { return true; }
    protected void inventoryChanged() { sync(); }
    public ItemStack stored(int slot) { return inventory.getStackInSlot(slot); }
    public void dropContents() {
        if (level==null || level.isClientSide) return;
        for (int i=0;i<slots;i++) {
            ItemStack stack=inventory.getStackInSlot(i); inventory.setStackInSlot(i,ItemStack.EMPTY);
            if (!stack.isEmpty()) net.minecraft.world.Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,stack);
        }
    }
    public void sync() {
        setChanged();
        if (level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap==ForgeCapabilities.ITEM_HANDLER && !isRemoved() ? itemCapability.cast() : super.getCapability(cap,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps();itemCapability.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps();itemCapability=LazyOptional.of(()->inventory); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag);tag.put("Inventory",inventory.serializeNBT()); }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        CompoundTag data=tag.getCompound("Inventory").copy(); data.putInt("Size",slots);
        ListTag items=new ListTag();
        for (Tag raw:data.getList("Items",Tag.TAG_COMPOUND)) {
            CompoundTag item=(CompoundTag)raw; int slot=item.getInt("Slot");
            if (slot>=0 && slot<slots) items.add(item);
        }
        data.put("Items",items);inventory.deserializeNBT(data);
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
