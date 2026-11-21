package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public  class WandPedestalBlock extends BaseEntityBlock {
    private static final VoxelShape COLLISION = Shapes.or(box(0, 0, 0, 16, 4, 16), box(2, 4, 2, 14, 8, 14), box(4, 8, 4, 12, 16, 12));
    public WandPedestalBlock() { super(Properties.of().strength(3, 15).requiresCorrectToolForDrops().noOcclusion()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return box(4, 0, 4, 12, 16, 12); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return COLLISION; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new WandPedestalBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TCArcane.WAND_PEDESTAL_ENTITY.get(), (world, pos, block, pedestal) -> pedestal.tick());
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) { return level.getBlockEntity(pos) instanceof WandPedestalBlockEntity pedestal ? pedestal.comparatorOutput() : 0; }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) { return interact(level, pos, player, hand); }
    public static InteractionResult interact(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).is(TCItems.WAND_PEDESTAL_FOCUS.get())) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof WandPedestalBlockEntity pedestal)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        float pitch;
        if (!pedestal.isEmpty()) {
            var stack = pedestal.removeItem(0, 1); var item = new net.minecraft.world.entity.item.ItemEntity(level, player.getX(), player.getY(), player.getZ(), stack);
            item.setDefaultPickUpDelay(); level.addFreshEntity(item); pitch = 1.5f;
        } else if (WandPedestalBlockEntity.chargeable(player.getItemInHand(hand))) {
            pedestal.setItem(0, player.getItemInHand(hand).split(1)); player.getInventory().setChanged(); pitch = 1.6f;
        } else return InteractionResult.PASS;
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, .2f, ((level.random.nextFloat() - level.random.nextFloat()) * .7f + 1) * pitch);
        return InteractionResult.CONSUME;
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (!level.isClientSide && level.hasChunkAt(pos.above()) && !level.isEmptyBlock(pos.above()) && !level.getBlockState(pos.above()).is(TCArcane.WAND_PEDESTAL_FOCUS.get())
                && level.getBlockEntity(pos) instanceof WandPedestalBlockEntity pedestal) { Containers.dropContents(level, pos, pedestal); pedestal.clearContent(); }
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!next.is(this) && !level.isClientSide && level.getBlockEntity(pos) instanceof WandPedestalBlockEntity pedestal) { Containers.dropContents(level, pos, pedestal); level.updateNeighbourForOutputSignal(pos, this); }
        super.onRemove(state, level, pos, next, moving);
    }
}
