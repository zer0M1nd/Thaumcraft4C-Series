package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public  class NodeStabilizerBlockEntity extends BlockEntity {
    private int extension;
    public NodeStabilizerBlockEntity(BlockPos pos, BlockState state) { super(TCArcane.NODE_STABILIZER_ENTITY.get(), pos, state); }
    public int lockStrength() { return getBlockState().is(TCArcane.ADVANCED_NODE_STABILIZER.get()) ? 2 : 1; }
    public int extension() { return extension; }
    public void tick() {
        if (level == null || !level.isClientSide) return;
        var above = level.hasChunkAt(worldPosition.above()) ? level.getBlockEntity(worldPosition.above()) : null;
        boolean active = (above instanceof AuraNodeBlockEntity || above instanceof EnergizedNodeBlockEntity) && !level.hasNeighborSignal(worldPosition);
        extension = Math.max(0, Math.min(37, extension + (active ? 1 : -1)));
    }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).expandTowards(0, 1, 0); }
}
