package automagy.content;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public final class VisReaderBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public VisReaderBlock(){super(Properties.of().strength(2.5f).sound(SoundType.STONE));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new VisReaderBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Content.VIS_READER_TILE.get(),VisReaderBlockEntity::tick);}
    @Override public boolean isSignalSource(BlockState state){return true;}
    @Override public int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){return level.getBlockEntity(pos) instanceof VisReaderBlockEntity reader?Math.min(15,reader.amplifiedSignal(side.getOpposite())):0;}
    @Override public int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){return getSignal(state,level,pos,side);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!player.mayBuild())return InteractionResult.FAIL;
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof VisReaderBlockEntity reader)NetworkHooks.openScreen((ServerPlayer)player,reader,pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof VisReaderBlockEntity reader)reader.dropContents();
        super.onRemove(state,level,pos,next,moving);
        if(!state.is(next.getBlock()))for(Direction d:Direction.values())level.updateNeighborsAt(pos.relative(d),this);
    }
}
