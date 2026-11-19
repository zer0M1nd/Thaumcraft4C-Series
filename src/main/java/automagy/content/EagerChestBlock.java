package automagy.content;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class EagerChestBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public EagerChestBlock() { super(Properties.of().strength(2.5f).sound(SoundType.WOOD)); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return box(1,0,1,15,14,15); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new EagerChestBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, Content.EAGER_TILE.get(), EagerChestBlockEntity::tick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EagerChestBlockEntity tile) {
            boolean config=player.getItemInHand(hand).getItem() instanceof FilterItem||player.getItemInHand(hand).getItem() instanceof tc4revival.content.CastingWandItem;
            if(!config)tile.trade(player);
            var provider=new net.minecraft.world.SimpleMenuProvider((id,inv,p)->new EagerChestMenu(id,inv,tile,config),tile.getDisplayName());
            net.minecraftforge.network.NetworkHooks.openScreen((net.minecraft.server.level.ServerPlayer)player,provider,buf->{buf.writeBlockPos(pos);buf.writeBoolean(config);});
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity entity,net.minecraft.world.item.ItemStack stack) {
        if(!level.isClientSide && entity instanceof Player player && level.getBlockEntity(pos) instanceof EagerChestBlockEntity tile) tile.allow(player,true);
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof EagerChestBlockEntity tile ? net.minecraftforge.items.ItemHandlerHelper.calcRedstoneFromInventory(tile.storage()) : 0;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof EagerChestBlockEntity tile) { tile.dropContents(); level.updateNeighbourForOutputSignal(pos, this); }
        super.onRemove(state,level,pos,next,moving);
    }
}
