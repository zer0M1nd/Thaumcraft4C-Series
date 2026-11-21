package tbmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.api.aspects.*;
import thaumcraft.api.wands.IWandable;

public final class Overchanter extends BlockEntity implements WorldlyContainer, MenuProvider, IWandable {
    public static final String HISTORY = "tbOverchants";
    private ItemStack item = ItemStack.EMPTY, working = ItemStack.EMPTY;
    private int seconds, clock, xp = 825;
    private boolean active;
    private LazyOptional<IItemHandlerModifiable> handler = LazyOptional.of(() -> new SidedInvWrapper(this, Direction.UP));
    public Overchanter(BlockPos pos, BlockState state) { super(Content.OVERCHANTER.get(), pos, state); }
    public static Enchantment nextEnchantment(ItemStack stack) {
        if (stack.isEmpty() || !stack.isEnchanted()) return null;
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(HISTORY) && !tag.contains(HISTORY, Tag.TAG_LIST)) return null;
        Set<String> done = new HashSet<>();
        if (tag != null) for (Tag entry : tag.getList(HISTORY, Tag.TAG_STRING)) done.add(entry.getAsString());
        return EnchantmentHelper.getEnchantments(stack).entrySet().stream()
            .filter(e -> e.getValue() > 0 && e.getValue() < 255 && !done.contains(ForgeRegistries.ENCHANTMENTS.getKey(e.getKey()).toString()))
            .map(Map.Entry::getKey).findFirst().orElse(null);
    }
    public boolean start() {
        if (level == null || level.isClientSide || active || nextEnchantment(item) == null) return false;
        active = true; seconds = clock = 0; xp = 825; working = item.copy(); changed();
        WorkEffects.sound(level,worldPosition,"craftstart",1,1);return true;
    }
    private void reset() { active = false; seconds = clock = 0; xp = 825; working = ItemStack.EMPTY; }
    public boolean active() { return active; }
    public int seconds() { return seconds; }
    public int experienceRemaining() { return xp; }
    public static void tick(Level level, BlockPos pos, BlockState state, Overchanter tile) {
        if (!tile.active) return;
        if (!ItemStack.matches(tile.item, tile.working) || nextEnchantment(tile.item) == null) { tile.reset(); tile.changed(); return; }
        if (tile.clock++ % 20 != 0) return;
        // Wait at the experience phase without paying more essentia. Old saves may
        // already exceed 32 seconds; they have paid the full cost and must not pay again.
        if (tile.seconds >= 15 && tile.xp > 0) {
            tile.absorbExperience();tile.changed();
            if (tile.xp > 0) return;
        }
        if (tile.seconds < 32) {
            if (!AspectSourceHelper.drainEssentia(tile, Aspect.MAGIC, null, 8)) return;
            tile.seconds++;
            WorkEffects.sound(level,pos,"infuserstart",.5F,1);
        }
        if (tile.seconds >= 32 && tile.xp == 0) {
            Enchantment enchantment = nextEnchantment(tile.item);
            if (enchantment != null) {
                var enchantments = EnchantmentHelper.getEnchantments(tile.item);
                enchantments.put(enchantment, enchantments.get(enchantment) + 1);
                EnchantmentHelper.setEnchantments(enchantments, tile.item);
                ListTag history = tile.item.getOrCreateTag().getList(HISTORY, Tag.TAG_STRING);
                history.add(StringTag.valueOf(ForgeRegistries.ENCHANTMENTS.getKey(enchantment).toString()));
                tile.item.getOrCreateTag().put(HISTORY, history);
                WorkEffects.sound(level,pos,"wand",1,1);
            }
            tile.reset();
        }
        tile.changed();
    }
    public static int legacyLevelsForExperience(int xp) {
        return (int)Math.round(xp > 255 ? (59 + Math.sqrt(24D * xp - 5159)) / 6 : xp / 17D);
    }
    private void absorbExperience() {
        // Shell order visits nearer blocks first without forcing unloaded chunks.
        for (int radius = 0; radius <= 8 && xp > 0; radius++)
            for (BlockPos offset : BlockPos.betweenClosed(-radius, -radius, -radius, radius, radius, radius)) {
                if (Math.max(Math.abs(offset.getX()), Math.max(Math.abs(offset.getY()), Math.abs(offset.getZ()))) != radius) continue;
                BlockPos target = worldPosition.offset(offset);
                if (level.hasChunkAt(target)) {
                    int drained=tbmodern.api.ExperienceSources.extract(level.getBlockEntity(target),xp);
                    xp-=drained;
                    if(drained>0)WorkEffects.lightning(level,target.getCenter(),worldPosition.getCenter(),0xffffff);
                }
                if (xp == 0) break;
            }
        if (xp <= 0) return;
        int levels = legacyLevelsForExperience(xp);
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(6, 3, 6))) {
            if (player.isSpectator() || player.experienceLevel < levels) continue;
            player.hurt(level.damageSources().magic(), 8);
            player.giveExperienceLevels(-levels); xp = 0;
            WorkEffects.lightning(level,player.getEyePosition(),worldPosition.getCenter(),0xffffff);
            WorkEffects.sound(level,player.blockPosition(),"zap",1,1);return;
        }
    }
    @Override public InteractionResult onWandUseFirst(ItemStack wand, UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        return start() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
    private void changed() { setChanged(); if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    @Override public int getContainerSize() { return 1; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean isEmpty() { return item.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? item : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int count) { if (slot != 0 || count <= 0) return ItemStack.EMPTY; ItemStack out = item.split(count); reset(); changed(); return out; }
    @Override public ItemStack removeItemNoUpdate(int slot) { if (slot != 0) return ItemStack.EMPTY; ItemStack out = item; item = ItemStack.EMPTY; reset(); return out; }
    @Override public void setItem(int slot, ItemStack stack) { if (slot != 0) return; item = stack; if (!item.isEmpty()) item.setCount(1); reset(); changed(); }
    @Override public void clearContent() { item = ItemStack.EMPTY; reset(); changed(); }
    @Override public boolean stillValid(Player player) { return level != null && !isRemoved() && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getCenter()) <= 64; }
    @Override public int[] getSlotsForFace(Direction face) { return new int[]{0}; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && nextEnchantment(stack) != null; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) { return !active && canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) { return !active && nextEnchantment(stack) == null; }
    @Override public Component getDisplayName() { return Component.translatable("block.thaumicbases.overchanter"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new Menu(id, inventory, this); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction face) { return !isRemoved() && capability == ForgeCapabilities.ITEM_HANDLER ? handler.cast() : super.getCapability(capability, face); }
    @Override public void invalidateCaps() { super.invalidateCaps(); handler.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); handler = LazyOptional.of(() -> new SidedInvWrapper(this, Direction.UP)); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.put("Item", item.save(new CompoundTag())); tag.put("Working", working.save(new CompoundTag()));
        tag.putInt("Seconds", seconds); tag.putInt("Clock", clock % 20); tag.putInt("XP", xp); tag.putBoolean("Active", active);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); item = ItemStack.of(tag.getCompound("Item")); working = ItemStack.of(tag.getCompound("Working"));
        seconds = Math.max(0, tag.getInt("Seconds")); clock = Math.floorMod(tag.getInt("Clock"), 20); xp = Math.max(0, Math.min(825, tag.getInt("XP"))); active = tag.getBoolean("Active");
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    public static final class Menu extends AbstractContainerMenu {
        private final Container container;
        public Menu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(1)); }
        public Menu(int id, Inventory inventory, Container container) {
            super(Content.OVERCHANTER_MENU.get(), id); this.container = container;
            addSlot(new Slot(container, 0, 80, 20) { @Override public boolean mayPlace(ItemStack stack) { return nextEnchantment(stack) != null; } });
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        @Override public boolean stillValid(Player player) { return container.stillValid(player); }
        @Override public ItemStack quickMoveStack(Player player, int index) {
            if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
            Slot slot = slots.get(index); ItemStack stack = slot.getItem(), copy = stack.copy();
            if (stack.isEmpty() || !(index == 0 ? moveItemStackTo(stack, 1, slots.size(), true) : moveItemStackTo(stack, 0, 1, false))) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
            slot.onTake(player, stack); return copy;
        }
    }
    public static final class Device extends BaseEntityBlock {
        public Device() { super(Properties.of().strength(5, 10).noOcclusion()); registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.POWERED, false)); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BlockStateProperties.POWERED); }
        @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Block.box(0, 0, 0, 16, 12, 16); }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Overchanter(pos, state); }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return level.isClientSide ? null : createTickerHelper(type, Content.OVERCHANTER.get(), Overchanter::tick); }
        @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos other, boolean moving) {
            if (level.isClientSide) return;
            boolean powered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.below());
            if (powered == state.getValue(BlockStateProperties.POWERED)) return;
            if (powered && level.getBlockEntity(pos) instanceof Overchanter tile) tile.start();
            level.setBlock(pos, state.setValue(BlockStateProperties.POWERED, powered), 2);
        }
        @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (player.isShiftKeyDown()) return InteractionResult.PASS;
            if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof Overchanter tile) net.minecraftforge.network.NetworkHooks.openScreen(server, tile);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
            if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof Overchanter tile) Containers.dropContents(level, pos, tile);
            super.onRemove(state, level, pos, replacement, moving);
        }
    }
}
