package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Forest density sampling with native data-pack-configurable huge mushroom features. */
public  class ForestMushroomFeature extends Feature<NoneFeatureConfiguration> {
    public ForestMushroomFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var world=context.level(); var random=context.random(); var chunk=new ChunkPos(context.origin()); boolean placed=false;
        for (int gx=0;gx<4;gx++) for (int gz=0;gz<4;gz++) {
            int x=chunk.getMinBlockX()+gx*4+9+random.nextInt(3),z=chunk.getMinBlockZ()+gz*4+9+random.nextInt(3);
            int y=LegacySurfaceHeight.at(world,x,z);
            if (random.nextInt(40)==0) placed|=grow(world,new BlockPos(x,y,z),random);
        }
        return placed;
    }
    static boolean grow(WorldGenLevel world,BlockPos base,RandomSource random) {
        // Bound all reads to the region already available to this decoration pass.
        for(var pos:BlockPos.betweenClosed(base.offset(-3,0,-3),base.offset(3,0,3)))
            if(!world.hasChunkAt(pos))return false;
        var key=random.nextBoolean()?net.minecraft.data.worldgen.features.TreeFeatures.HUGE_RED_MUSHROOM:
                net.minecraft.data.worldgen.features.TreeFeatures.HUGE_BROWN_MUSHROOM;
        var feature=world.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE).getHolderOrThrow(key).value();
        return feature.place(world,world.getLevel().getChunkSource().getGenerator(),random,base);
    }
}
