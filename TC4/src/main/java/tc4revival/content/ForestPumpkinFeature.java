package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Old pumpkins already had faces: use carved pumpkins with the old horizontal metadata order. */
public  class ForestPumpkinFeature extends Feature<NoneFeatureConfiguration> {
    public ForestPumpkinFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var random=context.random(); if (random.nextInt(32)!=0) return false;
        var world=context.level(); var chunk=new ChunkPos(context.origin());
        int x=chunk.getMinBlockX()+8+random.nextInt(16),z=chunk.getMinBlockZ()+8+random.nextInt(16);
        int surface=LegacySurfaceHeight.at(world,x,z),floor=surface>0 ? 0 : world.getMinBuildHeight();
        var origin=new BlockPos(x,floor+random.nextInt(Math.max(1,(surface-floor)*2)),z); boolean placed=false;
        for (int attempt=0;attempt<64;attempt++) {
            var pos=origin.offset(random.nextInt(8)-random.nextInt(8),random.nextInt(4)-random.nextInt(4),random.nextInt(8)-random.nextInt(8));
            if (world.isOutsideBuildHeight(pos) || !world.hasChunkAt(pos) || !world.isEmptyBlock(pos) || !world.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) continue;
            var facing=switch(random.nextInt(4)) { case 0 -> Direction.SOUTH; case 1 -> Direction.WEST; case 2 -> Direction.NORTH; default -> Direction.EAST; };
            placed|=DecorationWrites.set(world,pos,Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING,facing),2);
        }
        return placed;
    }
}
