package tc4revival.content;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

/** The magical forest oak shape with invocation-local generation state. */
public  class ForestOakGrowth {
    private final int minimumHeight,leafLayers;
    private final double attenuation,leafDensity;
    public ForestOakGrowth(){this(11,3,.6618,.8);}
    private ForestOakGrowth(int minimumHeight,int leafLayers,double attenuation,double leafDensity){
        this.minimumHeight=minimumHeight;this.leafLayers=leafLayers;this.attenuation=attenuation;this.leafDensity=leafDensity;
    }
    static ForestOakGrowth ordinaryLargeOak(){return new ForestOakGrowth(5,5,.618,1);}
    private record Branch(BlockPos leaves,int joinY) {}

    public boolean grow(WorldGenLevel level,BlockPos base,RandomSource source) {
        if(!DecorationWrites.allowed(level,base.offset(-12,0,-12))||!DecorationWrites.allowed(level,base.offset(12,0,12)))return false;
        var random=RandomSource.create(source.nextLong());
        int limit=minimumHeight+random.nextInt(12);
        if (base.getY()<=level.getMinBuildHeight() || base.getY()+limit>=level.getMaxBuildHeight()
                || !level.hasChunksAt(base.offset(-12,-1,-12),base.offset(12,limit,12))) return false;
        if (!level.getBlockState(base.below()).canSustainPlant(level,base.below(),Direction.UP,
                (net.minecraftforge.common.IPlantable)Blocks.OAK_SAPLING)) return false;
        int blocked=obstruction(level,base,base.above(limit-1));
        if (blocked>=0) {
            if (blocked<6) return false;
            limit=blocked;
        }
        int trunkHeight=Math.min(limit-1,(int)(limit*attenuation)),top=base.getY()+trunkHeight;
        int attempts=Math.max(1,(int)(1.382+Math.pow(leafDensity*limit/13,2)));
        int relative=limit-leafLayers,y=base.getY()+relative;
        var branches=new ArrayList<Branch>();
        branches.add(new Branch(new BlockPos(base.getX(),y--,base.getZ()),top));
        for (;relative>=0;relative--,y--) {
            if (relative<limit*.3) continue;
            float half=limit/2f,offset=half-relative;
            float radius=(Math.abs(offset)>=half ? 0 : (float)Math.sqrt(half*half-offset*offset))*.5f;
            for (int attempt=0;attempt<attempts;attempt++) {
                double distance=radius*(random.nextFloat()+.328),angle=random.nextFloat()*2*Math.PI;
                var leaves=new BlockPos(Mth.floor(base.getX()+.5+distance*Math.sin(angle)),y,
                        Mth.floor(base.getZ()+.5+distance*Math.cos(angle)));
                if (obstruction(level,leaves,leaves.above(leafLayers))>=0) continue;
                double reach=Math.sqrt(Math.pow(base.getX()-leaves.getX(),2)+Math.pow(base.getZ()-leaves.getZ(),2));
                int join=Math.min(top,(int)(y-reach*.381));
                if (obstruction(level,new BlockPos(base.getX(),join,base.getZ()),leaves)<0)
                    branches.add(new Branch(leaves,join));
            }
        }
        for (var branch:branches) for (int layer=0;layer<leafLayers;layer++) {
            int radius=layer==0 || layer==leafLayers-1 ? 2 : 3;
            for (int z=-radius;z<=radius;z++) for (int x=-radius;x<=radius;x++) {
                if (Math.pow(Math.abs(x)+.5,2)+Math.pow(Math.abs(z)+.5,2)>radius*radius) continue;
                var pos=branch.leaves().offset(x,layer,z);
                if (replaceable(level.getBlockState(pos))) DecorationWrites.set(level,pos,Blocks.OAK_LEAVES.defaultBlockState(),2);
            }
        }
        logs(level,base,base.above(trunkHeight));
        for (var branch:branches) if (branch.joinY()-base.getY()>=limit*.2)
            logs(level,new BlockPos(base.getX(),branch.joinY(),base.getZ()),branch.leaves());
        return true;
    }
    private static boolean replaceable(BlockState state) {
        // Modern tree canopies, including modded leaves, are replaceable.
        return state.isAir() || state.is(net.minecraft.tags.BlockTags.LEAVES);
    }
    private static int obstruction(WorldGenLevel level,BlockPos start,BlockPos end) {
        var points=GreatwoodGrowth.line(start,end,false);
        for (int index=0;index<points.size();index++) {
            var pos=points.get(index);
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !replaceable(level.getBlockState(pos))) return index;
        }
        return -1;
    }
    private static void logs(WorldGenLevel level,BlockPos start,BlockPos end) {
        for (var pos:GreatwoodGrowth.line(start,end,true)) {
            int x=Math.abs(pos.getX()-start.getX()),z=Math.abs(pos.getZ()-start.getZ());
            var axis=x==0 && z==0 ? Direction.Axis.Y : x>=z ? Direction.Axis.X : Direction.Axis.Z;
            DecorationWrites.set(level,pos,Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,axis),2);
        }
    }
}
