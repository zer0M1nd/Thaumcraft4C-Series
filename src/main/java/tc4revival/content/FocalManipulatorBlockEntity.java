package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectNbt;
import tc4revival.aspect.TCAspects;

public  class FocalManipulatorBlockEntity extends BlockEntity implements Container, MenuProvider {
    private ItemStack focus = ItemStack.EMPTY, paymentFocus = ItemStack.EMPTY;
    private java.util.List<Integer> queue = java.util.List.of();
    public java.util.List<Integer> queue() { return queue; }
    private AspectAmounts remaining = new AspectAmounts();
    public static final int VIS_CAPACITY = 20000;
    private AspectAmounts stored = new AspectAmounts();
    public AspectAmounts stored() { return stored.copy(); }
    private int size, upgrade = -1, rank = -1, ticks;
    public FocalManipulatorBlockEntity(BlockPos pos, BlockState state) { super(TCArcane.FOCAL_ENTITY.get(), pos, state); }
    public int size() { return size; }
    public int rank() { return rank; }
    public int upgrade() { return upgrade; }
    public AspectAmounts remaining() { return remaining.copy(); }
    public static AspectAmounts cost(FocusUpgrade upgrade, int rank) {
        if (rank < 1 || rank > 5) return new AspectAmounts();
        var inputs = new AspectAmounts();
        for (var name : upgrade.aspects) inputs.add(TCAspects.catalog().require(name), 200 << (rank - 1));
        return TCAspects.catalog().toPrimals(inputs, tc4revival.aspect.AspectCatalog.Reduction.SUM);
    }
    public boolean start(Player player, int id) { return startBatch(player, focus.copy(), java.util.List.of(id)); }
    public boolean startBatch(Player player, ItemStack expected, java.util.List<Integer> choices) {
        if (level == null || level.isClientSide || size > 0 || !stillValid(player) || !ItemStack.matches(focus, expected)
                || choices.size() > 1 && !tc4revival.config.TCConfig.SERVER.revival.batchFocusUpgrades.get()) return false;
        var plan = FocalUpgradePlan.create(focus, choices, player);
        if (plan == null || !player.getAbilities().instabuild
                && FocalUpgradePlan.availablePoints(player) < FocalUpgradePlan.experiencePoints(plan.experience())) return false;
        queue = java.util.List.copyOf(choices); paymentFocus = focus.copy();
        remaining = plan.cost(); size = Math.toIntExact(remaining.total());
        rank = ((WandFocusItem) focus.getItem()).nextRank(focus); upgrade = queue.get(0);
        if (!player.getAbilities().instabuild) player.giveExperiencePoints(-FocalUpgradePlan.experiencePoints(plan.experience()));
        if (size == 0) { focus = plan.result(); cancel(); }
        setChanged(); sound(TCSounds.CRAFT_START.get(), .25f); return true;
    }
    public void tick() {
        if (level == null) return;
        if (level.isClientSide) {
            if (size > 0) tc4revival.client.ClientFocusEffects.manipulator(level, worldPosition);
            return;
        }
        if (rank < 0) rank = 0;
        if (++ticks % 5 != 0) return;
        boolean charged = false;
        for (var aspect : TCAspects.catalog().primals()) {
            int needed = Math.min(100, VIS_CAPACITY - stored.amount(aspect));
            if (needed <= 0) continue;
            int drained = VisNetwork.drain(level, worldPosition, aspect, needed);
            if (drained > 0) { stored.add(aspect, drained); charged = true; }
        }
        if (charged) setChanged();
        if (size == 0) return;
        if (!ItemStack.matches(focus, paymentFocus) || FocalUpgradePlan.create(focus, queue, null) == null) {
            cancel(); sound(TCSounds.CRAFT_FAIL.get(), .33f); setChanged(); return;
        }
        for (var aspect : remaining.byAmount()) {
            int drained = Math.min(stored.amount(aspect), remaining.amount(aspect));
            if (drained > 0) { stored.reduce(aspect, drained); remaining.reduce(aspect, drained); setChanged(); }
        }
        if (remaining.total() == 0) {
            focus = FocalUpgradePlan.create(focus, queue, null).result();
            cancel(); sound(TCSounds.WAND.get(), 1); setChanged();
        }
    }
    private void cancel() { queue = java.util.List.of(); paymentFocus = ItemStack.EMPTY; size = 0; rank = -1; upgrade = -1; remaining = new AspectAmounts(); }
    public void sound(SoundEvent event, float volume) {
        if (level != null && !level.isClientSide) level.playSound(null, worldPosition, event, SoundSource.MASTER, volume, 1);
    }
    @Override public int getContainerSize() { return 1; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean isEmpty() { return focus.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? focus : ItemStack.EMPTY; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && stack.getItem() instanceof WandFocusItem; }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot != 0) return;
        // Bind an in-flight payment to its original item. Replacing it must not grant another item a free upgrade.
        if (size > 0) { cancel(); sound(TCSounds.CRAFT_FAIL.get(), .33f); }
        focus = stack; if (focus.getCount() > 1) focus.setCount(1); setChanged();
    }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || amount <= 0) return ItemStack.EMPTY;
        var taken = focus.split(amount);
        if (!taken.isEmpty()) { if (size > 0) { cancel(); sound(TCSounds.CRAFT_FAIL.get(), .33f); } setChanged(); }
        return taken;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) return ItemStack.EMPTY;
        var taken = focus; focus = ItemStack.EMPTY; cancel(); return taken;
    }
    @Override public void clearContent() { focus = ItemStack.EMPTY; cancel(); setChanged(); }
    @Override public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64;
    }
    @Override public void setChanged() {
        super.setChanged(); if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.put("Focus", focus.save(new CompoundTag()));
        var storage = new CompoundTag(); AspectNbt.write(storage, stored); tag.put("StoredVis", storage);
        tag.put("PaymentFocus", paymentFocus.save(new CompoundTag()));
        var choices = new net.minecraft.nbt.ListTag();
        for (int id : queue) choices.add(net.minecraft.nbt.StringTag.valueOf(FocusUpgrade.byId(id).orElseThrow().key));
        tag.put("UpgradeQueue", choices);
        AspectNbt.write(tag, remaining); tag.putInt("size", size); tag.putInt("rank", rank); tag.putInt("upgrade", upgrade); FocusUpgrade.byId(upgrade).ifPresent(value->tag.putString("upgradeKey",value.key));
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); focus = ItemStack.of(tag.getCompound("Focus"));
        stored = new AspectAmounts();
        AspectNbt.read(tag.getCompound("StoredVis"), TCAspects.catalog()).amounts().view().forEach((aspect, amount) -> {
            if (aspect.isPrimal() && amount > 0) stored.add(aspect, Math.min(VIS_CAPACITY, amount));
        });
        var decoded = AspectNbt.read(tag, TCAspects.catalog());
        remaining = decoded.amounts(); size = tag.getInt("size"); rank = tag.getInt("rank"); upgrade = tag.contains("upgradeKey")?java.util.Arrays.stream(FocusUpgrade.values()).filter(value->value.key.equals(tag.getString("upgradeKey"))).mapToInt(value->value.id).findFirst().orElse(-1):tag.getInt("upgrade");
        if (!decoded.problems().isEmpty() || size < 0 || remaining.view().entrySet().stream().anyMatch(entry -> !entry.getKey().isPrimal() || entry.getValue() < 0)
                || remaining.total() > size || size > 0 && (!(focus.getItem() instanceof WandFocusItem item) || rank < 1 || rank > 5
                    || item.nextRank(focus) != rank || FocusUpgrade.byId(upgrade).isEmpty())) { cancel(); return; }
        queue = java.util.List.of(); paymentFocus = ItemStack.EMPTY;
        if (size > 0) {
            if (tag.contains("UpgradeQueue")) {
                var choices = tag.getList("UpgradeQueue", net.minecraft.nbt.Tag.TAG_STRING);
                if (choices.isEmpty() || choices.size() > 5) { cancel(); return; }
                var ids = new java.util.ArrayList<Integer>();
                for (int i = 0; i < choices.size(); i++) {
                    String key = choices.getString(i);
                    var option = java.util.Arrays.stream(FocusUpgrade.values()).filter(value -> value.key.equals(key)).findFirst();
                    if (option.isEmpty()) { cancel(); return; } ids.add(option.get().id);
                }
                queue = java.util.List.copyOf(ids);
                paymentFocus = ItemStack.of(tag.getCompound("PaymentFocus"));
            } else { queue = java.util.List.of(upgrade); paymentFocus = focus.copy(); }
            var plan = FocalUpgradePlan.create(focus, queue, null);
            if (plan == null || queue.get(0) != upgrade || !ItemStack.matches(focus, paymentFocus)) { cancel(); return; }
            var expected = plan.cost();
            if (size != expected.total() || remaining.view().entrySet().stream().anyMatch(entry -> entry.getValue() > expected.amount(entry.getKey()))) cancel();
        }
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public Component getDisplayName() { return Component.translatable("block.thaumcraft.focal_manipulator"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new FocalManipulatorMenu(id, inventory, this); }
}
