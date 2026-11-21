package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The transient, air-like metadata-10 effect from BlockAiry. */
public  class ShockFieldBlock extends Block {
    public ShockFieldBlock() {
        super(Properties.of().air().replaceable().noCollission().noOcclusion().strength(100, 50)
                .lightLevel(state -> 8).randomTicks().noLootTable());
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { level.removeBlock(pos, false); }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { level.removeBlock(pos, false); }
    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide) {
            entity.hurt(level.damageSources().magic(), 1 + level.random.nextInt(2));
            if (level.random.nextInt(100) == 0) level.removeBlock(pos, false);
        }
        var velocity = entity.getDeltaMovement(); entity.setDeltaMovement(velocity.multiply(.8, 1, .8));
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        tc4revival.client.ClientFocusEffects.groundSpark(level, pos);
        if (random.nextInt(50) == 0) level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), TCSounds.JACOBS.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, .5f, 1 + (random.nextFloat() - random.nextFloat()) * .2f, false);
    }
}
