package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public  class TallowCandleBlock extends Block {
    public static final int[] COLORS={0xF0F0F0,15435844,12801229,6719955,14602026,4312372,14188952,0x434343,0xA0A0A0,2651799,8073150,2437522,5320730,3887386,11743532,0x1E1B1B};
    public final int color;
    public TallowCandleBlock(int color) { super(Properties.of().strength(.1f).sound(SoundType.WOOL).noCollission().noOcclusion().lightLevel(s->14)); this.color=color; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return box(6,0,6,10,8,10); }
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) { return level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP); }
    @Override public BlockState updateShape(BlockState state,Direction side,BlockState adjacent,LevelAccessor level,BlockPos pos,BlockPos other) { return !canSurvive(state,level,pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state,side,adjacent,level,pos,other); }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random) {
        level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+.7,pos.getZ()+.5,0,0,0);
        level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,pos.getX()+.5,pos.getY()+.7,pos.getZ()+.5,0,0,0);
    }

}
