package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Shared old scatter operations for the forest decorator, not modern random-patch placement. */
public  class LegacyForestPlants {
    private LegacyForestPlants() {}
    public static boolean flowers(WorldGenLevel world,BlockPos origin,RandomSource random,BlockState flower) {
        return scatter(world,origin,random,flower,64,true);
    }
    public static boolean grass(WorldGenLevel world,BlockPos origin,RandomSource random,boolean fern) {
        var floor=origin;
        while (floor.getY()>world.getMinBuildHeight()) {
            if (!world.hasChunkAt(floor)) return false;
            var state=world.getBlockState(floor);
            if (!state.isAir() && !state.is(BlockTags.LEAVES)) break;
            floor=floor.below();
        }
        return scatter(world,floor,random,(fern ? Blocks.FERN : Blocks.GRASS).defaultBlockState(),128,false);
    }
    private static boolean scatter(WorldGenLevel world,BlockPos origin,RandomSource random,BlockState plant,int attempts,boolean flower) {
        boolean placed=false;
        for (int attempt=0;attempt<attempts;attempt++) {
            var pos=origin.offset(random.nextInt(8)-random.nextInt(8),random.nextInt(4)-random.nextInt(4),random.nextInt(8)-random.nextInt(8));
            if (world.isOutsideBuildHeight(pos) || !world.hasChunkAt(pos) || !world.isEmptyBlock(pos)) continue;
            if (flower && world.getLevel().dimensionType().hasCeiling() && pos.getY()>=world.getMaxBuildHeight()-1) continue;
            if (!plant.canSurvive(world,pos)) continue;
            placed|=DecorationWrites.set(world,pos,plant,2);
        }
        return placed;
    }
    public static BlockState forestFlower(RandomSource random) {
        return switch(random.nextInt(9)) {
            case 0 -> Blocks.POPPY.defaultBlockState(); case 1 -> Blocks.BLUE_ORCHID.defaultBlockState();
            case 2 -> Blocks.ALLIUM.defaultBlockState(); case 3 -> Blocks.AZURE_BLUET.defaultBlockState();
            case 4 -> Blocks.RED_TULIP.defaultBlockState(); case 5 -> Blocks.ORANGE_TULIP.defaultBlockState();
            case 6 -> Blocks.WHITE_TULIP.defaultBlockState(); case 7 -> Blocks.PINK_TULIP.defaultBlockState();
            default -> Blocks.OXEYE_DAISY.defaultBlockState();
        };
    }
}
