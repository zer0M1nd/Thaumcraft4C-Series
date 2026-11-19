package automagy.content;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.*;

/** Server callers own scheduling; this class never loads chunks or bypasses sided extraction rules. */
public final class InventoryTransfer {
    public static Optional<IItemHandler> at(Level level, BlockPos pos, Direction face) {
        if (!level.hasChunkAt(pos)) return Optional.empty();
        var tile = level.getBlockEntity(pos); if (tile == null) return Optional.empty();
        var capability = tile.getCapability(ForgeCapabilities.ITEM_HANDLER, face).resolve();
        if (capability.isPresent()) return capability.filter(handler -> handler.getSlots() <= 65536);
        if (tile instanceof WorldlyContainer sided && face != null) return Optional.of(new SidedInvWrapper(sided, face));
        return tile instanceof Container container && container.getContainerSize() <= 65536 ? Optional.of(new InvWrapper(container)) : Optional.empty();
    }
    public static List<ItemStack> snapshot(IItemHandler inventory, boolean extractableOnly) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < Math.min(65536, inventory.getSlots()); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (extractableOnly) stack = inventory.extractItem(slot, stack.getCount(), true);
            if (!stack.isEmpty()) items.add(stack.copy());
        }
        return items;
    }
    public static long count(List<ItemStack> items, ItemStack key, BiPredicate<ItemStack, ItemStack> matching) {
        long count = 0;
        for (ItemStack stack : items) if (matching.test(key, stack)) count += stack.getCount();
        return count;
    }
    public static ItemStack insert(IItemHandler inventory, ItemStack stack, boolean simulate) {
        ItemStack remainder = stack.copy();
        // Existing stacks first prevents consuming an empty slot when a later partial stack fits.
        for (int pass = 0; pass < 2 && !remainder.isEmpty(); pass++) {
            for (int slot = 0; slot < Math.min(65536, inventory.getSlots()) && !remainder.isEmpty(); slot++) {
                boolean empty = inventory.getStackInSlot(slot).isEmpty();
                if (empty != (pass == 1)) continue;
                remainder = inventory.insertItem(slot, remainder, simulate);
            }
        }
        return remainder;
    }
    public static int room(IItemHandler inventory, ItemStack key, int maximum) {
        if (key.isEmpty() || maximum <= 0) return 0;
        ItemStack request = key.copyWithCount(maximum);
        return maximum - insert(inventory, request, true).getCount();
    }
    /** Unexpected remainders are restored or handed back to the caller for safe world dropping. */
    public static int move(IItemHandler source, IItemHandler destination, ItemStack key, int maximum,
                           BiPredicate<ItemStack, ItemStack> matching, Consumer<ItemStack> spill) {
        if (source == destination || key.isEmpty() || maximum <= 0) return 0;
        int moved = 0;
        for (int slot = 0; slot < Math.min(65536, source.getSlots()) && moved < maximum; slot++) {
            ItemStack simulated = source.extractItem(slot, maximum - moved, true);
            if (simulated.isEmpty() || !matching.test(key, simulated)) continue;
            int fits = room(destination, simulated, simulated.getCount()); if (fits <= 0) continue;
            ItemStack extracted = source.extractItem(slot, fits, false); if (extracted.isEmpty()) continue;
            ItemStack rest;
            if (!matching.test(key, extracted)) rest = extracted;
            else { rest = insert(destination, extracted, false); moved += extracted.getCount() - rest.getCount(); }
            if (!rest.isEmpty()) {
                rest = source.insertItem(slot, rest, false);
                if (!rest.isEmpty()) rest = insert(source, rest, false);
                if (!rest.isEmpty()) spill.accept(rest.copy());
            }
        }
        return moved;
    }
    private InventoryTransfer() {}
}
