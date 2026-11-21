package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public  class ManaPodBlock extends Block implements EntityBlock {
    public static final IntegerProperty AGE=IntegerProperty.create("age",0,7);
    private static final int[] BOTTOM={12,10,8,6,5,4,3,2};
    public ManaPodBlock() {
        super(Properties.of().strength(.5f).sound(SoundType.STONE).noOcclusion().randomTicks().lightLevel(state->state.getValue(AGE)));
        registerDefaultState(stateDefinition.any().setValue(AGE,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(AGE); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new ManaPodBlockEntity(pos,state); }
    @Override public net.minecraft.world.item.ItemStack getCloneItemStack(BlockGetter level,BlockPos pos,BlockState state) {
        return new net.minecraft.world.item.ItemStack(TCItems.MANA_BEAN.get());
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return box(4,BOTTOM[state.getValue(AGE)],4,12,16,12);
    }
    @Override public float getDestroyProgress(BlockState state,net.minecraft.world.entity.player.Player player,BlockGetter level,BlockPos pos) {
        // The old hardness is .5 / (8 - metadata); preserve the native tool-speed calculation.
        return super.getDestroyProgress(state,player,level,pos)*(8-state.getValue(AGE));
    }
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        if (!LegacyBiomeProfiles.get(level.getBiome(pos)).magical()) return false;
        var above=level.getBlockState(pos.above());
        return above.is(net.minecraft.tags.BlockTags.LOGS);
    }
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if (!canSurvive(state,level,pos)) { level.destroyBlock(pos,true); return; }
        if (level.random.nextInt(30)==0 && level.getBlockEntity(pos) instanceof ManaPodBlockEntity pod) pod.grow();
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block neighbour,BlockPos from,boolean moving) {
        if (!level.isClientSide && !canSurvive(state,level,pos)) level.destroyBlock(pos,true);
    }
}
