package automagy;

import automagy.content.InventoryTransfer;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import static automagy.gametest.Checks.*;

@net.minecraftforge.gametest.GameTestHolder("automagy")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public class InventoryTransferTest {
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void transferFillsPartialStacksBeforeEmptySlots(net.minecraft.gametest.framework.GameTestHelper h) {
        var source = new ItemStackHandler(1); source.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 20));
        var target = new ItemStackHandler(2); target.setStackInSlot(1, new ItemStack(Items.IRON_INGOT, 60));
        var spill = new ArrayList<ItemStack>();
        int moved = InventoryTransfer.move(source, target, source.getStackInSlot(0), 20, ItemStack::isSameItemSameTags, spill::add);
        assertEquals(20, moved); assertEquals(64, target.getStackInSlot(1).getCount());
        assertEquals(16, target.getStackInSlot(0).getCount()); assertTrue(source.getStackInSlot(0).isEmpty()); assertTrue(spill.isEmpty());
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void failedActualInsertionRestoresExtractedItems(net.minecraft.gametest.framework.GameTestHelper h) {
        var source = new ItemStackHandler(1); source.setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
        var target = new ItemStackHandler(1) {
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return simulate ? ItemStack.EMPTY : stack.copy(); }
        };
        var spill = new ArrayList<ItemStack>();
        assertEquals(0, InventoryTransfer.move(source, target, source.getStackInSlot(0), 3, ItemStack::isSameItemSameTags, spill::add));
        assertEquals(3, source.getStackInSlot(0).getCount()); assertTrue(spill.isEmpty());
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void extractDeniedInventoryAndSelfTransfersAreUntouched(net.minecraft.gametest.framework.GameTestHelper h) {
        var source = new ItemStackHandler(1) {
            @Override public ItemStack extractItem(int slot, int count, boolean simulate) { return ItemStack.EMPTY; }
        };
        source.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 10));
        assertTrue(InventoryTransfer.snapshot(source, true).isEmpty());
        assertEquals(0, InventoryTransfer.move(source, new ItemStackHandler(1), source.getStackInSlot(0), 10, ItemStack::isSameItemSameTags, s -> fail()));
        assertEquals(0, InventoryTransfer.move(source, source, source.getStackInSlot(0), 10, ItemStack::isSameItemSameTags, s -> fail()));
        assertEquals(10, source.getStackInSlot(0).getCount());
    h.succeed();
    }
}
