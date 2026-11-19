package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public  class MnemonicMatrixBlock extends Block {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public MnemonicMatrixBlock() {
        super(Properties.of().strength(3,17).sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.DOWN));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var facing=context.getClickedFace().getOpposite(); var support=context.getClickedPos().relative(facing);
        return context.getLevel().getBlockState(support).is(TCArcane.THAUMATORIUM.get()) ? defaultBlockState().setValue(FACING,facing) : null;
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return box(3,3,3,13,13,13); }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block from,BlockPos fromPos,boolean moving) {
        if (!level.isClientSide && level.isEmptyBlock(pos.relative(state.getValue(FACING)))) level.destroyBlock(pos,true);
    }
}
