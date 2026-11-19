package automagy.content;

import automagy.api.LocationLink;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class RemoteComparatorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public RemoteComparatorBlock() {
        super(Properties.of().strength(2.5f).noOcclusion().sound(SoundType.STONE));
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state=defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());
        return state.canSurvive(context.getLevel(),context.getClickedPos())?state:null;
    }
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        var support=level.getBlockState(pos.below());return support.isFaceSturdy(level,pos.below(),Direction.UP)||support.is(Blocks.GLASS)||support.is(Blocks.GLOWSTONE);
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos changed,boolean moving) {
        if (!level.isClientSide && !state.canSurvive(level,pos)) level.destroyBlock(pos,true);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return box(0,0,0,16,2,16); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new RemoteComparatorBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return level.isClientSide?null:createTickerHelper(type,Content.REMOTE_COMPARATOR_TILE.get(),RemoteComparatorBlockEntity::tick);
    }
    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side) { return level.getBlockEntity(pos) instanceof RemoteComparatorBlockEntity be?be.signal():0; }
    @Override public int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side) { return side==Direction.UP?0:getSignal(state,level,pos,side); }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof RemoteComparatorBlockEntity be) be.dropContents();
        super.onRemove(state,level,pos,next,moving);
        if (!state.is(next.getBlock())) for (Direction d:Direction.values()) level.updateNeighborsAt(pos.relative(d),this);
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (!player.mayBuild()) return InteractionResult.FAIL;
        if (!(level.getBlockEntity(pos) instanceof RemoteComparatorBlockEntity be)) return InteractionResult.PASS;
        var held=player.getItemInHand(hand);
        if (!(held.isEmpty()||held.getItem() instanceof LocationLink)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (!be.stored(0).isEmpty()) {
                var removed=be.removeEye();if(!player.getInventory().add(removed))player.drop(removed,false);
            } else if (!held.isEmpty()) {
                if (held.getItem() instanceof LocationLink link && link.linkedPosition(held).isPresent()) {
                    var one=held.copy();one.setCount(1);
                    if (be.insertEye(one).isEmpty() && !player.getAbilities().instabuild) held.shrink(1);
                } else player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.automagy.eye.unlinked"),true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
