package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.Aspect;
import java.util.List;

public  class FocalManipulatorMenu extends AbstractContainerMenu {
    private static final List<String> LEGACY_PRIMALS = List.of("aer", "ignis", "aqua", "terra", "ordo", "perditio");
    private final List<Aspect> primals;
    private static List<Aspect> primalOrder() {
        return TCAspects.catalog().primals().stream().sorted(java.util.Comparator
                .comparingInt((Aspect a) -> { int i = LEGACY_PRIMALS.indexOf(a.tag()); return i < 0 ? 6 : i; })
                .thenComparing(Aspect::tag)).toList();
    }
    public List<Aspect> primals() { return primals; }
    private final Container contents;
    private final ContainerData data;
    private final BlockPos position;
    private final FocalManipulatorBlockEntity table;
    public FocalManipulatorMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, new SimpleContainer(1), buffer.readBlockPos(), null, primalOrder());
    }
    public FocalManipulatorMenu(int id, Inventory inventory, FocalManipulatorBlockEntity table) {
        this(id, inventory, table, table.getBlockPos(), table, primalOrder());
    }
    private FocalManipulatorMenu(int id, Inventory inventory, Container contents, BlockPos pos,
            FocalManipulatorBlockEntity table, List<Aspect> primals) {
        super(TCArcane.FOCAL_MENU.get(), id);
        this.contents = contents; position = pos; this.table = table; this.primals = primals;
        int base = 4 + 2 * primals.size();
        data = table == null ? new SimpleContainerData(11 + 3 * primals.size()) : new ContainerData() {
            @Override public int get(int index) {
                if (index >= base + 7) return table.stored().amount(primals.get(index - base - 7));
                if (index == base) return tc4revival.config.TCConfig.SERVER.revival.batchFocusUpgrades.get() ? 1 : 0;
                if (index == base + 1) return table.queue().size();
                if (index >= base + 2) return index - base - 2 < table.queue().size() ? table.queue().get(index - base - 2) : -1;
                if (index == 2) return table.rank();
                if (index == 3) return table.upgrade();
                int value = index < 2 ? table.size() : table.remaining().amount(primals.get((index - 4) / 2));
                return (value >>> ((index & 1) * 16)) & 65535;
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 11 + 3 * primals.size(); }
        };
        addSlot(new Slot(contents, 0, 88, 60) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof WandFocusItem; }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, 9 + row * 9 + column, 16 + column * 18, 151 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 16 + column * 18, 209));
        addDataSlots(data);
    }
    private int full(int index) { return data.get(index) & 65535 | data.get(index + 1) << 16; }
    public int size() { return full(0); }
    private int queueBase() { return 4 + 2 * primals.size(); }
    public boolean batchEnabled() { return data.get(queueBase()) != 0; }
    public java.util.List<Integer> queue() {
        var result = new java.util.ArrayList<Integer>();
        for (int i = 0; i < Math.min(5, data.get(queueBase() + 1)); i++) result.add(data.get(queueBase() + 2 + i));
        return java.util.List.copyOf(result);
    }
    public boolean startBatch(Player player, ItemStack expected, java.util.List<Integer> choices) {
        if (table == null || !stillValid(player) || player.containerMenu != this) return false;
        boolean success = table.startBatch(player, expected, choices);
        if (!success) table.sound(TCSounds.CRAFT_FAIL.get(), .33f);
        broadcastChanges(); return success;
    }
    public int rank() { return data.get(2); }
    public int upgrade() { return data.get(3); }
    public ItemStack focus() { return contents.getItem(0); }
    public AspectAmounts stored() {
        var result = new AspectAmounts();
        for (int i = 0; i < primals.size(); i++) result.add(primals.get(i), Math.max(0, data.get(queueBase() + 7 + i)));
        return result;
    }
    public AspectAmounts remaining() {
        var result = new AspectAmounts();
        for (int i = 0; i < primals.size(); i++) {
            int value = full(4 + i * 2); if (value > 0) result.add(primals.get(i), value);
        }
        return result;
    }
    @Override public boolean stillValid(Player player) {
        return player.isAlive() && player.level().getBlockState(position).is(TCArcane.FOCAL_BLOCK.get())
                && player.distanceToSqr(position.getX() + .5, position.getY() + .5, position.getZ() + .5) <= 64;
    }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (table == null || !stillValid(player) || player.containerMenu != this || id < 0) return false;
        if (!table.start(player, id)) { table.sound(TCSounds.CRAFT_FAIL.get(), .33f); return false; }
        broadcastChanges(); return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem(); var before = stack.copy();
        boolean moved = index == 0 ? moveItemStackTo(stack, 1, 37, false)
                : stack.getItem() instanceof WandFocusItem ? moveItemStackTo(stack, 0, 1, false)
                : index < 28 ? moveItemStackTo(stack, 28, 37, false) : moveItemStackTo(stack, 1, 28, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (before.getCount() == stack.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack); return before;
    }
}
