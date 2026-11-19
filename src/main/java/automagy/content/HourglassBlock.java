package automagy.content;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.network.NetworkHooks;
import thaumcraft.api.wands.IWandable;

public final class HourglassBlock extends BaseEntityBlock implements IWandable {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(5.6, 0, 5.6, 10.4, 6.4, 10.4);
    public final boolean magic;
    public HourglassBlock(boolean magic) {
        super(Properties.of().strength(1).noOcclusion().sound(magic ? SoundType.METAL : SoundType.WOOD));
        this.magic = magic;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(below.getBlock());
        return below.isFaceSturdy(level, pos.below(), Direction.UP) || below.is(Blocks.GLASS)
                || below.is(Blocks.GLOWSTONE) || key != null && key.getNamespace().equals("thaumcraft") && key.getPath().contains("pedestal");
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HourglassBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, Content.HOURGLASS_TILE.get(), HourglassBlockEntity::tick);
    }
    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        if (side.getAxis() == state.getValue(FACING).getAxis()) return 0;
        return level.getBlockEntity(pos) instanceof HourglassBlockEntity be ? be.timer.signal() : 0;
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof HourglassBlockEntity be ? be.timer.comparator() : 0;
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block from, BlockPos fromPos, boolean moving) {
        if (level.isClientSide) return;
        if (!state.canSurvive(level, pos)) { level.destroyBlock(pos, true); return; }
        if (level.getBlockEntity(pos) instanceof HourglassBlockEntity be) be.refreshInput();
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        super.onRemove(state, level, pos, next, moving);
        if (!state.is(next.getBlock())) { level.updateNeighborsAt(pos, this); level.updateNeighbourForOutputSignal(pos, this); }
    }
    @Override public InteractionResult onWandUseFirst(net.minecraft.world.item.ItemStack wand, UseOnContext context) {
        if (context.getPlayer() == null) return InteractionResult.PASS;
        return interact(context.getLevel(), context.getClickedPos(), context.getPlayer(), true);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(level, pos, player, !magic && player.getItemInHand(hand).is(Items.STICK)
                || player.getItemInHand(hand).getItem() instanceof tc4revival.content.CastingWandItem);
    }
    private InteractionResult interact(Level level, BlockPos pos, Player player, boolean configure) {
        if (!player.mayBuild()) return InteractionResult.FAIL;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HourglassBlockEntity be) {
            if (magic && configure) NetworkHooks.openScreen((ServerPlayer) player, be, pos);
            else if (configure) {
                be.timer.setSeconds(be.timer.target() / 20 >= 60 ? 5 : be.timer.target() / 20 + 5);
                be.changed();
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.automagy.hourglass.seconds", be.timer.target()/20), true);
            } else be.flip();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
