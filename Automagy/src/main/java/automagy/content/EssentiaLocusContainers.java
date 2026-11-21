package automagy.content;

import automagy.api.EssentiaLocusReadable;
import automagy.logic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import tc4revival.content.*;

/** The scanner observes containers; it never simulates or commits extraction. */
public final class EssentiaLocusContainers {
    public static boolean readable(ServerLevel level,BlockPos pos){
        if(!level.hasChunkAt(pos))return false;var tile=level.getBlockEntity(pos);
        return tile instanceof EssentiaJarBlockEntity||tile instanceof EssentiaReservoirBlockEntity||tile instanceof EssentiaLocusReadable;
    }
    public static List<BlockPos> discover(ServerLevel level,BlockPos origin,List<GlobalPos> eyes,int distance,int jars){
        List<BlockPos> seeds=eyes.stream().filter(eye->eye.dimension().equals(level.dimension())&&eye.pos().distSqr(origin)<=(double)distance*distance).map(GlobalPos::pos).toList();
        return ConnectedContainers.discover(seeds,jars,pos->readable(level,pos),pos->Arrays.stream(Direction.values()).map(pos::relative).toList());
    }
    public static AspectReading observe(ServerLevel level,List<BlockPos> jars){
        var contents=new TreeMap<String,Integer>();var capacities=new TreeMap<String,Integer>();
        for(var pos:new LinkedHashSet<>(jars)){
            if(!level.hasChunkAt(pos))continue;var tile=level.getBlockEntity(pos);
            if(tile instanceof EssentiaJarBlockEntity jar){
                var aspect=jar.aspect()!=null?jar.aspect():jar.filter();
                if(aspect!=null){add(capacities,aspect.tag(),jar.capacity());if(jar.amount()>0)add(contents,aspect.tag(),jar.amount());}
            }else if(tile instanceof EssentiaReservoirBlockEntity reservoir){
                // Legacy reservoirs contribute stock, but no per-type maximum (shared capacity).
                reservoir.revealedAspects().view().forEach((aspect,n)->add(contents,aspect.tag(),n));
            }else if(tile instanceof EssentiaLocusReadable readable){
                var snapshot=readable.aspectReading();if(!snapshot.valid())continue;
                snapshot.contents().forEach((aspect,n)->add(contents,aspect,n));snapshot.capacities().forEach((aspect,n)->add(capacities,aspect,n));
            }
        }
        return new AspectReading(contents,capacities,capacities.values().stream().mapToLong(Integer::longValue).sum(),0,true,true);
    }
    private static void add(Map<String,Integer> values,String key,int amount){if(amount>0)values.put(key,(int)Math.min(Integer.MAX_VALUE,(long)values.getOrDefault(key,0)+amount));}
    private EssentiaLocusContainers(){}
}
