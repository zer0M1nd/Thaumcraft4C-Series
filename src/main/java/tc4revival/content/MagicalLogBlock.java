package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class MagicalLogBlock extends RotatedPillarBlock {
    public MagicalLogBlock(boolean node) { super(Properties.of().strength(2.5f).sound(SoundType.WOOD).lightLevel(state->node ? 7 : 0)); }
    @Override public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,Direction face) { return 5; }
    @Override public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,Direction face) { return 5; }
}
