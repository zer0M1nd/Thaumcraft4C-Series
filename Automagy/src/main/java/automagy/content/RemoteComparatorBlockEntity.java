package automagy.content;

import automagy.api.*;
import java.util.Optional;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

public final class RemoteComparatorBlockEntity extends InventoryBlockEntity {
    private int power;
    public RemoteComparatorBlockEntity(BlockPos pos, BlockState state) { super(Content.REMOTE_COMPARATOR_TILE.get(),pos,state,1); }
    @Override protected int slotLimit(int slot) { return 1; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return stack.getItem() instanceof LocationLink link && link.linkedPosition(stack).isPresent(); }
    public Optional<BlockPos> target() {
        var stack=stored(0);
        if (level==null || !(stack.getItem() instanceof LocationLink link)) return Optional.empty();
        return link.linkedPosition(stack).filter(pos->pos.dimension().equals(level.dimension()) && pos.pos().distSqr(worldPosition)<=400)
                .map(GlobalPos::pos).filter(level::hasChunkAt);
    }
    public boolean overridden() { return level!=null && level.getBlockEntity(worldPosition.below()) instanceof RemoteComparatorHost; }
    public int signal() { return overridden()?0:power; }
    public boolean visuallyPowered() { return level!=null && level.getBlockEntity(worldPosition.below()) instanceof RemoteComparatorHost host ? host.hasComparatorSignal() : power>0; }
    public ItemStack insertEye(ItemStack stack) { return inventory.insertItem(0,stack,false); }
    public ItemStack removeEye() { return inventory.extractItem(0,1,false); }
    @Override protected void inventoryChanged() { super.inventoryChanged();if(level!=null && !level.isClientSide) refresh(); }
    public static void tick(Level level, BlockPos pos, BlockState state, RemoteComparatorBlockEntity be) {
        if (level.getGameTime()%5==Math.floorMod(pos.asLong(),5)) be.refresh();
    }
    public void refresh() {
        if (level==null || level.isClientSide) return;
        int next=overridden()?0:target().filter(pos->!pos.equals(worldPosition)).map(this::reading).orElse(0);
        next=Math.max(0,Math.min(15,next));
        if (next!=power) {
            power=next;sync();level.updateNeighborsAt(worldPosition,getBlockState().getBlock());
            for (Direction d:Direction.values()) level.updateNeighborsAt(worldPosition.relative(d),getBlockState().getBlock());
        }
    }
    private int reading(BlockPos target) {
        var state=level.getBlockState(target);
        if (state.hasAnalogOutputSignal()) return state.getAnalogOutputSignal(level,target);
        var tile=level.getBlockEntity(target);
        if (tile instanceof Container container) return AbstractContainerMenu.getRedstoneSignalFromContainer(container);
        if (tile==null) return 0;
        var handler=tile.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
        if (handler.isEmpty()) return 0;
        int slots=handler.get().getSlots();
        if (slots==0 || slots>65536) return 0;
        double fullness=0;boolean any=false;
        for (int i=0;i<slots;i++) {
            var stack=handler.get().getStackInSlot(i);
            if (!stack.isEmpty()) {
                any=true; fullness+=Math.min(1, (double)stack.getCount()/Math.max(1,Math.min(stack.getMaxStackSize(),handler.get().getSlotLimit(i))));
            }
        }
        return (int)(fullness/slots*14)+(any?1:0);
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag);tag.putInt("Power",power); }
    @Override public void load(CompoundTag tag) { super.load(tag);power=Math.max(0,Math.min(15,tag.getInt("Power"))); }
}
