package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Orientation lives in synchronized block state in the modern port. */
public  class CrystalClusterBlockEntity extends BlockEntity {
    public CrystalClusterBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.CRYSTAL_CLUSTER_ENTITY.get(),pos,state); }
}
