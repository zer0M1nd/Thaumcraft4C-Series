package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public  class EnergizedNodeBlock extends BaseEntityBlock {
    private record Collapsing(Level level, BlockPos pos) {}
    private static final java.util.Set<Collapsing> COLLAPSING = new java.util.HashSet<>();
    public EnergizedNodeBlock() { super(Properties.of().strength(2, 200).noCollission().noOcclusion().lightLevel(state -> 8).noLootTable()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos position, CollisionContext context) { return box(4.8, 4.8, 4.8, 11.2, 11.2, 11.2); }
    @Override public BlockEntity newBlockEntity(BlockPos position, BlockState state) { return new EnergizedNodeBlockEntity(position, state); }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block block, BlockPos from, boolean moving) {
        if (level.isClientSide || !level.hasChunkAt(pos.below()) || !level.hasChunkAt(pos.above())) return;
        if (level.hasNeighborSignal(pos.below()) || !(level.getBlockEntity(pos.below()) instanceof NodeStabilizerBlockEntity)
                || !(level.getBlockEntity(pos.above()) instanceof NodeConverterBlockEntity)) collapse(level, pos);
    }
    public static void collapse(Level level, BlockPos pos) {
        if (level.isClientSide || !level.hasChunkAt(pos)) return;
        var key = new Collapsing(level, pos.immutable()); if (!COLLAPSING.add(key)) return;
        try {
            level.removeBlock(pos, false);
            level.explode(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 3, Level.ExplosionInteraction.NONE);
            // Legacy BlockAiry.explodify makes fifty independent attempts, including duplicates.
            for (int attempt = 0; attempt < 50; attempt++) {
                var target = pos.offset(level.random.nextInt(8) - level.random.nextInt(8),
                        level.random.nextInt(8) - level.random.nextInt(8),
                        level.random.nextInt(8) - level.random.nextInt(8));
                if (!level.isEmptyBlock(target)) continue;
                var flux = target.getY() < pos.getY() ? TCArcane.FLUX_GOO.get() : TCArcane.FLUX_GAS.get();
                level.setBlock(target, flux.defaultBlockState().setValue(FiniteFluidBlock.QUANTA, 9), 3);
            }
        } finally { COLLAPSING.remove(key); }
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TCArcane.ENERGIZED_NODE_ENTITY.get(), (world, position, block, node) -> node.tick());
    }
}
