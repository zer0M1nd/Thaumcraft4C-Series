package tc4revival.content;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Registry tags admit modern and modded trees without joining unrelated species. */
public class LegacyWoodLogs {
    private LegacyWoodLogs() {}
    public static boolean isLog(BlockState state) { return state.is(net.minecraft.tags.BlockTags.LOGS); }
    public static boolean sameBlock(Block first,Block second) { return first==second && isLog(first.defaultBlockState()); }
}
