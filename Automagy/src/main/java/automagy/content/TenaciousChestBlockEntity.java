package automagy.content;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;

/** The highest numbered matching slot is reserved for manual access. */
public final class TenaciousChestBlockEntity extends InventoryBlockEntity implements WorldlyContainer, MenuProvider {
    private boolean sorting, needsSort = true;
    private int countdown;
    private final java.util.Set<java.util.UUID> viewers = new java.util.HashSet<>();
    private boolean open;
    public float lid, previousLid;
    private LazyOptional<IItemHandler> automation = LazyOptional.of(this::restrictedInventory);
    public TenaciousChestBlockEntity(BlockPos pos, BlockState state) { super(Content.TENACIOUS_TILE.get(), pos, state, 27); }
    private IItemHandler restrictedInventory() {
        return new IItemHandler() {
            public int getSlots() { return 27; }
            public ItemStack getStackInSlot(int slot) { schedule(200); return inventory.getStackInSlot(slot); }
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return inventory.insertItem(slot, stack, simulate); }
            public ItemStack extractItem(int slot, int count, boolean simulate) {
                return reserved(slot) ? ItemStack.EMPTY : inventory.extractItem(slot, count, simulate);
            }
            public int getSlotLimit(int slot) { return 64; }
            public boolean isItemValid(int slot, ItemStack stack) { return true; }
        };
    }
    public boolean reserved(int slot) {
        ItemStack stack = stored(slot); if (stack.isEmpty()) return false;
        for (int i = 26; i > slot; i--) if (ItemStack.isSameItemSameTags(stack, stored(i))) return false;
        return true;
    }
    private void schedule(int delay) { countdown = needsSort ? Math.min(countdown, delay) : delay; needsSort = true; }
    @Override public void setChanged() { super.setChanged(); if (!sorting) schedule(0); }
    @Override protected void inventoryChanged() {
        super.inventoryChanged();
        if (level != null && !level.isClientSide) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }
    public static void tick(Level level, BlockPos pos, BlockState state, TenaciousChestBlockEntity tile) {
        if (level.isClientSide) {
            tile.previousLid = tile.lid; tile.lid = Math.max(0, Math.min(1, tile.lid + (tile.open ? .1f : -.1f))); return;
        }
        if (level.getGameTime() % 20 == 0) {
            tile.viewers.removeIf(id -> {
                Player player = level.getPlayerByUUID(id);
                return player == null || !(player.containerMenu instanceof ChestMenu menu) || menu.getContainer() != tile;
            }); tile.updateOpen();
        }
        if (!tile.needsSort) return;
        if (tile.countdown-- > 0) return;
        tile.needsSort = false; tile.sorting = true;
        try { tile.sortOne(); } finally { tile.sorting = false; }
    }
    private void sortOne() {
        int first = -1, last = -1;
        for (int i = 0; i < 27; i++) if (stored(i).isEmpty()) { if (first < 0) first = i; last = i; }
        if (first >= 0) for (int i = 26; i >= 0; i--) {
            ItemStack stack = stored(i); if (stack.getCount() < 2) continue;
            int copies = 0;
            for (int j = 0; j < 27; j++) if (ItemStack.isSameItemSameTags(stack, stored(j))) copies++;
            if (copies != 1) continue;
            int destination = last > i ? last : first;
            int amount = destination > i ? 1 : stack.getCount() - 1;
            inventory.setStackInSlot(destination, inventory.extractItem(i, amount, false)); schedule(0); return;
        }
        for (int i = 26; i >= 1; i--) {
            ItemStack stack = stored(i); if (stack.getCount() <= 1 || !reserved(i)) continue;
            ItemStack remaining = stack.copyWithCount(stack.getCount() - 1);
            int start = remaining.getCount();
            for (int j = 0; j < 27 && !remaining.isEmpty(); j++) if (j != i) remaining = inventory.insertItem(j, remaining, false);
            int moved = start - remaining.getCount();
            if (moved > 0) { inventory.extractItem(i, moved, false); schedule(0); return; }
        }
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == ForgeCapabilities.ITEM_HANDLER && !isRemoved() ? automation.cast() : super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); automation.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); automation = LazyOptional.of(this::restrictedInventory); }
    @Override public int getContainerSize() { return 27; }
    @Override public int[] getSlotsForFace(Direction face) { return java.util.stream.IntStream.range(0,27).toArray(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) { return true; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) { return !reserved(slot); }
    @Override public boolean isEmpty() { for (int i=0;i<27;i++) if (!stored(i).isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int slot) { schedule(200); return stored(slot); }
    @Override public ItemStack removeItem(int slot, int count) { return inventory.extractItem(slot, count, false); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return inventory.extractItem(slot, Integer.MAX_VALUE, false); }
    @Override public void setItem(int slot, ItemStack stack) { inventory.setStackInSlot(slot, stack.copyWithCount(Math.min(64, stack.getCount()))); }
    @Override public void clearContent() { for (int i=0;i<27;i++) inventory.setStackInSlot(i, ItemStack.EMPTY); }
    @Override public boolean stillValid(Player player) { return !isRemoved() && level == player.level() && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getCenter()) <= 64; }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public void startOpen(Player player) { if (!player.isSpectator() && !player.level().isClientSide) { viewers.add(player.getUUID()); updateOpen(); } }
    @Override public void stopOpen(Player player) { if (!player.level().isClientSide) { viewers.remove(player.getUUID()); updateOpen(); schedule(0); } }
    private void updateOpen() {
        boolean next = !viewers.isEmpty(); if (next == open) return; open = next; sync();
        level.playSound(null, worldPosition, open ? net.minecraft.sounds.SoundEvents.CHEST_OPEN : net.minecraft.sounds.SoundEvents.CHEST_CLOSE,
                net.minecraft.sounds.SoundSource.BLOCKS, .5f, .9f + level.random.nextFloat() * .1f);
    }
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag() {
        var tag = super.getUpdateTag(); tag.putBoolean("Open", open); return tag;
    }
    @Override public void load(net.minecraft.nbt.CompoundTag tag) { super.load(tag); open = tag.getBoolean("Open"); needsSort = true; countdown = 0; }
    @Override public AbstractContainerMenu createMenu(int id, Inventory player, Player owner) { return ChestMenu.threeRows(id, player, this); }
}
