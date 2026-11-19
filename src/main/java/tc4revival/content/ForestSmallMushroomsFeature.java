package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Six biome attempts followed by the two unconditional decorator probability checks. */
public  class ForestSmallMushroomsFeature extends Feature<NoneFeatureConfiguration> {
    public ForestSmallMushroomsFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        boolean placed=false;
        for (int attempt=0;attempt<7;attempt++) {
            if (context.random().nextInt(4)==0) placed|=patch(context,false,attempt==6);
            if (context.random().nextInt(8)==0) placed|=patch(context,true,true);
        }
        return placed;
    }
    static boolean patch(FeaturePlaceContext<NoneFeatureConfiguration> context,boolean red,boolean randomHeight) {
        var world=context.level(); var random=context.random(); var chunk=new ChunkPos(context.origin());
        int x=chunk.getMinBlockX()+8+random.nextInt(16),z=chunk.getMinBlockZ()+8+random.nextInt(16);
        int y=LegacySurfaceHeight.at(world,x,z);
        if (randomHeight) { int floor=y>0 ? 0 : world.getMinBuildHeight(); y=floor+random.nextInt(Math.max(1,(y-floor)*2)); }
        var origin=new BlockPos(x,y,z); boolean placed=false;
        for (int i=0;i<64;i++) {
            var pos=origin.offset(random.nextInt(8)-random.nextInt(8),random.nextInt(4)-random.nextInt(4),random.nextInt(8)-random.nextInt(8));
            if (world.isOutsideBuildHeight(pos) || !world.hasChunkAt(pos) || !world.isEmptyBlock(pos)) continue;
            if (world.getLevel().dimensionType().hasCeiling() && pos.getY()>=world.getMaxBuildHeight()-1) continue;
            var soil=world.getBlockState(pos.below());
            if (soil.is(Blocks.MYCELIUM) || soil.is(Blocks.PODZOL)
                    || world.getRawBrightness(pos,0)<13 && soil.isSolidRender(world,pos.below()))
                placed|=DecorationWrites.set(world,pos,(red ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState(),2);
        }
        return placed;
    }
}
