package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.TCAspects;

public  class WandPedestalBlockEntity extends BlockEntity implements WorldlyContainer, RevealedAspectContainer {
    @Override public AspectAmounts revealedAspects() {
        var result = new AspectAmounts(); var item = getItem(0); if (!chargeable(item)) return result;
        TCAspects.catalog().primals().stream().sorted(java.util.Comparator.comparing(Aspect::tag))
                .forEach(aspect -> result.add(aspect, WandVis.stored(item.getTag(), aspect) / 100));
        return result;
    }
    @Override public float popupHeight() { return .6f; }
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private List<BlockPos> nodes;
    private int ticks, drainColor;
    private BlockPos drainPosition;
    private String customName = "";
    private boolean notifyCharge;
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> automation =
            net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.wrapper.SidedInvWrapper(this, Direction.UP));
    public WandPedestalBlockEntity(BlockPos pos, BlockState state) { super(TCArcane.WAND_PEDESTAL_ENTITY.get(), pos, state); }
    public BlockPos drainPosition() { return drainPosition; }
    public int drainColor() { return drainColor; }
    public void setCustomName(String name) { customName = name; setChanged(); }
    public static boolean chargeable(ItemStack stack) { return stack.getItem() instanceof CastingWandItem || stack.getItem() instanceof VisAmuletItem; }
    private int capacity(ItemStack stack) { return stack.getItem() instanceof CastingWandItem wand ? wand.capacity(stack) : ((VisAmuletItem) stack.getItem()).capacity(stack); }
    private int stored(ItemStack stack, Aspect aspect) { return WandVis.stored(stack.getTag(), aspect); }
    private void charge(ItemStack stack, Aspect aspect) {
        if (stack.getItem() instanceof CastingWandItem wand) wand.receiveVis(stack, aspect, 100, false);
        else ((VisAmuletItem) stack.getItem()).addVis(stack, aspect, 1, true, true);
    }
    public void tick() {
        if (level == null || level.isClientSide) return;
        if (nodes == null) findNodes(); ticks++;
        if (ticks % 20 == 0 && notifyCharge && !nodes.isEmpty() && !isEmpty()) {
            notifyCharge = false; level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        boolean rescan = false;
        if (ticks % 5 == 0 && !nodes.isEmpty() && chargeable(getItem(0))) {
            ItemStack stack = getItem(0); int minimum = stack.getItem() instanceof CastingWandItem wand
                    && (wand.cap(stack) == WandParts.Cap.IRON || wand.rod(stack) == WandParts.Rod.WOOD) ? 0 : 1;
            var needed = TCAspects.catalog().primals().stream().filter(aspect -> stored(stack, aspect) < capacity(stack)).toList();
            BlockPos previous = drainPosition; drainPosition = null;
            boolean compound = level.hasChunkAt(worldPosition.above()) && level.getBlockState(worldPosition.above()).is(TCArcane.WAND_PEDESTAL_FOCUS.get());
            if (!needed.isEmpty()) {
                for (var pos : nodes) {
                    if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node)) continue;
                    var available = node.aspects();
                    for (var aspect : needed) if (available.amount(aspect) > minimum) { transfer(node, aspect, aspect, stack); break; }
                    if (drainPosition != null) break;
                    if (compound) for (var aspect : available.aspects()) {
                        if (aspect.isPrimal() || available.amount(aspect) <= minimum) continue;
                        var primals = TCAspects.catalog().toPrimals(new AspectAmounts().add(aspect, 1), AspectCatalog.Reduction.SUM);
                        for (var primal : needed) if (primals.amount(primal) > 0) { transfer(node, aspect, primal, stack); break; }
                        if (drainPosition != null) break;
                    }
                    if (drainPosition != null) break;
                }
                rescan = drainPosition == null;
            }
            if (previous != null && drainPosition == null) sync();
        }
        if (ticks % 100 == 0 && (rescan || nodes.isEmpty())) findNodes();
    }
    private void transfer(AuraNodeBlockEntity node, Aspect spent, Aspect received, ItemStack stack) {
        if (stack.getItem() instanceof CastingWandItem wand && wand.receiveVis(stack, received,
                Math.min(100, Math.max(0, wand.capacity(stack) - wand.vis(stack, received))), true) <= 0) return;
        if (!node.take(spent, 1)) return;
        charge(stack, received); node.sync(); drainPosition = node.getBlockPos(); drainColor = spent.color();
        if (stack.getItem() instanceof CastingWandItem) notifyCharge = true;
        sync();
    }
    private void findNodes() {
        nodes = new ArrayList<>();
        // Preserve the old X/Y/Z traversal order, cube range and lack of a line-of-sight condition.
        for (int x = -8; x <= 8; x++) for (int y = -8; y <= 8; y++) for (int z = -8; z <= 8; z++) {
            var pos = worldPosition.offset(x, y, z);
            if (!level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof AuraNodeBlockEntity) nodes.add(pos);
        }
    }
    public int comparatorOutput() {
        var stack = getItem(0); if (!(stack.getItem() instanceof CastingWandItem wand)) return 0;
        long total = 0; for (var aspect : TCAspects.catalog().primals()) total += wand.vis(stack, aspect);
        return Math.min(15, (int) Math.floor(total / (wand.capacity(stack) * 6f) * 14) + 1);
    }
    @Override public int getContainerSize() { return 1; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean isEmpty() { return items.get(0).isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? items.get(0) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) { if (slot != 0) return ItemStack.EMPTY; var result = ContainerHelper.removeItem(items, slot, amount); if (!result.isEmpty()) sync(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return slot == 0 ? ContainerHelper.takeItem(items, slot) : ItemStack.EMPTY; }
    @Override public void setItem(int slot, ItemStack stack) { if (slot != 0) return; items.set(0, stack); if (stack.getCount() > 1) stack.setCount(1); drainPosition = null; sync(); }
    @Override public void clearContent() { items.clear(); drainPosition = null; sync(); }
    @Override public boolean stillValid(Player player) { return level != null && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && chargeable(stack); }
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 0 && isEmpty() && chargeable(stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 0; }
    private void sync() { setChanged(); if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); ContainerHelper.saveAllItems(tag, items); if (!customName.isEmpty()) tag.putString("CustomName", customName); }
    @Override public void load(CompoundTag tag) {
        super.load(tag); items.clear(); ContainerHelper.loadAllItems(tag, items); customName = tag.getString("CustomName");
        drainPosition = tag.contains("DrainPosition", net.minecraft.nbt.Tag.TAG_LONG) ? BlockPos.of(tag.getLong("DrainPosition")) : null; drainColor = tag.getInt("DrainColor");
        if (drainPosition != null && (Math.abs(drainPosition.getX() - (long) worldPosition.getX()) > 8 || Math.abs(drainPosition.getY() - (long) worldPosition.getY()) > 8 || Math.abs(drainPosition.getZ() - (long) worldPosition.getZ()) > 8)) drainPosition = null;
    }
    @Override public CompoundTag getUpdateTag() { var tag = saveWithoutMetadata(); if (drainPosition != null) tag.putLong("DrainPosition", drainPosition.asLong()); tag.putInt("DrainColor", drainColor); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(2); }
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, Direction side) {
        if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && !isRemoved()) return automation.cast(); return super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); automation.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); automation = net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.wrapper.SidedInvWrapper(this, Direction.UP)); }
}
