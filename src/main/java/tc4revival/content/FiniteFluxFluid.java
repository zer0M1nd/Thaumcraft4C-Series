package tc4revival.content;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidType;

/** A view of the block's finite volume. Only block ticks may move or consume that volume. */
public  class FiniteFluxFluid extends Fluid {
    private final Supplier<FluidType> type;
    private final Supplier<? extends FiniteFluidBlock> block;
    FiniteFluxFluid(Supplier<FluidType> type, Supplier<? extends FiniteFluidBlock> block) {
        this.type = type; this.block = block;
        registerDefaultState(stateDefinition.any().setValue(FiniteFluidBlock.QUANTA, 1));
    }
    @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) { builder.add(FiniteFluidBlock.QUANTA); }
    @Override public FluidType getFluidType() { return type.get(); }
    @Override public Item getBucket() { return block.get() instanceof LiquidDeathBlock ? TCItems.LIQUID_DEATH_BUCKET.get() : Items.AIR; }
    @Override public int getTickDelay(LevelReader level) { return Math.max(1, getFluidType().getViscosity() / 200); }
    @Override protected float getExplosionResistance() { return 0; }
    @Override public int getAmount(FluidState state) { return state.getValue(FiniteFluidBlock.QUANTA); }
    @Override public boolean isSource(FluidState state) { return getAmount(state) == block.get().capacity(); }
    @Override protected BlockState createLegacyBlock(FluidState state) { return block.get().defaultBlockState().setValue(FiniteFluidBlock.QUANTA, getAmount(state)); }
    @Override protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid other, Direction direction) {
        return other != this && other.getFluidType().getDensity() > getFluidType().getDensity();
    }
    @Override public float getOwnHeight(FluidState state) { return getAmount(state) / (float)block.get().capacity(); }
    @Override public float getHeight(FluidState state, BlockGetter level, BlockPos pos) { return getOwnHeight(state); }
    @Override public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
        float height = getOwnHeight(state);
        return getFluidType().isLighterThanAir() ? Shapes.box(0, 1 - height, 0, 1, 1, 1) : Shapes.box(0, 0, 0, 1, height, 1);
    }
    @Override protected Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) { return block.get().flowVector(level, pos); }
}
