package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public  class MagicalPlanksBlock extends Block {
    public MagicalPlanksBlock() { super(Properties.of().strength(2.5f,6).sound(SoundType.WOOD)); }
    @Override public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,Direction face) { return 20; }
    @Override public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,Direction face) { return 5; }
}
