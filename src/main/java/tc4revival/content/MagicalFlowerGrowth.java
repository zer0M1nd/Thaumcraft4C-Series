package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;

public  class MagicalFlowerGrowth {
    private MagicalFlowerGrowth() {}
    public static int scatter(WorldGenLevel level,BlockPos origin,RandomSource random,MagicalPlantBlock.Kind kind) {
        int placed=0;
        for (int attempt=0;attempt<18;attempt++) {
            var pos=origin.offset(random.nextInt(8)-random.nextInt(8),random.nextInt(4)-random.nextInt(4),random.nextInt(8)-random.nextInt(8));
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !level.isEmptyBlock(pos)) continue;
            var state=TCArcane.plant(kind).defaultBlockState();
            if(state.canSurvive(level,pos) && DecorationWrites.set(level,pos,state,3))placed++;
        }
        return placed;
    }
}
