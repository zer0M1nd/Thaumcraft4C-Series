package automagy.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Optional block contract for shelves with a slot allowance other than the ordinary 81. */
public interface InventariumShelf {
    int inventariumSlots(BlockState state,BlockGetter level,BlockPos position);
}
