package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** The magical forest's final decoration pass, after ordinary biome plants and trees. */
public  class ForestUnderstoryFeature extends Feature<NoneFeatureConfiguration> {
    public ForestUnderstoryFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var world=context.level(); var random=context.random(); var chunk=new ChunkPos(context.origin());
        boolean placed=false;
        for (int attempt=0;attempt<10;attempt++) {
            var pos=new BlockPos(chunk.getMinBlockX()+random.nextInt(16)+8,64,chunk.getMinBlockZ()+random.nextInt(16)+8);
            placed|=TCWorldFeatures.MANA_POD.get().place(new FeaturePlaceContext<>(java.util.Optional.empty(),world,context.chunkGenerator(),random,pos,NoneFeatureConfiguration.INSTANCE));
        }
        for (int attempt=0;attempt<8;attempt++) {
            int x=chunk.getMinBlockX()+random.nextInt(16),z=chunk.getMinBlockZ()+random.nextInt(16);
            var pos=new BlockPos(x,LegacySurfaceHeight.at(world,x,z),z);
            if (!world.hasChunkAt(pos)) continue;
            while (pos.getY()>50 && !world.getBlockState(pos).is(Blocks.GRASS_BLOCK)) pos=pos.below();
            if (!world.getBlockState(pos).is(Blocks.GRASS_BLOCK)) continue;
            var above=pos.above();
            if (!world.isOutsideBuildHeight(above) && world.getBlockState(above).canBeReplaced() && nearWood(world,above))
                placed|=DecorationWrites.set(world,above,TCArcane.plant(MagicalPlantBlock.Kind.VISHROOM).defaultBlockState(),2);
        }
        return placed;
    }
    private static boolean nearWood(WorldGenLevel world,BlockPos pos) {
        for (int x=-1;x<=1;x++) for (int y=-1;y<=1;y++) for (int z=-1;z<=1;z++) {
            if (x==0 && y==0 && z==0) continue;
            var target=pos.offset(x,y,z); if (!world.hasChunkAt(target)) continue;
            if (world.getBlockState(target).is(net.minecraft.tags.BlockTags.LOGS)) return true;
        }
        return false;
    }
}
