package thmodern.plane;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** The legacy dust is a shallow, slowing surface, not falling sand. */
public final class PlanarDustBlock extends Block {
 private static final VoxelShape COLLISION=Block.box(0,0,0,16,14,16);
 public PlanarDustBlock(){super(Properties.of().strength(.5f).sound(SoundType.SAND));}
 @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return COLLISION;}
 @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){entity.setDeltaMovement(entity.getDeltaMovement().multiply(.4,1,.4));}
}
