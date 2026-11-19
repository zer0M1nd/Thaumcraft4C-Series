package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public  class VisRelayBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = DirectionProperty.create("facing");
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");
    private final boolean charger;
    public VisRelayBlock(boolean charger) {
        super(Properties.of().strength(3, 10.2f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
                .lightLevel(state -> charger ? 0 : state.getValue(CONNECTED) ? 10 : 2));
        this.charger = charger;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(CONNECTED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, CONNECTED); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, charger ? Direction.UP : context.getClickedFace()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (charger ? Direction.DOWN : state.getValue(FACING)) {
            case DOWN -> box(5, 8, 5, 11, 16, 11);
            case UP -> box(5, 0, 5, 11, 8, 11);
            case NORTH -> box(5, 5, 8, 11, 11, 16);
            case SOUTH -> box(5, 5, 0, 11, 11, 8);
            case WEST -> box(8, 5, 5, 16, 11, 11);
            case EAST -> box(0, 5, 5, 8, 11, 11);
        };
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        var support = pos.relative(state.getValue(FACING).getOpposite());
        if (!charger && !level.isClientSide && level.hasChunkAt(support) && level.isEmptyBlock(support)) level.destroyBlock(pos, true);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof VisRelayBlockEntity relay)) return InteractionResult.PASS;
        for (var shard : ShardType.values()) if (player.getItemInHand(hand).is(TCItems.shard(shard))) {
            relay.setAttunement(shard.metadata() == relay.attunement() ? -1 : shard.metadata());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return charger ? new WorkbenchChargerBlockEntity(pos, state) : new VisRelayBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return charger ? createTickerHelper(type, TCArcane.WORKBENCH_CHARGER_ENTITY.get(), (world, pos, block, relay) -> relay.tick())
                : createTickerHelper(type, TCArcane.VIS_RELAY_ENTITY.get(), (world, pos, block, relay) -> relay.tick());
    }
}
