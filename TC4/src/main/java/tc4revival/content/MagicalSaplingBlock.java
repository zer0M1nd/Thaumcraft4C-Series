package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** TC4 saplings grow directly, without vanilla staging or bonemeal growth. */
public  class MagicalSaplingBlock extends BushBlock {
    private final boolean silver;
    public MagicalSaplingBlock(boolean silver) { super(Properties.of().noCollission().instabreak().sound(SoundType.GRASS).randomTicks().lightLevel(state->silver ? 8 : 0)); this.silver=silver; }
    @Override public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.core.Direction face) { return 100; }
    @Override public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.core.Direction face) { return 60; }
    @Override public boolean canSurvive(BlockState state,net.minecraft.world.level.LevelReader level,BlockPos pos) {
        return (level.getRawBrightness(pos,0)>=8 || level.canSeeSky(pos)) && super.canSurvive(state,level,pos);
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return box(1.6,0,1.6,14.4,12.8,14.4); }
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if (!state.canSurvive(level,pos)) { level.destroyBlock(pos,true); return; }
        if (level.getMaxLocalRawBrightness(pos.above())<9 || random.nextInt(silver ? 50 : 25)!=0) return;
        growTree(state,level,pos,random);
    }
    public boolean isSilverwood() { return silver; }
    public void growTree(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        level.removeBlock(pos,false);
        boolean grown=silver ? SilverwoodGrowth.grow(level,pos,random,7,5) : GreatwoodGrowth.grow(level,pos,random);
        if (!grown) level.setBlock(pos,state,3);
    }
}
