package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

/** The physical pair shares one inventory owner; the other half only forwards interaction. */
public  class TableBlock extends BaseEntityBlock {
    public enum Part implements StringRepresentable {
        SINGLE, MASTER, EXTENSION;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final java.util.List<Direction> SEARCH_ORDER = java.util.List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);
    public TableBlock() {
        super(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(PART, Part.SINGLE).setValue(FACING, Direction.SOUTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART, FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getAxis() == Direction.Axis.X ? Direction.EAST : Direction.SOUTH);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public net.minecraft.world.item.ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target,
            BlockGetter level, BlockPos pos, Player player) {
        return new net.minecraft.world.item.ItemStack(state.getValue(PART) == Part.SINGLE ? TCItems.TABLE.get() : TCItems.RESEARCH_TABLE.get());
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ResearchTableBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TCTables.TABLE_ENTITY.get(), ResearchTableBlockEntity::tick);
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
    @Override public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Block.box(0, 15, 0, 16, 16, 16); // TC4 explicitly supports the upper face only.
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(PART) == Part.SINGLE) return Shapes.block();
        var direction = state.getValue(FACING);
        return Shapes.box(Math.min(0, direction.getStepX()), 0, Math.min(0, direction.getStepZ()),
                1 + Math.max(0, direction.getStepX()), 1, 1 + Math.max(0, direction.getStepZ()));
    }
    public static boolean paired(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(PART) == Part.SINGLE) return false;
        var other = pos.relative(state.getValue(FACING));
        if (!level.hasChunkAt(other)) return false;
        var mate = level.getBlockState(other);
        return mate.is(state.getBlock()) && mate.getValue(FACING)==state.getValue(FACING).getOpposite() && mate.getValue(PART) == (state.getValue(PART) == Part.MASTER ? Part.EXTENSION : Part.MASTER);
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        if (!level.isClientSide && state.getValue(PART) != Part.SINGLE
                && level.hasChunkAt(pos.relative(state.getValue(FACING))) && !paired(level, pos, state)) {
            level.setBlock(pos, defaultBlockState(), Block.UPDATE_ALL);
        }
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!level.isClientSide && (!next.is(this) || (state.getValue(PART) == Part.MASTER && next.getValue(PART) != Part.MASTER))) {
            if (level.getBlockEntity(pos) instanceof ResearchTableBlockEntity table) table.dropContents();
        }
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown() || state.getValue(PART) == Part.SINGLE) return InteractionResult.PASS;
        var owner = interactionOwner(level, pos, state);
        if (owner != null && player instanceof ServerPlayer server && level.getBlockEntity(owner) instanceof ResearchTableBlockEntity table && table.owner()) {
            NetworkHooks.openScreen(server, table, owner);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @javax.annotation.Nullable
    public static BlockPos interactionOwner(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(PART) == Part.MASTER) return pos;
        if (state.getValue(PART) == Part.SINGLE) return null;
        return paired(level,pos,state) ? pos.relative(state.getValue(FACING)) : null;
    }
}
