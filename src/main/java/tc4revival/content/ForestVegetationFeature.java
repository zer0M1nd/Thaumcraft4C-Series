package tc4revival.content;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Consecutive sediment, vegetation and spring stages, following ore decoration. */
public  class ForestVegetationFeature extends Feature<NoneFeatureConfiguration> {
    public ForestVegetationFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var random=context.random(); var world=context.level(); var chunk=new ChunkPos(context.origin()); boolean placed=false;
        placed|=TCWorldFeatures.FOREST_SEDIMENTS.get().place(context);
        int trees=2+(random.nextInt(10)==0 ? 1 : 0);
        for (int attempt=0;attempt<trees;attempt++) {
            int x=chunk.getMinBlockX()+random.nextInt(16)+8,z=chunk.getMinBlockZ()+random.nextInt(16)+8;
            var pos=new BlockPos(x,LegacySurfaceHeight.at(world,x,z),z);
            placed|=TCWorldFeatures.FOREST_TREE.get().place(new FeaturePlaceContext<>(Optional.empty(),world,context.chunkGenerator(),random,pos,NoneFeatureConfiguration.INSTANCE));
        }
        // Do not use one placed feature per stage: that reseeds Minecraft's feature RNG.
        placed|=TCWorldFeatures.FOREST_FLOWERS.get().place(context);
        placed|=TCWorldFeatures.FOREST_LILY.get().place(context);
        placed|=TCWorldFeatures.FOREST_SMALL_MUSHROOMS.get().place(context);
        placed|=TCWorldFeatures.FOREST_REEDS.get().place(context);
        placed|=TCWorldFeatures.FOREST_PUMPKIN.get().place(context);
        placed|=TCWorldFeatures.FOREST_SPRINGS.get().place(context);
        placed|=TCWorldFeatures.FOREST_UNDERSTORY.get().place(context);
        return placed;
    }
}
