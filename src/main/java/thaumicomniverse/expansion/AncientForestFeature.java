package thaumicomniverse.expansion;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import tc4revival.content.*;

public final class AncientForestFeature extends Feature<NoneFeatureConfiguration> {
    public AncientForestFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        var world=context.level();var random=context.random();var chunk=new ChunkPos(context.origin());
        TCWorldFeatures.FOREST_ORES.get().place(context);TCWorldFeatures.FOREST_SEDIMENTS.get().place(context);
        TCWorldFeatures.FOREST_SPRINGS.get().place(context);
        // Keep the ordinary forest-oak attempt frequency exactly: 2.1 * 13/14 * 9/10.
        // Tree rolls are independent so extra magic trees do not replace oak attempts.
        int attempts=2+(random.nextInt(10)==0?1:0);
        if(tc4revival.config.TCConfig.SERVER.generates(tc4revival.config.TCConfig.WorldFeature.TREES)){
            // Reserve canopy space before the small trees. A typical 128–184 block
            // grove spans dozens of chunks; retry slopes within the selected chunk.
            if(random.nextInt(2)==0){
                boolean silver=random.nextBoolean();
                for(int retry=0;retry<3;retry++)if(AncientTrees.giant(world,surface(context),random,silver))break;
            }
            for(int i=0;i<attempts;i++){
                if(random.nextInt(14)!=0&&random.nextInt(10)!=0)new ForestOakGrowth().grow(world,surface(context),random);
                int silverRolls=6+(random.nextBoolean()?1:0);
                int greatRolls=3+(random.nextInt(10)<9?1:0);
                for(int j=0;j<silverRolls;j++)if(random.nextInt(14)==0)AncientTrees.grow(world,surface(context),random,true);
                for(int j=0;j<greatRolls;j++)if(random.nextInt(14)!=0&&random.nextInt(10)==0)AncientTrees.grow(world,surface(context),random,false);
            }
            for(int i=0;i<3+(random.nextInt(10)<9?1:0);i++)if(random.nextInt(25)==7)AncientTrees.grow(world,surface(context),random,false);
            if(random.nextInt(40)<13){
                var id=ResourceLocation.parse(random.nextBoolean()?"minecraft:oak":"minecraft:birch");
                world.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(ResourceKey.create(Registries.CONFIGURED_FEATURE,id))
                        .ifPresent(tree->tree.value().place(world,context.chunkGenerator(),random,surface(context)));
            }
        }
        int plantPasses=2+(random.nextInt(10)<6?1:0);
        for(int i=0;i<plantPasses;i++){
            TCWorldFeatures.FOREST_MOSS.get().place(context);TCWorldFeatures.FOREST_MUSHROOM.get().place(context);
            TCWorldFeatures.FOREST_FLOWERS.get().place(context);TCWorldFeatures.FOREST_LILY.get().place(context);
            TCWorldFeatures.FOREST_SMALL_MUSHROOMS.get().place(context);TCWorldFeatures.FOREST_REEDS.get().place(context);
            TCWorldFeatures.FOREST_PUMPKIN.get().place(context);TCWorldFeatures.FOREST_UNDERSTORY.get().place(context);
        }
        return true;
    }
    private static BlockPos surface(FeaturePlaceContext<?> context){
        var chunk=new ChunkPos(context.origin());int x=chunk.getMinBlockX()+4+context.random().nextInt(8),z=chunk.getMinBlockZ()+4+context.random().nextInt(8);
        return new BlockPos(x,context.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);
    }
}
