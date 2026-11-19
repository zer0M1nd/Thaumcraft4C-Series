package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** The decorator's three sand disks, one clay disk, then one gravel disk. */
public  class ForestSedimentsFeature extends Feature<NoneFeatureConfiguration> {
    public ForestSedimentsFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var world=context.level(); var random=context.random(); var chunk=new ChunkPos(context.origin());
        boolean placed=false;
        for (int attempt=0;attempt<5;attempt++) {
            int x=chunk.getMinBlockX()+random.nextInt(16)+8;
            int z=chunk.getMinBlockZ()+random.nextInt(16)+8;
            var center=bottom(world,x,z);
            if (center!=null) placed|=disk(world,random,center,attempt==3 ? Blocks.CLAY.defaultBlockState()
                    : attempt==4 ? Blocks.GRAVEL.defaultBlockState() : Blocks.SAND.defaultBlockState());
        }
        return placed;
    }

    // Old getTopSolidOrLiquidBlock actually skips liquids and leaves. WORLD_SURFACE alone
    // would select air above the water and reject every submerged sediment candidate.
    @SuppressWarnings("deprecation")
    static BlockPos bottom(WorldGenLevel world,int x,int z) {
        var cursor=new BlockPos.MutableBlockPos(x,Math.min(world.getMaxBuildHeight()-1,
                LegacySurfaceHeight.nonAirTop(world,x,z)),z);
        if (!world.hasChunkAt(cursor)) return null;
        for (;cursor.getY()>world.getMinBuildHeight();cursor.move(0,-1,0)) {
            var state=world.getBlockState(cursor);
            if (state.blocksMotion() && !state.is(BlockTags.LEAVES)) return cursor.above();
        }
        return null;
    }

    static boolean disk(WorldGenLevel world,RandomSource random,BlockPos center,BlockState output) {
        if (world.isOutsideBuildHeight(center) || !world.getBlockState(center).is(Blocks.WATER)) return false;
        boolean clay=output.is(Blocks.CLAY);
        int radius=random.nextInt(clay ? 2 : output.is(Blocks.SAND) ? 5 : 4)+2;
        int thickness=clay ? 1 : 2;
        if (!world.hasChunksAt(center.offset(-radius,-thickness,-radius),center.offset(radius,thickness,radius))) return false;
        boolean placed=false;
        for (int dx=-radius;dx<=radius;dx++) for (int dz=-radius;dz<=radius;dz++) {
            if (dx*dx+dz*dz>radius*radius) continue;
            for (int dy=-thickness;dy<=thickness;dy++) {
                var pos=center.offset(dx,dy,dz);
                if (!world.isOutsideBuildHeight(pos) && replaces(world.getBlockState(pos),clay))
                    placed|=DecorationWrites.set(world,pos,output,2);
            }
        }
        return placed;
    }

    static boolean replaces(BlockState state,boolean clay) {
        return state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL)
                || (clay ? state.is(Blocks.CLAY) : state.is(Blocks.GRASS_BLOCK));
    }
}
