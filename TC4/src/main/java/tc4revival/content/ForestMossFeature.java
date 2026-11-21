package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** The forest's 0-2 small mossy boulders, retaining the old three-lobe random walk. */
public  class ForestMossFeature extends Feature<NoneFeatureConfiguration> {
    public ForestMossFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var world=context.level(); var random=context.random(); var chunk=new ChunkPos(context.origin());
        int count=random.nextInt(3); boolean placed=false;
        for (int i=0;i<count;i++) {
            int x=chunk.getMinBlockX()+random.nextInt(16)+8,z=chunk.getMinBlockZ()+random.nextInt(16)+8;
            placed|=boulder(world,new BlockPos(x,LegacySurfaceHeight.at(world,x,z),z),random);
        }
        return placed;
    }
    static boolean boulder(WorldGenLevel world,BlockPos origin,RandomSource random) {
        return boulder(world,origin,random,Blocks.MOSSY_COBBLESTONE.defaultBlockState());
    }
    static boolean boulder(WorldGenLevel world,BlockPos origin,RandomSource random,net.minecraft.world.level.block.state.BlockState output) {
        return boulder(world,origin,random,output,pos->true);
    }
    static boolean boulder(WorldGenLevel world,BlockPos origin,RandomSource random,net.minecraft.world.level.block.state.BlockState output,
                           java.util.function.Predicate<BlockPos> allowed) {
        var center=origin;
        int bottom=world.getMinBuildHeight()+3;
        while (center.getY()>bottom) {
            if (!world.hasChunkAt(center.below())) return false;
            var below=world.getBlockState(center.below());
            if (below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT) || below.is(Blocks.COARSE_DIRT) || below.is(Blocks.PODZOL) || below.is(Blocks.STONE)) break;
            center=center.below();
        }
        if (center.getY()<=bottom) return false;
        boolean placed=false;
        for (int lobe=0;lobe<3;lobe++) {
            int rx=random.nextInt(2),ry=random.nextInt(2),rz=random.nextInt(2);
            float radius=(rx+ry+rz)*.333f+.5f;
            for (int x=-rx;x<=rx;x++) for (int z=-rz;z<=rz;z++) for (int y=-ry;y<=ry;y++) {
                if (x*x+z*z+y*y>radius*radius) continue;
                var target=center.offset(x,y,z);
                if (!world.isOutsideBuildHeight(target) && world.hasChunkAt(target) && allowed.test(target))
                    placed|=DecorationWrites.set(world,target,output,4);
            }
            int dx=random.nextInt(3)-1,dz=random.nextInt(3)-1,dy=-random.nextInt(2);
            center=center.offset(dx,dy,dz);
        }
        return placed;
    }
}
