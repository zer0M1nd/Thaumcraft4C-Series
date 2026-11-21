package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persistent ingredients and wand; each viewer owns their derived output separately. */
public  class ArcaneWorkbenchBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    private final NonNullList<ItemStack> items = NonNullList.withSize(11, ItemStack.EMPTY);
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> automation =
            net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.wrapper.SidedInvWrapper(this, Direction.UP));
    public ArcaneWorkbenchBlockEntity(BlockPos pos, BlockState state) { super(TCArcane.ENTITY.get(), pos, state); }
    @Override public int getContainerSize() { return 11; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return slot == 9 ? ItemStack.EMPTY : items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (slot == 9) return ItemStack.EMPTY;
        var result = ContainerHelper.removeItem(items, slot, amount); if (!result.isEmpty()) setChanged(); return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot == 9) return;
        items.set(slot, stack); if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize()); setChanged();
    }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64;
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot < 9 || slot == 10 && validWand(stack); }
    public static boolean validWand(ItemStack stack) { return stack.getItem() instanceof CastingWandItem casting && !casting.isStaff(stack); }
    @Override public int[] getSlotsForFace(Direction direction) { return new int[]{10}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) { return slot == 10 && validWand(stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) { return slot == 10; }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); ContainerHelper.saveAllItems(tag, items); }
    @Override public void load(CompoundTag tag) { super.load(tag); items.clear(); ContainerHelper.loadAllItems(tag, items); items.set(9, ItemStack.EMPTY); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }
    @Override public Component getDisplayName() { return Component.translatable("container.arcaneworkbench"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ArcaneWorkbenchMenu(id, inventory, this); }
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, Direction direction) {
        if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && !isRemoved()) return automation.cast();
        return super.getCapability(capability, direction);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); automation.invalidate(); }
    @Override public void reviveCaps() {
        super.reviveCaps(); automation = net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.wrapper.SidedInvWrapper(this, Direction.UP));
    }
}
