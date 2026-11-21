package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

/** Old forest springs retain the nested height distribution and exact stone-wall shape. */
public  class ForestSpringsFeature extends Feature<NoneFeatureConfiguration> {
    public ForestSpringsFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var random=context.random(); var chunk=new ChunkPos(context.origin()); boolean placed=false;
        for (int attempt=0;attempt<70;attempt++) {
            boolean lava=attempt>=50;
            int x=chunk.getMinBlockX()+8+random.nextInt(16);
            int y=lava ? random.nextInt(random.nextInt(random.nextInt(240)+8)+8) : random.nextInt(random.nextInt(248)+8);
            int z=chunk.getMinBlockZ()+8+random.nextInt(16);
            placed|=spring(context.level(),new BlockPos(x,y,z),lava);
        }
        return placed;
    }
    static boolean spring(WorldGenLevel world,BlockPos pos,boolean lava) {
        if (world.isOutsideBuildHeight(pos.above()) || world.isOutsideBuildHeight(pos.below())
                || !world.hasChunksAt(pos.offset(-1,-1,-1),pos.offset(1,1,1))) return false;
        if (!world.getBlockState(pos.above()).is(Blocks.STONE) || !world.getBlockState(pos.below()).is(Blocks.STONE)) return false;
        var center=world.getBlockState(pos); if (!center.isAir() && !center.is(Blocks.STONE)) return false;
        int stone=0,air=0;
        for (var side:new Direction[]{Direction.WEST,Direction.EAST,Direction.NORTH,Direction.SOUTH}) {
            var state=world.getBlockState(pos.relative(side));
            if (state.is(Blocks.STONE)) stone++; if (state.isAir()) air++;
        }
        if (stone!=3 || air!=1) return false;
        var fluid=lava ? Fluids.LAVA : Fluids.WATER;
        if (!DecorationWrites.set(world,pos,fluid.defaultFluidState().createLegacyBlock(),2)) return false;
        world.scheduleTick(pos,fluid,0);
        return true;
    }
}
