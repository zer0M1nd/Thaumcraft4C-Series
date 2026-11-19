package thmodern;

import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

public final class NodeMonitorBlock extends BaseEntityBlock {
 public static final DirectionProperty FACING=BlockStateProperties.FACING;
 public static final BooleanProperty ACTIVE=BlockStateProperties.POWERED;
 public NodeMonitorBlock(){super(Properties.of().strength(.7f,1).sound(SoundType.GLASS).lightLevel(s->7).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP).setValue(ACTIVE,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,ACTIVE);}
 @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new NodeMonitorEntity(pos,state);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext context){var state=defaultBlockState().setValue(FACING,context.getClickedFace());return canSurvive(state,context.getLevel(),context.getClickedPos())?state:null;}
 @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){var support=pos.relative(state.getValue(FACING).getOpposite());return !level.hasChunkAt(support)||level.getBlockEntity(support) instanceof tc4revival.content.ScannableNode;}
 @Override public boolean isSignalSource(BlockState state){return true;}
 @Override public int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){return state.getValue(ACTIVE)?15:0;}
 @Override public int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){return getSignal(state,level,pos,side);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,Horizons.NODE_MONITOR_ENTITY.get(),(l,p,s,e)->e.tick());}
}
