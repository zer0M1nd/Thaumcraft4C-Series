package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
/** Biomes are selected by the biome source; decoration never repaints neighbouring columns. */
public  class TaintDecorationFeature extends Feature<NoneFeatureConfiguration> {
    private final ForestOakGrowth magicTree=new ForestOakGrowth(),largeOak=ForestOakGrowth.ordinaryLargeOak();
    public TaintDecorationFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        var world=context.level();var random=context.random();var chunk=new ChunkPos(context.origin());
        boolean placed=TCWorldFeatures.FOREST_ORES.get().place(context);
        placed|=TCWorldFeatures.FOREST_SEDIMENTS.get().place(context);
        int trees=2+(random.nextInt(10)==0?1:0);
        for(int i=0;i<trees;i++){
            int x=chunk.getMinBlockX()+8+random.nextInt(16),z=chunk.getMinBlockZ()+8+random.nextInt(16);
            var p=new BlockPos(x,LegacySurfaceHeight.at(world,x,z),z);
            if(!inside(world,p))continue;
            placed|=random.nextInt(8)==0?magicTree.grow(world,p,random):random.nextInt(10)==0?largeOak.grow(world,p,random):LegacySmallOak.grow(world,p,random);
        }
        for(int i=0;i<2;i++){
            int x=chunk.getMinBlockX()+8+random.nextInt(16),z=chunk.getMinBlockZ()+8+random.nextInt(16),surface=LegacySurfaceHeight.at(world,x,z),floor=surface>0?0:world.getMinBuildHeight();
            placed|=LegacyForestPlants.grass(world,new BlockPos(x,floor+random.nextInt(Math.max(1,(surface-floor)*2)),z),random,false);
        }
        if(random.nextInt(4)==0)placed|=ForestSmallMushroomsFeature.patch(context,false,true);
        if(random.nextInt(8)==0)placed|=ForestSmallMushroomsFeature.patch(context,true,true);
        placed|=TCWorldFeatures.FOREST_REEDS.get().place(context);
        placed|=TCWorldFeatures.FOREST_PUMPKIN.get().place(context);
        placed|=TCWorldFeatures.FOREST_SPRINGS.get().place(context);
        int boulders=random.nextInt(3);
        for(int i=0;i<boulders;i++){
            int x=chunk.getMinBlockX()+8+random.nextInt(16),z=chunk.getMinBlockZ()+8+random.nextInt(16);
            var origin=new BlockPos(x,LegacySurfaceHeight.at(world,x,z),z);
            if(!inside(world,origin))continue;
            // Each lobe can walk across a boundary even when its origin is inside.
            placed|=ForestMossFeature.boulder(world,origin,random,TCArcane.TAINT_CRUST.get().defaultBlockState(),target->inside(world,target));
        }
        for(int i=0;i<10;i++){
            int x=chunk.getMinBlockX()+random.nextInt(16),z=chunk.getMinBlockZ()+random.nextInt(16);
            var p=new BlockPos(x,LegacySurfaceHeight.at(world,x,z)-1,z);var state=world.getBlockState(p);
            if(state.is(Blocks.GRASS_BLOCK) && inside(world,p.above()))placed|=world.setBlock(p.above(),TCArcane.TAINT_FIBRES.get().defaultBlockState(),2);
            else if(!state.isAir()&&state.canBeReplaced()&&world.getBlockState(p.below()).is(Blocks.GRASS_BLOCK)&&inside(world,p))placed|=world.setBlock(p,TCArcane.TAINT_FIBRES.get().defaultBlockState(),2);
        }
        for(int i=0;i<8;i++){
            int x=chunk.getMinBlockX()+random.nextInt(16),z=chunk.getMinBlockZ()+random.nextInt(16);
            int y=Math.max(10,world.getMinBuildHeight());
            while(y+1<world.getMaxBuildHeight()&&!world.isEmptyBlock(new BlockPos(x,y+1,z)))y++;
            var p=new BlockPos(x,y+1,z);
            if(!inside(world,p))continue;
            if(!world.isOutsideBuildHeight(p)&&world.isEmptyBlock(p)&&adjacentSolid(world,p))placed|=world.setBlock(p,TCArcane.TAINT_FIBRES.get().defaultBlockState(),2);
        }
        return placed;
    }
    private static boolean inside(WorldGenLevel world,BlockPos pos) {
        return !world.isOutsideBuildHeight(pos) && world.hasChunkAt(pos) && world.getBiome(pos).is(TCBiomes.TAINTED_LAND);
    }
    private static boolean adjacentSolid(WorldGenLevel level,BlockPos p){
        for(var direction:Direction.values()){var n=p.relative(direction);if(level.getBlockState(n).isFaceSturdy(level,n,direction.getOpposite()))return true;}return false;
    }
}
