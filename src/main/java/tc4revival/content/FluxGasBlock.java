package tc4revival.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public  class FluxGasBlock extends FiniteFluidBlock {
    public FluxGasBlock() { super(Properties.of().noCollission().noOcclusion().noLootTable().randomTicks()
            .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY).lightLevel(state -> (state.getValue(QUANTA) - 1) * 7 / 8), -4, 2500); }
    @Override public net.minecraft.world.level.material.FluidState getFluidState(BlockState state) { return TCFluids.GAS.get().defaultFluidState().setValue(QUANTA, state.getValue(QUANTA)); }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { flow(level, pos, random); }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { flow(level, pos, random); }
    @Override public boolean canBeReplaced(BlockState state, BlockPlaceContext context) { return state.getValue(QUANTA) <= 2; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || level.random.nextInt(10) != 0 || !(entity instanceof LivingEntity living) || entity instanceof TaintedMob
                || living.isInvertedHealAndHarm() || living.hasEffect(TCEffects.VIS_EXHAUST.get()) || living.hasEffect(MobEffects.CONFUSION)) return;
        int metadata = state.getValue(QUANTA) - 1;
        if (level.random.nextBoolean()) {
            var effect = new MobEffectInstance(TCEffects.VIS_EXHAUST.get(), 1200, metadata / 3, true, true);
            effect.setCurativeItems(List.of()); living.addEffect(effect);
        } else living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80 + metadata * 20, 0, false, true));
        if (metadata > 0) level.setBlock(pos, state.setValue(QUANTA, metadata), 3);
        else level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
    }
}
