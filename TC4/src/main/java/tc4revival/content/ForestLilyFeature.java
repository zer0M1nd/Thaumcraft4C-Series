package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Six old waterlily patches, each trying ten triangular-distribution offsets. */
public  class ForestLilyFeature extends Feature<NoneFeatureConfiguration> {
    public ForestLilyFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var world=context.level(); var random=context.random(); var chunk=new ChunkPos(context.origin()); boolean placed=false;
        for (int patch=0;patch<6;patch++) {
            int x=chunk.getMinBlockX()+8+random.nextInt(16),z=chunk.getMinBlockZ()+8+random.nextInt(16);
            int surface=LegacySurfaceHeight.at(world,x,z),floor=surface>0 ? 0 : world.getMinBuildHeight();
            var origin=new BlockPos(x,floor+random.nextInt(Math.max(1,(surface-floor)*2)),z);
            while (origin.getY()>floor && world.hasChunkAt(origin.below()) && world.isEmptyBlock(origin.below())) origin=origin.below();
            for (int attempt=0;attempt<10;attempt++) {
                var pos=origin.offset(random.nextInt(8)-random.nextInt(8),random.nextInt(4)-random.nextInt(4),random.nextInt(8)-random.nextInt(8));
                if (world.isOutsideBuildHeight(pos) || !world.hasChunkAt(pos) || !world.isEmptyBlock(pos)) continue;
                // Modern water joins the old still/flowing block IDs. Do not admit ice or waterlogged blocks.
                if (world.getBlockState(pos.below()).is(Blocks.WATER) && world.getFluidState(pos.below()).isSource())
                    placed|=DecorationWrites.set(world,pos,Blocks.LILY_PAD.defaultBlockState(),2);
            }
        }
        return placed;
    }
}
