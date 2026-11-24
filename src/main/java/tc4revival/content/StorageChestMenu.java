package tc4revival.content;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;

/** Every stored item has a permanent network slot, including on pages the viewer has closed. */
public class StorageChestMenu extends ChestMenu {
    public static final int VISIBLE_ROWS = 6;
    private int page;

    public StorageChestMenu(int id, Inventory inventory, Container chest) {
        super(TCArcane.storageChestMenu(chest.getContainerSize()), id, inventory, chest, chest.getContainerSize() / 9);
    }

    public int pages() { return (getRowCount() + VISIBLE_ROWS - 1) / VISIBLE_ROWS; }
    public int page() { return page; }
    public void showPage(int requested) { page = Math.max(0, Math.min(pages() - 1, requested)); }

    @Override public boolean clickMenuButton(net.minecraft.world.entity.player.Player player, int requested) {
        if (requested < 0 || requested >= pages()) return false;
        showPage(requested);
        return true;
    }

    @Override protected Slot addSlot(Slot slot) {
        int index = slot.getContainerSlot();
        if (slot.container instanceof Inventory) {
            int offset = (getRowCount() - VISIBLE_ROWS) * 18;
            return super.addSlot(new Slot(slot.container, index, slot.x, slot.y - offset));
        }
        int row = index / 9;
        return super.addSlot(new Slot(slot.container, index, slot.x, 18 + row % VISIBLE_ROWS * 18) {
            @Override public boolean isActive() { return page == row / VISIBLE_ROWS; }
        });
    }
}
