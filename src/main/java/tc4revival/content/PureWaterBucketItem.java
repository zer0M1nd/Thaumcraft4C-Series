package tc4revival.content;

public  class PureWaterBucketItem extends LegacyFluidBucketItem {
    @Override protected net.minecraft.world.level.material.Fluid fluid() { return TCFluids.PURE.get(); }
    @Override protected net.minecraft.world.level.block.state.BlockState placedState() { return TCCleansing.PURE_WATER.get().defaultBlockState(); }
}
