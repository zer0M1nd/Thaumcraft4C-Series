package wtmodern;

import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Regressions for behaviors checked against the original 1.0-24 event handlers. */
@GameTestHolder("warptheory")
@PrefixGameTestTemplate(false)
public class WarpParityServerTests {
    private static void pass(GameTestHelper helper) {
        LogUtils.getLogger().info("WARPTHEORY_TEST_PASS {}", Thread.currentThread().getStackTrace()[2].getMethodName());
        helper.succeed();
    }

    /** Forces only the event's decisions; unexpected additional random rolls fail the test. */
    public static class Decisions extends LegacyRandomSource {
        private int[] numbers;
        private boolean[] booleans;
        private int numberIndex;
        private int booleanIndex;

        public Decisions(int[] numbers, boolean... booleans) {
            super(1);
            this.numbers = numbers;
            this.booleans = booleans;
        }

        @Override
        public int nextInt(int bound) {
            if (numberIndex >= numbers.length) throw new IllegalStateException("Unexpected integer decision");
            int result = numbers[numberIndex++];
            if (result < 0 || result >= bound) throw new IllegalStateException("Invalid scripted decision for bound " + bound);
            return result;
        }

        @Override
        public boolean nextBoolean() {
            if (booleanIndex >= booleans.length) throw new IllegalStateException("Unexpected boolean decision");
            return booleans[booleanIndex++];
        }
    }

    @GameTest(template = "empty")
    public static void decayIncludesNonBushPlantables(GameTestHelper helper) {
        // Cactus and reeds implement IPlantable without inheriting BushBlock.
        for (var block : List.of(Blocks.CACTUS, Blocks.SUGAR_CANE, Blocks.WHEAT, Blocks.OAK_SAPLING)) {
            var result = WarpEvents.decayState(block.defaultBlockState());
            helper.assertTrue(result != null && result.isAir(), "Plant must decay to air: " + block);
        }
        helper.assertTrue(WarpEvents.decayState(Blocks.OAK_LEAVES.defaultBlockState()).isAir(), "Leaves still decay");
        helper.assertTrue(WarpEvents.decayState(Blocks.OAK_LOG.defaultBlockState()).is(Blocks.DIRT), "Logs retain the explicit decay mapping");
        helper.assertTrue(WarpEvents.decayState(Blocks.CHEST.defaultBlockState()) == null, "Unmapped blocks remain intact");
        pass(helper);
    }

    @GameTest(template = "empty")
    public static void livestockUsesOriginalThreeChoices(GameTestHelper helper) {
        var random = new Decisions(new int[] {0, 1, 2}) {
            @Override
            public int nextInt(int bound) {
                helper.assertTrue(bound == 3, "Original livestock roll has three outcomes, not four");
                return super.nextInt(bound);
            }
        };
        helper.assertTrue(WarpEvents.livestockType(random) == EntityType.COW, "First animal is cow");
        helper.assertTrue(WarpEvents.livestockType(random) == EntityType.PIG, "Second animal is pig");
        helper.assertTrue(WarpEvents.livestockType(random) == EntityType.SHEEP, "Third animal is sheep");
        pass(helper);
    }

    @GameTest(template = "empty", batch = "warp_parity_world")
    public static void chestSearchUsesEightBlockSphere(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(2, 35, 2));
        var axis = center.offset(8, 0, 0);
        var corner = center.offset(8, 0, 8);
        try {
            level.setBlockAndUpdate(axis, Blocks.CHEST.defaultBlockState());
            level.setBlockAndUpdate(corner, Blocks.CHEST.defaultBlockState());
            var found = WarpEvents.nearbyChests(level, center);
            helper.assertTrue(found.stream().anyMatch(chest -> chest.getBlockPos().equals(axis)), "Chest exactly eight blocks away is included");
            helper.assertTrue(found.stream().noneMatch(chest -> chest.getBlockPos().equals(corner)), "Cube corner outside the radius is excluded");
        } finally {
            level.removeBlock(axis, false);
            level.removeBlock(corner, false);
        }
        pass(helper);
    }

    @GameTest(template = "empty")
    public static void chestShuffleRetriesAnEmptyPairExactlyOnce(GameTestHelper helper) {
        var first = new SimpleContainer(2);
        var second = new SimpleContainer(2);
        first.setItem(1, new ItemStack(Items.DIAMOND, 17));
        second.setItem(1, new ItemStack(Items.GOLD_INGOT, 5));
        var decisions = new Decisions(new int[] {0, 0, 1, 1});
        WarpEvents.shuffleChests(decisions, first, second);
        helper.assertTrue(first.getItem(1).is(Items.GOLD_INGOT) && first.getItem(1).getCount() == 5, "Retry exchanges the second chosen slots");
        helper.assertTrue(second.getItem(1).is(Items.DIAMOND) && second.getItem(1).getCount() == 17, "Retry preserves the entire original stack");
        helper.assertTrue(decisions.numberIndex == 4, "Failed first choice makes exactly one retry");
        WarpEvents.shuffleChests(new Decisions(new int[] {1, 1}), first, second);
        helper.assertTrue(first.getItem(1).is(Items.DIAMOND), "Successful first exchange does not retry and undo itself");
        pass(helper);
    }

    @GameTest(template = "empty")
    public static void chestShuffleHonorsBothDestinationSlotRules(GameTestHelper helper) {
        var first = new SimpleContainer(1) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) { return !stack.is(Items.GOLD_INGOT); }
        };
        var second = new SimpleContainer(1);
        first.setItem(0, new ItemStack(Items.DIAMOND, 7));
        second.setItem(0, new ItemStack(Items.GOLD_INGOT, 9));
        for (boolean reverse : new boolean[] {false, true}) {
            WarpEvents.shuffleChests(new Decisions(new int[] {0, 0, 0, 0}), reverse ? second : first, reverse ? first : second);
            helper.assertTrue(first.getItem(0).is(Items.DIAMOND) && first.getItem(0).getCount() == 7, "Rejected destination leaves the first stack unchanged");
            helper.assertTrue(second.getItem(0).is(Items.GOLD_INGOT) && second.getItem(0).getCount() == 9, "Rejected destination leaves the other stack unchanged");
        }
        pass(helper);
    }

    @GameTest(template = "empty")
    public static void swampCanReplacePlantsButRetainsExistingSaplings(GameTestHelper helper) {
        var level = helper.getLevel();
        var ground = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(ground, Blocks.DIRT.defaultBlockState());
        level.setBlockAndUpdate(ground.above(), Blocks.DANDELION.defaultBlockState());
        helper.assertTrue(WarpEvents.swampAt(level, ground, new Decisions(new int[] {0}, true, true)), "Soil growth consumes an event step");
        helper.assertTrue(level.getBlockState(ground.above()).is(Blocks.OAK_SAPLING), "Existing small plant is replaced by a sapling");
        level.setBlockAndUpdate(ground.above(), Blocks.SPRUCE_SAPLING.defaultBlockState());
        WarpEvents.swampAt(level, ground, new Decisions(new int[0], true, true));
        helper.assertTrue(level.getBlockState(ground.above()).is(Blocks.SPRUCE_SAPLING), "Existing sapling is not replaced or rerolled");
        level.setBlockAndUpdate(ground.above(), Blocks.STONE.defaultBlockState());
        helper.assertTrue(WarpEvents.swampAt(level, ground, new Decisions(new int[0], true, true)), "Original consumes a growth branch even if obstructed");
        helper.assertTrue(level.getBlockState(ground.above()).is(Blocks.STONE), "Growth does not overwrite solid obstacles");
        pass(helper);
    }

    @GameTest(template = "empty")
    public static void swampPodzolBranchRequiresGrassAndEmptySpace(GameTestHelper helper) {
        var level = helper.getLevel();
        var ground = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(ground.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(ground, Blocks.GRASS_BLOCK.defaultBlockState());
        WarpEvents.swampAt(level, ground, new Decisions(new int[0], true, false, false));
        helper.assertTrue(level.getBlockState(ground).is(Blocks.PODZOL), "Original dirt metadata 2 becomes modern podzol");
        level.setBlockAndUpdate(ground, Blocks.DIRT.defaultBlockState());
        WarpEvents.swampAt(level, ground, new Decisions(new int[0], true, false, false));
        helper.assertTrue(level.getBlockState(ground).is(Blocks.DIRT), "Ordinary dirt is not turned to podzol by the grass-only branch");
        level.setBlockAndUpdate(ground, Blocks.GRASS_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(ground.above(), Blocks.DANDELION.defaultBlockState());
        WarpEvents.swampAt(level, ground, new Decisions(new int[0], true, false, false));
        helper.assertTrue(level.getBlockState(ground).is(Blocks.GRASS_BLOCK), "Plant above blocks the podzol branch");
        pass(helper);
    }

    @GameTest(template = "empty")
    public static void swampVinesUseSixRandomAttemptsWithoutFallback(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(2, 2, 2));
        for (var side : Direction.Plane.HORIZONTAL) level.setBlockAndUpdate(origin.relative(side), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(origin, Blocks.OAK_LOG.defaultBlockState());
        level.setBlockAndUpdate(origin.west(), Blocks.STONE.defaultBlockState());
        // East would succeed, but the author samples north six times here. It must not
        // deterministically try every side afterward or treat the log itself as support.
        var random = new Decisions(new int[] {0, 0, 0, 0, 0, 0});
        helper.assertTrue(!WarpEvents.swampAt(level, origin, random), "Six failed random directions leave the event pending");
        helper.assertTrue(random.numberIndex == 6, "Exactly six directions are rolled");
        helper.assertTrue(level.isEmptyBlock(origin.east()), "Unselected valid side stays empty");
        WarpEvents.swampAt(level, origin, new Decisions(new int[] {3}));
        helper.assertTrue(level.getBlockState(origin.east()).is(Blocks.VINE), "A selected valid direction grows a vine");
        helper.assertTrue(level.getBlockState(origin.east()).getValue(VineBlock.WEST), "Vine attachment follows the original opposite-side metadata");
        pass(helper);
    }

    @GameTest(template = "empty")
    public static void swampVinesRetainOriginalMutableCoordinate(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(2, 2, 3));
        level.setBlockAndUpdate(origin.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(origin, Blocks.OAK_LOG.defaultBlockState());
        level.setBlockAndUpdate(origin.south(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(origin.north(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(origin.north(2), Blocks.AIR.defaultBlockState());
        helper.assertTrue(WarpEvents.swampAt(level, origin, new Decisions(new int[] {0, 0})), "Second north attempt grows after the occupied first destination");
        helper.assertTrue(level.getBlockState(origin.north()).is(Blocks.STONE), "Occupied first destination is not replaced");
        var vine = level.getBlockState(origin.north(2));
        helper.assertTrue(vine.is(Blocks.VINE) && vine.getValue(VineBlock.SOUTH), "Second attempt is relative to the mutated first destination");
        pass(helper);
    }

    @GameTest(template = "empty")
    public static void swampWaterUsesOnlyExplicitDecayMappings(GameTestHelper helper) {
        var level = helper.getLevel();
        var target = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(target.above(), Blocks.AIR.defaultBlockState());
        for (var side : Direction.values()) if (side != Direction.UP) level.setBlockAndUpdate(target.relative(side), Blocks.STONE.defaultBlockState());
        for (var block : List.of(Blocks.CACTUS, Blocks.SUGAR_CANE, Blocks.DANDELION, Blocks.OAK_LEAVES)) {
            var state = block.defaultBlockState();
            level.setBlock(target, state, 2);
            helper.assertTrue(WarpEvents.decayState(state).isAir(), "Plant/leaf is still eligible for ordinary decay");
            helper.assertTrue(WarpEvents.decayMappingState(state) == null, "Plant/leaf is not an explicit terrain mapping");
            helper.assertTrue(!WarpEvents.canTurnToSwampWater(level, target), "Plant/leaf does not become a water-pool target");
        }
        for (var block : List.of(Blocks.STONE, Blocks.DIRT, Blocks.SAND, Blocks.OAK_LOG)) {
            level.setBlock(target, block.defaultBlockState(), 2);
            helper.assertTrue(WarpEvents.canTurnToSwampWater(level, target), "Explicit mapped terrain remains eligible: " + block);
        }
        level.setBlock(target, Blocks.AIR.defaultBlockState(), 2);
        helper.assertTrue(WarpEvents.canTurnToSwampWater(level, target), "Contained air also remains eligible");
        pass(helper);
    }

    @GameTest(template = "empty", batch = "warp_parity_world")
    public static void swampGrowsStageZeroSaplingDirectly(GameTestHelper helper) {
        var level = helper.getLevel();
        // Above the ordinary fixtures so the generated canopy cannot overwrite another test.
        var sapling = helper.absolutePos(new BlockPos(2, 50, 2));
        var min = sapling.offset(-5, -1, -5);
        var max = sapling.offset(5, 16, 5);
        try {
            for (var pos : BlockPos.betweenClosed(min, max)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            level.setBlockAndUpdate(sapling.below(), Blocks.DIRT.defaultBlockState());
            level.setBlockAndUpdate(sapling, Blocks.OAK_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, 0));
            helper.assertTrue(WarpEvents.swampAt(level, sapling, RandomSource.create(1)), "Sapling growth branch consumes a step");
            helper.assertTrue(level.getBlockState(sapling).is(BlockTags.LOGS), "Tree grows immediately instead of only advancing the sapling stage");
        } finally {
            for (var pos : BlockPos.betweenClosed(min, max)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        pass(helper);
    }

    @GameTest(template = "empty")
    public static void rainPreservesExistingWeatherCountdowns(GameTestHelper helper) {
        var weather = (ServerLevelData) helper.getLevel().getLevelData();
        boolean raining = weather.isRaining(), thundering = weather.isThundering();
        int rainTime = weather.getRainTime(), thunderTime = weather.getThunderTime(), clearTime = weather.getClearWeatherTime();
        try {
            weather.setRaining(false);
            weather.setThundering(false);
            weather.setRainTime(241);
            weather.setThunderTime(517);
            weather.setClearWeatherTime(37);
            helper.assertTrue(WarpEvents.startRain(helper.getLevel()), "Rain starts when not already thundering");
            helper.assertTrue(weather.isRaining() && weather.isThundering(), "Both original weather flags are set");
            helper.assertTrue(weather.getRainTime() == 241 && weather.getThunderTime() == 517 && weather.getClearWeatherTime() == 37, "Existing weather countdowns are retained");
            helper.assertTrue(!WarpEvents.startRain(helper.getLevel()), "Already-thundering event is rejected without resetting its duration");
        } finally {
            weather.setRaining(raining);
            weather.setThundering(thundering);
            weather.setRainTime(rainTime);
            weather.setThunderTime(thunderTime);
            weather.setClearWeatherTime(clearTime);
        }
        pass(helper);
    }
}
