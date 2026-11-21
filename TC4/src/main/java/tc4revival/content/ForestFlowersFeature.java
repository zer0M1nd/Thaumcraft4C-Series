package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** The forest decorator's consecutive ten flower and twelve grass attempts. */
public  class ForestFlowersFeature extends Feature<NoneFeatureConfiguration> {
    public ForestFlowersFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var world=context.level(); var random=context.random(); var chunk=new ChunkPos(context.origin()); boolean placed=false;
        for (int attempt=0;attempt<22;attempt++) {
            int x=chunk.getMinBlockX()+8+random.nextInt(16),z=chunk.getMinBlockZ()+8+random.nextInt(16);
            int surface=LegacySurfaceHeight.at(world,x,z);
            // Retain the old y=0 origin where meaningful; avoid nextInt(nonpositive) below sea level.
            int floor=surface>0 ? 0 : world.getMinBuildHeight();
            int bound=attempt<10 ? surface+32-floor : (surface-floor)*2;
            int y=floor+random.nextInt(Math.max(1,bound));
            var pos=new BlockPos(x,y,z);
            if (attempt<10) placed|=LegacyForestPlants.flowers(world,pos,random,LegacyForestPlants.forestFlower(random));
            else placed|=LegacyForestPlants.grass(world,pos,random,random.nextInt(4)==0);
        }
        return placed;
    }
}
