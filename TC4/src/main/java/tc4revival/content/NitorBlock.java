package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
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

public  class NitorBlock extends BaseEntityBlock {
    public NitorBlock() { super(Properties.of().strength(0).sound(new net.minecraft.world.level.block.SoundType(0, 1,
            net.minecraft.sounds.SoundEvents.WOOL_BREAK, net.minecraft.sounds.SoundEvents.WOOL_STEP, net.minecraft.sounds.SoundEvents.WOOL_PLACE,
            net.minecraft.sounds.SoundEvents.WOOL_HIT, net.minecraft.sounds.SoundEvents.WOOL_FALL)).noCollission().noOcclusion().lightLevel(state -> 15)); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return box(4.8, 4.8, 4.8, 11.2, 11.2, 11.2); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new NitorBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, TCArcane.NITOR_ENTITY.get(), (world, pos, block, nitor) -> tc4revival.client.NitorParticles.emit(world, pos, false)) : null;
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) { tc4revival.client.NitorParticles.emit(level, pos, true); }
}
