package automagy.content;

import net.minecraft.core.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

/** Analog inversion: 15 - input, including intermediate signal levels. */
public final class InversionTorchBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public InversionTorchBlock() {
        super(Properties.of().noCollission().instabreak().sound(SoundType.WOOD).lightLevel(state -> state.getValue(POWER)>0 ? 7 : 0));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(POWER, 15));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, POWER); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction dir : context.getNearestLookingDirections()) {
            if (dir == Direction.UP) continue;
            BlockState state = defaultBlockState().setValue(FACING, dir.getOpposite());
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) return state;
        }
        return null;
    }
    @Override public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        Direction outward = state.getValue(FACING);
        BlockPos support = pos.relative(outward.getOpposite());
        return outward != Direction.DOWN && world.getBlockState(support).isFaceSturdy(world, support, outward);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Y ? Block.box(6,0,6,10,10,10) : Block.box(3,3,3,13,14,13);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new InversionTorchBlockEntity(pos, state); }
    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction side) { return state.getValue(POWER); }
    @Override public int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
        return side == Direction.DOWN ? state.getValue(POWER) : 0;
    }
    @Override public void onPlace(BlockState state, Level world, BlockPos pos, BlockState old, boolean moving) {
        if (!world.isClientSide && !old.is(this)) world.scheduleTick(pos, this, 2);
    }
    @Override public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos changed, boolean moving) {
        if (world.isClientSide) return;
        if (!state.canSurvive(world, pos)) world.destroyBlock(pos, true);
        else world.scheduleTick(pos, this, 2);
    }
    @Override public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (world.getBlockEntity(pos) instanceof InversionTorchBlockEntity tile) tile.update();
    }
    @Override public void onRemove(BlockState state, Level world, BlockPos pos, BlockState next, boolean moving) {
        super.onRemove(state, world, pos, next, moving);
        if (!state.is(next.getBlock())) {
            world.updateNeighborsAt(pos, this);
            for (Direction d : Direction.values()) world.updateNeighborsAt(pos.relative(d), this);
        }
    }
    @Override public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (state.getValue(POWER)>0) world.addParticle(DustParticleOptions.REDSTONE,
                pos.getX()+.5+(random.nextDouble()-.5)*.2, pos.getY()+.7, pos.getZ()+.5+(random.nextDouble()-.5)*.2, 0,0,0);
    }
}
