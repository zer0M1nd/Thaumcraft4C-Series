package tbmodern;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.common.IPlantable;
import tc4revival.content.TCArcane;

/** Special flora whose harvesting or growth differs from ordinary crops. */
public final class SpecialPlants {
    private static BlockBehaviour.Properties plant() {
        return BlockBehaviour.Properties.of().noCollission().instabreak().sound(SoundType.GRASS).randomTicks();
    }
    private static boolean soil(BlockState state) { return state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK); }

    public static final class RainbowCactus extends CactusBlock {
        public RainbowCactus() { super(BlockBehaviour.Properties.copy(Blocks.CACTUS)); }
        @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {}
        @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockState adjacent = level.getBlockState(pos.relative(side));
                if (adjacent.isSolid() || adjacent.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) return false;
            }
            BlockState below = level.getBlockState(pos.below());
            return (below.is(this) || below.is(Blocks.SAND) || below.is(Blocks.RED_SAND)
                || below.canSustainPlant(level, pos.below(), Direction.UP, this)) && !level.getBlockState(pos.above()).liquid();
        }
        @Override public boolean canSustainPlant(BlockState state, BlockGetter level, BlockPos pos, Direction side, IPlantable plant) {
            return plant.getPlant(level, pos.above()).is(this) || super.canSustainPlant(state, level, pos, side, plant);
        }
        @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!level.isEmptyBlock(pos.above())) return;
            int height = 1;
            while (height < 3 && level.getBlockState(pos.below(height)).is(this)) height++;
            if (height >= 3) return;
            int age = state.getValue(AGE);
            if (age == 15) {
                level.setBlockAndUpdate(pos.above(), defaultBlockState());
                level.setBlock(pos, state.setValue(AGE, 0), 4);
            } else level.setBlock(pos, state.setValue(AGE, age + 1), 4);
        }
        @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
            BlockPos pos = BlockPos.containing(context.getParameter(LootContextParams.ORIGIN));
            if (!context.getLevel().getBlockState(pos.below()).is(this)) return List.of(new ItemStack(this));
            RandomSource random = context.getLevel().random;
            Item[] dyes = {Items.RED_DYE,Items.GREEN_DYE,Items.PURPLE_DYE,Items.GREEN_DYE,Items.CYAN_DYE,Items.LIGHT_GRAY_DYE,
                Items.GREEN_DYE,Items.GRAY_DYE,Items.PINK_DYE,Items.LIME_DYE,Items.GREEN_DYE,Items.YELLOW_DYE,
                Items.LIGHT_BLUE_DYE,Items.MAGENTA_DYE,Items.ORANGE_DYE,Items.GREEN_DYE};
            List<ItemStack> drops = new ArrayList<>();
            int count = 3 + (int)Math.round(random.nextDouble() * 4.91662);
            for (int i = 0; i < count; i++) drops.add(new ItemStack(dyes[random.nextInt(dyes.length)]));
            return drops;
        }
    }

    public static final class Redlon extends CropBlock {
        public static final IntegerProperty CONNECTION = IntegerProperty.create("connection", 0, 4);
        public Redlon() { super(plant()); registerDefaultState(defaultBlockState().setValue(CONNECTION, 0)); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(CONNECTION); }
        private BlockState connected(BlockState state, LevelReader level, BlockPos pos) {
            int connection = 0;
            if (getAge(state) == 7) {
                Direction[] order = {Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};
                for (int i = 0; i < order.length; i++) if (level.getBlockState(pos.relative(order[i])).is(Blocks.REDSTONE_BLOCK)) { connection = i + 1; break; }
            }
            return state.setValue(CONNECTION, connection);
        }
        @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos other) {
            BlockState updated = super.updateShape(state, side, neighbor, level, pos, other);
            return updated.is(this) ? connected(updated, level, pos) : updated;
        }
        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Block.box(6, 0, 6, 10, 2 + getAge(state) * 2, 10); }
        @Override protected net.minecraft.world.level.ItemLike getBaseSeedId() { return Content.item("redlon_seeds"); }
        @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (level.getRawBrightness(pos, 0) < 9 || random.nextInt((int)(25 / getGrowthSpeed(this, level, pos)) + 1) != 0) return;
            int age = getAge(state);
            if (age < 7) { level.setBlock(pos, connected(getStateForAge(age + 1), level, pos), 2); return; }
            for (Direction side : Direction.Plane.HORIZONTAL) if (level.getBlockState(pos.relative(side)).is(Blocks.REDSTONE_BLOCK)) return;
            BlockPos target = pos.relative(Direction.Plane.HORIZONTAL.getRandomDirection(random));
            if (random.nextInt(4) == 0 && level.isEmptyBlock(target)) level.setBlockAndUpdate(target, Blocks.REDSTONE_BLOCK.defaultBlockState());
        }
        @Override public boolean isRandomlyTicking(BlockState state) { return true; }
        @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
            BlockState grown = getStateForAge(Math.min(7, getAge(state) + 2 + random.nextInt(4)));
            level.setBlock(pos, connected(grown, level, pos), 2);
            if (getAge(grown) == 7) randomTick(grown, level, pos, random);
        }
        @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
            List<ItemStack> drops = new ArrayList<>();
            for (int i = 0; i < 3; i++) if (context.getLevel().random.nextInt(15) <= getAge(state)) drops.add(new ItemStack(Content.item("redlon_seeds")));
            return drops;
        }
    }

    public static final class Aurelia extends BushBlock {
        public static final BooleanProperty OPEN = BooleanProperty.create("open");
        public Aurelia() { super(plant().lightLevel(s -> 7)); registerDefaultState(stateDefinition.any().setValue(OPEN, false)); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(OPEN); }
        @Override protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) { return soil(state); }
        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Block.box(4, 0, 4, 12, 13.6, 12); }
        private boolean refresh(Level level, BlockPos pos, BlockState state) {
            boolean open = level.canSeeSky(pos.above()) && !level.isDay() && level.getMoonPhase() == 0;
            if (state.getValue(OPEN) != open) level.setBlock(pos, state.setValue(OPEN, open), 2);
            return open;
        }
        @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block from, BlockPos other, boolean moving) {
            if (!level.isClientSide && canSurvive(state, level, pos)) refresh(level, pos, state);
        }
        @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!refresh(level, pos, state)) return;
            int dx = random.nextInt(5) - random.nextInt(5), dz = random.nextInt(5) - random.nextInt(5);
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos ground = pos.offset(dx, dy, dz);
                if (soil(level.getBlockState(ground))) {
                    if (level.isEmptyBlock(ground.above())) level.setBlockAndUpdate(ground.above(), Content.block("aurelia_petal_block").defaultBlockState());
                    return;
                }
            }
        }
    }

    public static final class Petal extends Block {
        public Petal() { super(plant().lightLevel(s -> 4)); }
        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Block.box(4, 0, 4, 12, 4, 12); }
        @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder context) { return List.of(new ItemStack(Content.item("aurelia_petal"))); }
    }
    public static final class Flaxium extends BushBlock {
        public Flaxium() { super(plant()); }
        @Override protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) { return soil(state); }
        @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (level.isEmptyBlock(pos.above())) level.setBlockAndUpdate(pos.above(), TCArcane.FLUX_GAS.get().defaultBlockState());
            if (!canSurvive(state, level, pos)) level.removeBlock(pos, false);
        }
    }

    public static final class Briar extends DoublePlantBlock implements BonemealableBlock {
        public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 7);
        public Briar() { super(plant()); registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(AGE, 0)); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(AGE); }
        private BlockPos bottom(BlockState state, BlockPos pos) { return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below(); }
        private boolean pair(LevelReader level, BlockPos pos) {
            BlockState lower = level.getBlockState(pos), upper = level.getBlockState(pos.above());
            return lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER && upper.is(this) && upper.getValue(HALF) == DoubleBlockHalf.UPPER;
        }
        private void age(Level level, BlockPos pos, int age) {
            level.setBlock(pos, level.getBlockState(pos).setValue(AGE, age), 2);
            level.setBlock(pos.above(), level.getBlockState(pos.above()).setValue(AGE, age), 2);
        }
        @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            BlockPos lower = bottom(state, pos);
            if (pair(level, lower) && state.getValue(AGE) < 7 && random.nextInt(4) == 0) age(level, lower, state.getValue(AGE) + 1);
        }
        @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            BlockPos lower = bottom(state, pos);
            if (!pair(level, lower) || level.getBlockState(lower).getValue(AGE) != 7 || level.getBlockState(lower.above()).getValue(AGE) != 7) return InteractionResult.PASS;
            if (!level.isClientSide) { popResource(level, lower, new ItemStack(Content.item("briar_seedbag"), 1 + level.random.nextInt(4))); age(level, lower, 0); }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
            if (state.getValue(HALF) == DoubleBlockHalf.LOWER) return List.of(new ItemStack(this));
            return state.getValue(AGE) == 7 ? List.of(new ItemStack(Content.item("briar_seedbag"), 1 + context.getLevel().random.nextInt(4))) : List.of();
        }
        @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean client) { return state.getValue(AGE) < 7 && pair(level, bottom(state, pos)); }
        @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return level.getRawBrightness(pos, 0) >= 9; }
        @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) { randomTick(state, level, pos, random); }
    }
    private SpecialPlants() {}
}
