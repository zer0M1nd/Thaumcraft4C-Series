package automagy.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class TallyMenu extends AbstractContainerMenu {
    private final TallyBlockEntity tile;
    private final ContainerData data;
    public TallyMenu(int id, Inventory inventory, BlockPos pos) { this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof TallyBlockEntity tile ? tile : null); }
    public TallyMenu(int id, Inventory inventory, TallyBlockEntity tile) {
        super(Content.TALLY_MENU.get(), id); this.tile = tile;
        data = tile == null || inventory.player.level().isClientSide ? new SimpleContainerData(3) : new ContainerData() {
            public int get(int i) { return (i == 0 ? tile.requireAll : i == 1 ? tile.validTarget() : tile.hasComparatorSignal()) ? 1 : 0; }
            public void set(int i, int value) {} public int getCount() { return 3; }
        }; addDataSlots(data);
        addSlot(new SlotItemHandler(tile == null ? new net.minecraftforge.items.ItemStackHandler(1) : tile.filterInventory(), 0, 80, 28));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
    }
    public int value(int index) { return data.get(index); }
    @Override public boolean stillValid(Player player) {
        return tile != null && !tile.isRemoved() && tile.getLevel() == player.level() && player.mayBuild()
                && player.level().getBlockEntity(tile.getBlockPos()) == tile && player.distanceToSqr(tile.getBlockPos().getCenter()) <= 64;
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (player.level().isClientSide || !stillValid(player) || button != 0) return false;
        tile.requireAll = !tile.requireAll; tile.refresh(); tile.sync(); broadcastChanges(); return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem(); var copy = stack.copy();
        if (index == 0) { if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY; }
        else if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged(); slot.onTake(player, stack); return copy;
    }
}
