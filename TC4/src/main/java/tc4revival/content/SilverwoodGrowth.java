package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;

/** Cross-shaped silverwood trunks, rounded crowns, radial roots and embedded pure nodes. */
public  class SilverwoodGrowth {
    private SilverwoodGrowth() {}
    public static boolean natural(WorldGenLevel level,BlockPos base,RandomSource random,int minimum,int variance) {
        if (!grow(level,base,random,minimum,variance)) return false;
        MagicalFlowerGrowth.scatter(level,base,random,MagicalPlantBlock.Kind.SHIMMERLEAF); return true;
    }
    public static boolean grow(WorldGenLevel level,BlockPos base,RandomSource random,int minimum,int variance) {
        return grow(level,base,random,minimum,variance,false,0);
    }
    /** Surface nodes with a fixed per-height rate allow larger addon trees without hidden nodes. */
    public static boolean grow(WorldGenLevel level,BlockPos base,RandomSource random,int minimum,int variance,boolean surfaceNodes,int nodeInterval) {
        int height=minimum+random.nextInt(variance);
        if (base.getY()<=level.getMinBuildHeight() || base.getY()+height+5>=level.getMaxBuildHeight()
                || !level.hasChunksAt(base.offset(-5,-1,-5),base.offset(5,height+5,5))) return false;
        if(!DecorationWrites.allowed(level,base.offset(-5,0,-5))||!DecorationWrites.allowed(level,base.offset(5,0,5)))return false;
        for (int y=0;y<=height+1;y++) {
            int spread=y==0 ? 0 : y>=height-1 ? 3 : 1;
            for (int x=-spread;x<=spread;x++) for (int z=-spread;z<=spread;z++) if (y>0 && !replaceable(level,base.offset(x,y,z))) return false;
        }
        var soil=level.getBlockState(base.below());
        if (!soil.canSustainPlant(level,base.below(),Direction.UP,(net.minecraftforge.common.IPlantable)Blocks.OAK_SAPLING)) return false;
        // The removed 1.7 soil hook converted these vanilla supporting soils to dirt.
        if (soil.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) || soil.is(net.minecraft.world.level.block.Blocks.MYCELIUM)
                || soil.is(net.minecraft.world.level.block.Blocks.FARMLAND))
            DecorationWrites.set(level,base.below(),net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(),3);
        int end=height+3+random.nextInt(3);
        for (int y=height-5;y<=end;y++) {
            int center=Mth.clamp(y,height-3,height);
            for (int x=-5;x<=5;x++) for (int z=-5;z<=5;z++) {
                int distance=x*x+(y-center)*(y-center)+z*z;
                var pos=base.offset(x,y,z);
                if (distance<10+random.nextInt(8) && leafReplaceable(level,pos)) DecorationWrites.set(level,pos,TCArcane.SILVERWOOD_LEAVES.get().defaultBlockState(),3);
            }
        }
        int chance=nodeInterval>0?nodeInterval:(int)(height*1.5); boolean previousNode=false;
        var exposedNodes=new java.util.ArrayList<BlockPos>();
        for (int y=0;y<height;y++) {
            var center=base.above(y); if (!replaceable(level,center)) continue;
            boolean node=y>0 && !previousNode && random.nextInt(chance)==0;
            if (node && surfaceNodes) {
                log(level,center,Direction.Axis.Y);
                exposedNodes.add(center.relative(Direction.from2DDataValue(random.nextInt(4))));
            } else if (node) {
                DecorationWrites.set(level,center,TCArcane.SILVERWOOD_NODE_LOG.get().defaultBlockState(),3);
                AuraNodeGeneration.createRandom(level,center,random,true,false,false); if(nodeInterval<=0)chance+=height;
            } else log(level,center,Direction.Axis.Y);
            previousNode=node;
            for (var side:Direction.Plane.HORIZONTAL) log(level,center.relative(side),Direction.Axis.Y);
        }
        log(level,base.above(height),Direction.Axis.Y);
        // Ordering also determines the subsequent random stream used by embedded nodes and foliage.
        int[][] corners={{-1,-1},{1,1},{-1,1},{1,-1}};
        for (var corner:corners) log(level,base.offset(corner[0],0,corner[1]),Direction.Axis.Y);
        for (var corner:corners) if (random.nextInt(3)!=0) log(level,base.offset(corner[0],1,corner[1]),Direction.Axis.Y);
        arms(level,base,0);
        for (var side:Direction.Plane.HORIZONTAL) log(level,base.relative(side,2).below(),Direction.Axis.Y);
        for (var corner:corners) log(level,base.offset(corner[0],height-4,corner[1]),Direction.Axis.Y);
        for (var corner:corners) if (random.nextInt(3)==0) log(level,base.offset(corner[0],height-5,corner[1]),Direction.Axis.Y);
        arms(level,base,height-4);
        for(var node:exposedNodes){
            DecorationWrites.set(level,node,TCArcane.SILVERWOOD_NODE_LOG.get().defaultBlockState(),3);
            AuraNodeGeneration.createRandom(level,node,random,true,false,false);
        }
        return true;
    }
    private static void arms(WorldGenLevel level,BlockPos base,int y) {
        for (var side:Direction.Plane.HORIZONTAL) log(level,base.above(y).relative(side,2),side.getAxis());
    }
    private static void log(WorldGenLevel level,BlockPos pos,Direction.Axis axis) { DecorationWrites.set(level,pos,TCArcane.SILVERWOOD_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,axis),3); }
    private static boolean replaceable(WorldGenLevel level,BlockPos pos) {
        var state=level.getBlockState(pos); return state.isAir() || state.is(BlockTags.LEAVES) || state.canBeReplaced();
    }
    private static boolean leafReplaceable(WorldGenLevel level,BlockPos pos) {
        var state=level.getBlockState(pos); return state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.REPLACEABLE_BY_TREES);
    }
}
