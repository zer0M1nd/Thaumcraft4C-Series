package automagy.content;

import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

public final class GlowingAmberBlock extends Block {
    public GlowingAmberBlock(){super(Properties.of().strength(1.5f,3).sound(SoundType.STONE).lightLevel(state->15).noOcclusion());}
    @Override public int getLightBlock(BlockState state,BlockGetter level,BlockPos pos){return 3;}
    @Override public boolean skipRendering(BlockState state,BlockState adjacent,Direction side){return state.is(adjacent.getBlock())||super.skipRendering(state,adjacent,side);}
}
