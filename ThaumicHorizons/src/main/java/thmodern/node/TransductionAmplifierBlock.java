package thmodern.node;

import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import thmodern.Horizons;

public final class TransductionAmplifierBlock extends NodeDeviceBlock<TransductionAmplifierEntity> {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public TransductionAmplifierBlock(){super(Properties.of().strength(2).noOcclusion(),()->Horizons.TRANSDUCTION_AMPLIFIER_ENTITY.get(),TransductionAmplifierEntity::tick);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){var face=context.getClickedFace();if(!face.getAxis().isHorizontal())return null;var state=defaultBlockState().setValue(FACING,face);return canSurvive(state,context.getLevel(),context.getClickedPos())?state:null;}
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){var target=pos.relative(state.getValue(FACING).getOpposite());return level.hasChunkAt(target)&&level.getBlockEntity(target) instanceof tc4revival.content.EnergizedNodeBlockEntity;}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof TransductionAmplifierEntity amplifier)amplifier.revoke();super.onRemove(state,level,pos,next,moving);}
}
