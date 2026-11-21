package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.*;

public  class LiquidDeathBlock extends FiniteFluidBlock implements BucketPickup {
    public LiquidDeathBlock() {
        super(Properties.of().noCollission().noOcclusion().noLootTable().replaceable().randomTicks()
                .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)
                .lightLevel(state->Math.min(15,(state.getValue(QUANTA)-1)*8/4)),1000,1500,4);
    }
    @Override public boolean liquidMaterial() { return true; }
    @Override public FluidState getFluidState(BlockState state) { return TCFluids.DEATH.get().defaultFluidState().setValue(QUANTA,state.getValue(QUANTA)); }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Shapes.empty(); }
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) { flow(level,pos,random); }
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) { flow(level,pos,random); }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living) living.hurt(LiquidDeathDrops.source(level),state.getValue(QUANTA));
    }
    @Override public ItemStack pickupBlock(LevelAccessor level,BlockPos pos,BlockState state) {
        if (!state.is(this) || state.getValue(QUANTA)!=capacity() || !level.getBlockState(pos).equals(state)) return ItemStack.EMPTY;
        if (!level.setBlock(pos,Blocks.AIR.defaultBlockState(),3)) return ItemStack.EMPTY;
        return new ItemStack(TCItems.LIQUID_DEATH_BUCKET.get());
    }
    @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getPickupSound() { return java.util.Optional.empty(); }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) { tc4revival.client.AlchemyBubbleParticles.liquidDeath(level,pos,state.getValue(QUANTA)-1,random); }
}
