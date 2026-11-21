package automagy.content;

import net.minecraft.core.BlockPos;

/** Request terminals reserve memory independently of inventory-reading scribes. */
public interface InventariumMemoryUser {
    BlockPos linkedCore();
    void linkCore(BlockPos position);
}
