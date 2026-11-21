package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Finite quanta with legacy remainder distribution and density-driven vertical exchange. */
public abstract class FiniteFluidBlock extends Block {
    // Crust dissolution writes old metadata 8, temporarily representing nine quanta.
    public static final IntegerProperty QUANTA = IntegerProperty.create("quanta", 1, 9);
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    protected final int density, delay, capacity;
    protected final Direction flowDirection;
    protected FiniteFluidBlock(Properties properties, int density, int viscosity) {
        this(properties,density,viscosity,8);
    }
    protected FiniteFluidBlock(Properties properties,int density,int viscosity,int capacity) {
        super(properties); this.capacity=capacity; this.density = density; delay = Math.max(1, viscosity / 200);
        flowDirection = density > 0 ? Direction.DOWN : Direction.UP;
        registerDefaultState(stateDefinition.any().setValue(QUANTA, 1));
    }
    public  int capacity() { return capacity; }
    public boolean liquidMaterial() { return false; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(QUANTA); }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        if (!previous.is(this)) level.scheduleTick(pos, this, delay);
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos changed, boolean moving) {
        level.scheduleTick(pos, this, delay);
    }
    private int quantity(ServerLevel level, BlockPos pos, int below) {
        if (!level.hasChunkAt(pos)) return -1;
        var state = level.getBlockState(pos); int quantity = state.isAir() ? 0 : state.is(this) ? state.getValue(QUANTA) : -1;
        return quantity >= below ? -1 : quantity;
    }
    public int quantity(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.is(this) ? state.getValue(QUANTA) : state.isAir() ? 0 : -1;
    }
    public net.minecraft.world.phys.Vec3 flowVector(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        var flow = net.minecraft.world.phys.Vec3.ZERO;
        int decay = capacity - quantity(level, pos);
        for (var side : new Direction[]{Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH}) {
            var neighbor = pos.relative(side);
            int neighborDecay = capacity - quantity(level, neighbor), strength;
            if (neighborDecay >= capacity) {
                var adjacent = level.getBlockState(neighbor);
                if (adjacent.blocksMotion() || adjacent.getBlock() instanceof FiniteFluidBlock) continue;
                // The legacy base queried below even for gas. This affects surface texture direction.
                neighborDecay = capacity - quantity(level, neighbor.below());
                strength = neighborDecay - (decay - capacity);
            } else strength = neighborDecay - decay;
            if (neighborDecay >= 0) flow = flow.add(side.getStepX() * strength, 0, side.getStepZ() * strength);
        }
        if (level.getBlockState(pos.above()).is(this)) {
            for (var side : SIDES) {
                var neighbor = pos.relative(side);
                if (solidMaterial(level.getBlockState(neighbor)) || solidMaterial(level.getBlockState(neighbor.above()))) {
                    flow = flow.normalize().add(0, -6, 0); break;
                }
            }
        }
        return flow.normalize();
    }
    public static boolean solidMaterial(BlockState state) {
        return state.blocksMotion() && !(state.getBlock() instanceof FiniteFluidBlock)
                && !(state.getBlock() instanceof TaintFibreBlock) && !(state.getBlock() instanceof TaintSolidBlock);
    }
    protected boolean displace(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos)) return false;
        var state = level.getBlockState(pos);
        if (state.isAir()) return true;
        if (state.getBlock() instanceof FiniteFluidBlock || state.getBlock() instanceof TaintFibreBlock || state.hasBlockEntity() || level.getBlockEntity(pos) != null || state.is(BlockTags.DOORS)
                || state.is(BlockTags.SIGNS) || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL) || state.blocksMotion()) return false;
        int otherDensity = density(state);
        if (otherDensity != Integer.MAX_VALUE) return density > otherDensity;
        Block.dropResources(state, level, pos); return true;
    }
    private static int density(BlockState state) {
        if (state.getBlock() instanceof FiniteFluidBlock finite) return finite.density;
        if (!state.getFluidState().isEmpty()) return state.getFluidState().getFluidType().getDensity();
        return Integer.MAX_VALUE;
    }
    private void put(ServerLevel level, BlockPos pos, int quantity, int flags) {
        level.setBlock(pos, quantity == 0 ? Blocks.AIR.defaultBlockState() : defaultBlockState().setValue(QUANTA, quantity), flags);
        if (quantity > 0) level.scheduleTick(pos, this, delay);
    }
    private void retain(ServerLevel level, BlockPos pos, int quantity) {
        level.setBlock(pos, defaultBlockState().setValue(QUANTA, quantity), 2);
    }
    private int vertical(ServerLevel level, BlockPos pos, int amount) {
        var destination = pos.relative(flowDirection);
        if (level.isOutsideBuildHeight(destination)) { level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); return 0; }
        if (!level.hasChunkAt(destination)) return amount;
        int below = quantity(level, destination, capacity);
        if (below >= 0) {
            int total = below + amount; put(level, destination, Math.min(total, capacity), 3);
            if (total <= capacity) { level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); return 0; }
            return total - capacity;
        }
        var displaced = level.getBlockState(destination); int otherDensity = density(displaced);
        if (otherDensity == Integer.MAX_VALUE) {
            if (displace(level, destination)) { put(level, destination, amount, 3); level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); return 0; }
        } else if ((flowDirection == Direction.DOWN ? otherDensity < density : otherDensity > density)
                && !displaced.hasBlockEntity() && level.getBlockEntity(destination) == null) {
            // The old cast to BlockFluidBase crashed for some fluids; preserve the displaced state directly.
            put(level, destination, amount, 3); level.setBlock(pos, displaced, 3);
            if (displaced.getBlock() instanceof FiniteFluidBlock finite) level.scheduleTick(pos, finite, finite.delay);
            else if (!displaced.getFluidState().isEmpty()) level.scheduleTick(pos, displaced.getFluidState().getType(), displaced.getFluidState().getType().getTickDelay(level));
            return 0;
        }
        return amount;
    }
    protected  void flow(ServerLevel level, BlockPos pos, RandomSource random) {
        var original = level.getBlockState(pos); if (!original.is(this)) return;
        int previous = original.getValue(QUANTA), amount = vertical(level, pos, previous);
        if (amount == 0 || !level.getBlockState(pos).is(this)) return;
        if (amount == 1) { if (amount != previous) retain(level, pos, amount); return; }
        for (var side : SIDES) {
            var neighbor = pos.relative(side);
            if (displace(level, neighbor)) level.setBlock(neighbor, Blocks.AIR.defaultBlockState(), 3);
        }
        int[] quantities = new int[4]; int total = amount, cells = 1;
        for (int i = 0; i < SIDES.length; i++) {
            quantities[i] = quantity(level, pos.relative(SIDES[i]), amount - 1);
            if (quantities[i] >= 0) { total += quantities[i]; cells++; }
        }
        if (cells == 1) { if (amount != previous) retain(level, pos, amount); return; }
        int average = total / cells, remaining = total % cells;
        for (int i = 0; i < SIDES.length; i++) {
            if (quantities[i] < 0) continue;
            int assigned = average;
            if (remaining == cells || remaining > 1 && random.nextInt(cells - remaining) != 0) { assigned++; remaining--; }
            if (assigned != quantities[i]) put(level, pos.relative(SIDES[i]), assigned, assigned == 0 ? 3 : 2);
            cells--;
        }
        if (level.getBlockState(pos).is(this)) retain(level, pos, average + (remaining > 0 ? 1 : 0));
    }
}
