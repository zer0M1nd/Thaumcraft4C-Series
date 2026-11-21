package automagy.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Greedy logistics must consult this before stealing from an adjacent inventory. */
public interface GreedyExtractionPermission {
    boolean allowGreedyExtraction(BlockPos collector, Direction inventorySide);
}
