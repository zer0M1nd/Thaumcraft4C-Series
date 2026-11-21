package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public  class ForestReedsFeature extends Feature<NoneFeatureConfiguration> {
    public ForestReedsFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var world=context.level(); var random=context.random(); var chunk=new ChunkPos(context.origin()); boolean placed=false;
        for (int i=0;i<10;i++) {
            int x=chunk.getMinBlockX()+8+random.nextInt(16),z=chunk.getMinBlockZ()+8+random.nextInt(16);
            int surface=LegacySurfaceHeight.at(world,x,z),floor=surface>0 ? 0 : world.getMinBuildHeight();
            int y=floor+random.nextInt(Math.max(1,(surface-floor)*2));
            placed|=patch(world,new BlockPos(x,y,z),random);
        }
        return placed;
    }
    static boolean patch(WorldGenLevel world,BlockPos origin,RandomSource random) {
        boolean placed=false;
        for (int attempt=0;attempt<20;attempt++) {
            var pos=origin.offset(random.nextInt(4)-random.nextInt(4),0,random.nextInt(4)-random.nextInt(4));
            if (world.isOutsideBuildHeight(pos) || !world.hasChunkAt(pos) || !world.isEmptyBlock(pos) || !besideWater(world,pos.below())) continue;
            int height=2+random.nextInt(random.nextInt(3)+1);
            for (int y=0;y<height;y++) {
                var target=pos.above(y);
                var cane=Blocks.SUGAR_CANE.defaultBlockState();
                if(world.isOutsideBuildHeight(target) || !world.hasChunkAt(target) || !world.isEmptyBlock(target) || !cane.canSurvive(world,target))break;
                placed|=DecorationWrites.set(world,target,cane,2);
            }
        }
        return placed;
    }
    private static boolean besideWater(WorldGenLevel world,BlockPos soil) {
        for (var side:new Direction[]{Direction.WEST,Direction.EAST,Direction.NORTH,Direction.SOUTH}) {
            var pos=soil.relative(side);
            if (world.hasChunkAt(pos) && world.getBlockState(pos).is(Blocks.WATER)) return true;
        }
        return false;
    }
}
