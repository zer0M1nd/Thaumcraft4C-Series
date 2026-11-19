package thaumicomniverse.expansion;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.*;

/** Solid octagonal trunks, buttress roots and tiered radial crowns. */
public final class AncientTrees {
    public static boolean grow(WorldGenLevel level,BlockPos pos,RandomSource random,boolean silver){
        if(silver){
            int height=random.nextInt(4)==0?13+random.nextInt(4):10+random.nextInt(3);
            boolean result=SilverwoodGrowth.grow(level,pos,random,height,1,false,18);
            if(result)MagicalFlowerGrowth.scatter(level,pos,random,MagicalPlantBlock.Kind.SHIMMERLEAF);
            return result;
        }
        return GreatwoodGrowth.natural(level,pos,random,random.nextInt(4)==0?22+random.nextInt(8):17+random.nextInt(5));
    }
    public static boolean section(int x,int z,int width){
        double center=(width-1)/2d,dx=Math.abs(x-center),dz=Math.abs(z-center);
        return Math.max(dx,dz)+.5*Math.min(dx,dz)<=width/2d;
    }
    public static boolean giant(WorldGenLevel level,BlockPos base,RandomSource random,boolean silver){
        int greatWidth=5+random.nextInt(3),greatHeight=40+random.nextInt(40);
        int width=silver?Math.round(greatWidth*1.5f):greatWidth;
        int height=silver?Math.round(greatHeight*.6f):greatHeight;
        int radius=silver?Math.min(18,width+5):Math.min(20,width*2+3);
        if(!DecorationWrites.allowed(level,base.offset(-radius,-1,-radius))
                ||!DecorationWrites.allowed(level,base.offset(radius,height+9,radius)))return false;
        var logs=new LinkedHashMap<BlockPos,BlockState>();
        var leaves=new HashSet<BlockPos>();
        var log=(silver?TCArcane.SILVERWOOD_LOG:TCArcane.GREATWOOD_LOG).get().defaultBlockState();
        var leaf=(silver?TCArcane.SILVERWOOD_LEAVES:TCArcane.GREATWOOD_LEAVES).get().defaultBlockState();
        int low=-(width/2);
        for(int x=0;x<width;x++)for(int z=0;z<width;z++){
            if(!section(x,z,width))continue;
            var foot=base.offset(low+x,0,low+z);
            // Natural forests are rarely flat across a 5–11 block trunk. Each column
            // roots in its own soil height; the crown still shares a single origin.
            int ground=ground(level,foot);
            if(ground==Integer.MIN_VALUE)return false;
            for(int y=ground+1;y<base.getY()+height;y++)logs.put(new BlockPos(foot.getX(),y,foot.getZ()),log);
        }
        // Wide branches at three heights preserve the two-crown greatwood silhouette.
        for(int tier=0;tier<3;tier++){
            int y=height*(6+tier)/10;
            double phase=random.nextDouble()*Math.PI*2;
            for(int branch=0;branch<8;branch++){
                double angle=phase+branch*Math.PI/4;
                int reach=radius-5-tier*2;
                var end=base.offset((int)Math.round(Math.cos(angle)*reach),y+4+random.nextInt(4),(int)Math.round(Math.sin(angle)*reach));
                line(logs,base.above(y-3),end,log,1);
                crown(leaves,end,4+(tier==0?1:0),silver?5:3);
            }
        }
        crown(leaves,base.above(height-1),silver?width+2:width+1,silver?8:5);
        for(var side:Direction.Plane.HORIZONTAL){
            var end=base.relative(side,width/2+3);
            int ground=ground(level,end);
            if(ground!=Integer.MIN_VALUE)line(logs,base.above(6),new BlockPos(end.getX(),ground+1,end.getZ()),log,1);
        }
        // Plan first: no partial trunks, no overwriting machines, no writes into ungenerated chunks.
        for(var pos:logs.keySet()){
            boolean rootSoil=Math.abs(pos.getY()-base.getY())<=6&&level.getBlockState(pos).is(BlockTags.DIRT);
            if(!DecorationWrites.allowed(level,pos)||!rootSoil&&!replaceable(level,pos))return false;
        }
        for(var pos:leaves)if(!DecorationWrites.allowed(level,pos))return false;
        for(var entry:logs.entrySet())DecorationWrites.set(level,entry.getKey(),entry.getValue(),3);
        for(var pos:leaves)if(!logs.containsKey(pos)&&replaceable(level,pos))DecorationWrites.set(level,pos,leaf,3);
        if(silver){
            boolean previous=false;
            for(int y=1;y<height;y++){
                boolean selected=!previous&&random.nextInt(18)==0;previous=selected;
                if(!selected)continue;
                var node=base.above(y);
                // TC4 embeds pure nodes in the central log, enclosed by the trunk.
                DecorationWrites.set(level,node,TCArcane.SILVERWOOD_NODE_LOG.get().defaultBlockState(),3);
                AuraNodeGeneration.createRandom(level,node,random,true,false,false);
            }
            MagicalFlowerGrowth.scatter(level,base,random,MagicalPlantBlock.Kind.SHIMMERLEAF);
        }
        return true;
    }
    private static int ground(WorldGenLevel level,BlockPos foot){
        for(int y=foot.getY()+6;y>=foot.getY()-7;y--){
            var soil=new BlockPos(foot.getX(),y,foot.getZ());
            var state=level.getBlockState(soil);
            if(state.canSustainPlant(level,soil,Direction.UP,(net.minecraftforge.common.IPlantable)Blocks.OAK_SAPLING))return y;
            if(!replaceable(level,soil))return Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }
    private static boolean replaceable(WorldGenLevel level,BlockPos pos){
        var state=level.getBlockState(pos);return state.isAir()||state.is(BlockTags.LEAVES)||state.is(BlockTags.REPLACEABLE_BY_TREES)||state.canBeReplaced();
    }
    private static void line(Map<BlockPos,BlockState> logs,BlockPos from,BlockPos to,BlockState state,int thickness){
        int length=Math.max(Math.abs(from.getX()-to.getX()),Math.max(Math.abs(from.getY()-to.getY()),Math.abs(from.getZ()-to.getZ())));
        var axis=Math.abs(to.getX()-from.getX())>=Math.abs(to.getZ()-from.getZ())?Direction.Axis.X:Direction.Axis.Z;
        for(int step=0;step<=length;step++){
            double t=length==0?0:(double)step/length;
            var pos=BlockPos.containing(from.getX()+(to.getX()-from.getX())*t,from.getY()+(to.getY()-from.getY())*t,from.getZ()+(to.getZ()-from.getZ())*t);
            logs.put(pos,state.setValue(RotatedPillarBlock.AXIS,axis));
            if(thickness>0)logs.put(pos.above(),state.setValue(RotatedPillarBlock.AXIS,axis));
        }
    }
    private static void crown(Set<BlockPos> leaves,BlockPos center,int radius,int height){
        for(int y=-height;y<=height;y++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)
            if((double)(x*x+z*z)/(radius*radius)+(double)y*y/(height*height)<=1)leaves.add(center.offset(x,y,z));
    }
    private AncientTrees(){}
}
