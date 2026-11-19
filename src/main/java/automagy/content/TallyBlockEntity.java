package automagy.content;

import automagy.api.RemoteComparatorHost;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

public final class TallyBlockEntity extends InventoryBlockEntity implements MenuProvider, RemoteComparatorHost, automagy.api.GreedyExtractionPermission {
    public boolean requireAll = true;
    private boolean validTarget;
    public TallyBlockEntity(BlockPos pos, BlockState state) { super(Content.TALLY_TILE.get(), pos, state, 1); }
    @Override protected int slotLimit(int slot) { return 1; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return stack.getItem() instanceof FilterItem && ItemFilter.load(stack).populated(); }
    public IItemHandler filterInventory() { return inventory; }
    @Override public boolean allowGreedyExtraction(BlockPos collector, Direction side) { return false; }
    public boolean validTarget() { return validTarget; }
    @Override public boolean hasComparatorSignal() { return getBlockState().getValue(TallyBlock.LIT); }
    public static void tick(Level level, BlockPos pos, BlockState state, TallyBlockEntity tile) {
        if (level.getGameTime() % 11 == Math.floorMod(pos.asLong(), 11)) tile.refresh();
    }
    private Optional<BlockPos> directTarget() {
        if (level == null) return Optional.empty();
        if (level.hasChunkAt(worldPosition.above()) && level.getBlockEntity(worldPosition.above()) instanceof RemoteComparatorBlockEntity remote) {
            if (getBlockState().getValue(TallyBlock.FACING) != Direction.DOWN)
                level.setBlock(worldPosition, getBlockState().setValue(TallyBlock.FACING, Direction.DOWN), 3);
            return remote.target().map(this::inventoryTarget);
        }
        BlockPos target = worldPosition.relative(getBlockState().getValue(TallyBlock.FACING).getOpposite());
        return level.hasChunkAt(target) ? Optional.of(inventoryTarget(target)) : Optional.empty();
    }
    private BlockPos inventoryTarget(BlockPos target){
        String kind=((TallyBlock)getBlockState().getBlock()).kind;
        if(kind.equals("world")||kind.equals("drops")||!level.hasChunkAt(target))return target;
        var state=level.getBlockState(target);
        if(!(state.getBlock() instanceof automagy.api.InventariumShelf)&&!state.is(InventariumEnvironment.SHELVES))return target;
        // Legacy lookup order: x first, then z, at one block above the queried shelf.
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            var candidate=target.offset(x,1,z);
            if(level.hasChunkAt(candidate)&&level.getBlockEntity(candidate) instanceof InventariumBlockEntity)return candidate;
        }
        return target;
    }
    private Optional<BlockPos> ultimateTarget() {
        Set<BlockPos> visited = new HashSet<>(); TallyBlockEntity current = this;
        for (int i = 0; i < 64; i++) {
            if (!visited.add(current.worldPosition)) return Optional.empty();
            Optional<BlockPos> target = current.directTarget(); if (target.isEmpty()) return target;
            if (!(level.getBlockEntity(target.get()) instanceof TallyBlockEntity tally)) return target;
            current = tally;
        }
        return Optional.empty();
    }
    private List<ItemStack> detected(BlockPos target) {
        String kind = ((TallyBlock)getBlockState().getBlock()).kind;
        List<ItemStack> items = new ArrayList<>(); validTarget = true;
        if (kind.equals("world")) {
            // Legacy world lens validates the chain but samples its direct target, unlike the other lenses.
            target = directTarget().orElse(target);
            for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(target).inflate(.1))) if (entity.isAlive()) items.add(entity.getItem().copy());
        } else if (kind.equals("drops")) {
            BlockState state = level.getBlockState(target); validTarget = !state.isAir();
            if (validTarget && level instanceof ServerLevel server) {
                items.addAll(Block.getDrops(state, server, target, level.getBlockEntity(target)));
                ItemStack block = new ItemStack(state.getBlock());
                if (!block.isEmpty() && items.stream().noneMatch(stack -> ItemStack.isSameItemSameTags(stack, block))) items.add(block);
            }
        } else {
            var tile = level.getBlockEntity(target);
            if (tile instanceof Container container) {
                for (int i = 0; i < Math.min(65536, container.getContainerSize()); i++) items.add(container.getItem(i).copy());
            } else if (tile != null) {
                var handler = tile.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
                validTarget = handler.isPresent();
                if (validTarget) for (int i = 0; i < Math.min(65536, handler.get().getSlots()); i++) items.add(handler.get().getStackInSlot(i).copy());
            } else validTarget = false;
        }
        return items;
    }
    public void refresh() {
        if (level == null || level.isClientSide) return;
        validTarget = false; Optional<BlockPos> target = ultimateTarget();
        ItemStack stack=stored(0);boolean powered=false;
        String kind=((TallyBlock)getBlockState().getBlock()).kind;
        if(target.isPresent()&&!kind.equals("world")&&!kind.equals("drops")&&level.getBlockEntity(target.get()) instanceof InventariumBlockEntity core){
            validTarget=core.capacity().loaded();
            if(validTarget&&stack.getItem() instanceof FilterItem item)powered=ItemFilter.load(stack).tallySnapshot(core.contents().entries(),item.blacklist,requireAll);
        }else{
            List<ItemStack> items=target.map(this::detected).orElseGet(List::of);
            powered=validTarget&&stack.getItem() instanceof FilterItem item&&ItemFilter.load(stack).tally(items,item.blacklist,requireAll);
        }
        if (powered != hasComparatorSignal()) {
            level.setBlock(worldPosition, getBlockState().setValue(TallyBlock.LIT, powered), 3);
            for (Direction direction : Direction.values()) level.updateNeighborsAt(worldPosition.relative(direction), getBlockState().getBlock());
        }
    }
    @Override protected void inventoryChanged() { super.inventoryChanged(); refresh(); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putBoolean("RequireAll", requireAll); }
    @Override public void load(CompoundTag tag) { super.load(tag); requireAll = !tag.contains("RequireAll") || tag.getBoolean("RequireAll"); }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new TallyMenu(id, inventory, this); }
}
