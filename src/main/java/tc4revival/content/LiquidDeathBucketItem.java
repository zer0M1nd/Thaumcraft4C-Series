package tc4revival.content;

public  class LiquidDeathBucketItem extends LegacyFluidBucketItem {
    @Override protected net.minecraft.world.level.material.Fluid fluid() { return TCFluids.DEATH.get(); }
    @Override protected net.minecraft.world.level.block.state.BlockState placedState() {
        return TCArcane.LIQUID_DEATH.get().defaultBlockState().setValue(FiniteFluidBlock.QUANTA,4);
    }
}
