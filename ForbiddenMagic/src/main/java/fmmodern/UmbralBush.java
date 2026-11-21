package fmmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class UmbralBush extends DoublePlantBlock implements BonemealableBlock {
    public UmbralBush() { super(Properties.copy(Blocks.ROSE_BUSH).randomTicks()); }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { if (state.getValue(HALF)==DoubleBlockHalf.LOWER && random.nextInt(10)<=2) spread(level,pos,random); }
    @Override public boolean isValidBonemealTarget(LevelReader l, BlockPos p, BlockState s, boolean client) { return true; }
    @Override public boolean isBonemealSuccess(Level l, RandomSource r, BlockPos p, BlockState s) { return true; }
    @Override public void performBonemeal(ServerLevel l, RandomSource r, BlockPos p, BlockState s) { spread(l, s.getValue(HALF)==DoubleBlockHalf.UPPER ? p.below() : p,r); }
    private void spread(ServerLevel level, BlockPos origin, RandomSource random) {
        BlockPos cursor=origin;
        var flower=Content.INK_FLOWER.get().defaultBlockState();
        for(int i=0;i<9;i++) { var next=cursor.offset(random.nextInt(3)-1,random.nextInt(2)-random.nextInt(2),random.nextInt(3)-1); if(level.isLoaded(next)&&level.isEmptyBlock(next)&&flower.canSurvive(level,next)) { cursor=next; if(i==8) level.setBlock(next,flower,3); } }
    }
}
