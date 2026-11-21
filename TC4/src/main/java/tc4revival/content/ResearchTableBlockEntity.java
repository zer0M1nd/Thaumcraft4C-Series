package tc4revival.content;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import tc4revival.aspect.TCAspects;
import tc4revival.config.TCConfig;
import tc4revival.network.ScanFeedbackPacket;
import tc4revival.network.TCNetwork;
import tc4revival.research.*;

/** One owner per physical pair. All research transactions execute on the server thread. */
public  class ResearchTableBlockEntity extends BlockEntity implements MenuProvider {
    private final Set<String> bonuses = new LinkedHashSet<>();
    private int nextRecalc;
    private boolean batching;
    public final ItemStackHandler items = new ItemStackHandler(2) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 ? ScribingToolsItem.accepts(stack) : stack.getItem() instanceof ResearchNotesItem;
        }
        @Override protected void onContentsChanged(int slot) { if (!batching) changed(); }
    };
    private final IItemHandler automation = new IItemHandler() {
        private boolean accessible() { return owner() && !isRemoved(); }
        @Override public int getSlots() { return 2; }
        @Override public ItemStack getStackInSlot(int slot) { return accessible() ? items.getStackInSlot(slot) : ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return accessible() && items.isItemValid(slot, stack) && (slot == 0 || !stack.is(TCItems.DISCOVERY.get()));
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? items.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return accessible() ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    };
    private LazyOptional<IItemHandler> inventoryCapability = LazyOptional.of(() -> automation);
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable net.minecraft.core.Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER && owner() && !isRemoved()) return inventoryCapability.cast();
        return super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); inventoryCapability.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); inventoryCapability = LazyOptional.of(() -> automation); }

    public ResearchTableBlockEntity(BlockPos pos, BlockState state) { super(TCTables.TABLE_ENTITY.get(), pos, state); }
    public Set<String> bonuses() { return Set.copyOf(bonuses); }
    public boolean owner() { return getBlockState().getValue(TableBlock.PART) == TableBlock.Part.MASTER; }
    public boolean valid(Player player) {
        return level != null && player.level() == level && !isRemoved() && owner()
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
    }
    public static void tick(Level level, BlockPos pos, BlockState state, ResearchTableBlockEntity table) {
        if (!table.owner()) return;
        table.nextRecalc = ResearchTableBonuses.nextCounter(table.nextRecalc);
        if (table.nextRecalc != 0) return;
        var next = ResearchTableBonuses.recalculate(table.bonuses, new ResearchTableEnvironment(level, pos),
                TCAspects.catalog().all().stream().map(aspect -> aspect.tag()).toList(), level.random::nextInt);
        table.bonuses.clear(); table.bonuses.addAll(next); table.changed();
    }
    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    public void dropContents() {
        if (level == null || level.isClientSide) return;
        batching = true;
        try {
            for (int slot = 0; slot < 2; slot++) {
                var stack = items.getStackInSlot(slot);
                items.setStackInSlot(slot, ItemStack.EMPTY);
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            }
            bonuses.clear(); nextRecalc = 0;
        } finally { batching = false; }
        changed();
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        var inventory = new ListTag();
        for (int slot = 0; slot < 2; slot++) if (!items.getStackInSlot(slot).isEmpty()) {
            var stack = items.getStackInSlot(slot);
            var row = stack.save(new CompoundTag()); row.putByte("Slot", (byte) slot);
            // Preserve oversized stacks already present in older saves while new copies obey the stack limit.
            row.putInt("FullCount", stack.getCount());
            inventory.add(row);
        }
        tag.put("Inventory", inventory); tag.putInt("nextRecalc", nextRecalc);
        var list = new ListTag();
        for (String bonus : bonuses) { var row = new CompoundTag(); row.putString("tag", bonus); list.add(row); }
        tag.put("bonusAspects", list);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        batching = true;
        try {
            items.setStackInSlot(0, ItemStack.EMPTY); items.setStackInSlot(1, ItemStack.EMPTY);
            var inventory = tag.getList("Inventory", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(2, inventory.size()); i++) {
                var row = inventory.getCompound(i); int slot = Byte.toUnsignedInt(row.getByte("Slot"));
                if (slot >= 2) continue;
                var normalized = row.copy();
                if (row.contains("FullCount", Tag.TAG_INT)) normalized.putByte("Count", (byte) 1);
                var stack = ItemStack.of(normalized);
                if (row.contains("FullCount", Tag.TAG_INT)) stack.setCount(Math.max(0, row.getInt("FullCount")));
                items.setStackInSlot(slot, stack);
            }
            nextRecalc = Math.max(0, Math.min(601, tag.getInt("nextRecalc")));
            bonuses.clear(); var list = tag.getList("bonusAspects", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                String aspect = list.getCompound(i).getString("tag");
                if (TCAspects.catalog().find(aspect).isPresent()) bonuses.add(aspect);
            }
        } finally { batching = false; }
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 2, 2)); }
    @Override public Component getDisplayName() { return Component.translatable("container.thaumcraft.research_table"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ResearchTableMenu(id, inventory, this); }

    public enum Action { PLACE, ERASE, COMBINE, DUPLICATE, SYNTHESIZE }
    public boolean act(ServerPlayer player, Action action, int q, int r, @Nullable String first, @Nullable String second) {
        if (!valid(player) || !(player.containerMenu instanceof ResearchTableMenu menu) || menu.table() != this) return false;
        var storage = KnowledgeSavedData.get(player.getServer());
        long revision = storage.revision(player.getUUID());
        var knowledge = storage.snapshot(player.getUUID());
        if (knowledge.isEmpty()) return false;
        var noteStack = items.getStackInSlot(1);
        var read = noteStack.getItem() instanceof ResearchNotesItem
                ? ResearchNoteNbt.read(noteStack.getTag(), TCAspects.catalog()) : new ResearchNoteNbt.ReadResult(Optional.empty(), java.util.List.of());
        var state = new ResearchTableActions.State(read.note(), read.note().isPresent() ? noteStack.getCount() : 0,
                ScribingToolsItem.ink(items.getStackInSlot(0)), bonuses);
        var actions = new ResearchTableActions(TCAspects.catalog(), ResearchCatalogs.catalog(), ResearchCatalogs::enabled);
        var result = switch (action) {
            case PLACE, ERASE -> actions.place(state, knowledge.orElseThrow(), new ResearchHex(q, r),
                    action == Action.ERASE ? null : first, level.random::nextFloat);
            case SYNTHESIZE -> actions.synthesize(state, knowledge.orElseThrow(), first, TCConfig.SERVER.aspectPoolSoftCap.get());
            case COMBINE -> actions.combine(state, knowledge.orElseThrow(), first, second, TCConfig.SERVER.aspectPoolSoftCap.get());
            case DUPLICATE -> actions.duplicate(state, knowledge.orElseThrow(), count(player, Items.PAPER), count(player, Items.INK_SAC));
        };
        if (!result.accepted()) return false;
        var change = result.change().orElseThrow();
        // Prepare every stack before committing knowledge. No user callbacks or RNG occur after the commit.
        var nextNote = noteStack.copy();
        if (change.table().note().isPresent()) {
            nextNote = ResearchNotesItem.create(change.table().note().orElseThrow());
            if (noteStack.hasTag()) nextNote.setTag(noteStack.getTag().copy());
            ResearchNoteNbt.write(nextNote.getOrCreateTag(), change.table().note().orElseThrow());
            nextNote.setCount(change.table().noteCount());
        }
        var tools = items.getStackInSlot(0).copy();
        if (tools.getItem() instanceof thaumcraft.api.IScribeTools scribe) scribe.setInk(tools, change.table().ink());
        if (!storage.replace(player.getUUID(), revision, change.knowledge())) return false;
        batching = true;
        try {
            items.setStackInSlot(0, tools); items.setStackInSlot(1, nextNote);
            bonuses.clear(); bonuses.addAll(change.table().bonuses());
            consume(player, Items.PAPER, change.paper()); consume(player, Items.INK_SAC, change.inkSac());
        } finally { batching = false; }
        changed(); player.getInventory().setChanged(); menu.broadcastChanges();
        change.orbPitch().ifPresent(pitch -> level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.2f, pitch));
        if (change.completedNow() || action == Action.DUPLICATE)
            level.playSound(null, worldPosition, TCSounds.LEARN.get(), SoundSource.BLOCKS, 1, 1);
        change.reward().ifPresent(reward -> TCNetwork.send(player, new ScanFeedbackPacket(ScanRules.Status.SUCCESS, "",
                java.util.List.of(new ScanFeedbackPacket.Reward(reward.aspect().tag(), reward.gained(), reward.discovery())))));
        return true;
    }
    private static int count(Player player, Item item) {
        int total = 0;
        for (var stack : player.getInventory().items) if (stack.is(item)) total += stack.getCount();
        return total;
    }
    private static void consume(Player player, Item item, int count) {
        for (var stack : player.getInventory().items) if (count > 0 && stack.is(item)) {
            int taken = Math.min(count, stack.getCount()); stack.shrink(taken); count -= taken;
        }
    }
}
