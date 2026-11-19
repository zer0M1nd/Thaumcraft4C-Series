package automagy.content;

import automagy.api.AspectReadingProvider;
import automagy.logic.AspectReading;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thaumcraft.api.aspects.*;
import thaumcraft.api.nodes.NodeApi;

/** Adapters are observational: no fake source containers and no calls to extraction methods. */
public final class AspectReadings {
    public static AspectReading at(ServerLevel level,BlockPos position) {
        return at(level,position,new HashSet<>());
    }
    static AspectReading at(ServerLevel level,BlockPos pos,Set<BlockPos> visited) {
        if(visited.size()>=64||!visited.add(pos)||!level.hasChunkAt(pos))return AspectReading.MISSING;
        if(level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.WATER)){
            net.minecraft.core.Direction[] directions={net.minecraft.core.Direction.NORTH,net.minecraft.core.Direction.SOUTH,net.minecraft.core.Direction.EAST,net.minecraft.core.Direction.WEST};
            for(var direction:directions){var first=pos.above(2).relative(direction);
                if(level.hasChunkAt(first)&&level.getBlockEntity(first) instanceof EssentiaLocusBlockEntity locus)return locus.aspectReading();
                for(var second:directions){var candidate=first.relative(second);if(level.hasChunkAt(candidate)&&level.getBlockEntity(candidate) instanceof EssentiaLocusBlockEntity locus)return locus.aspectReading();}
            }
        }
        var tile=level.getBlockEntity(pos);
        if(tile instanceof VisReaderBlockEntity reader)return reader.read(visited);
        if(tile instanceof AspectReadingProvider provider)return provider.aspectReading();
        if(tile instanceof AuraNodeBlockEntity node) {
            var snapshot=NodeApi.snapshot(node);var contents=map(snapshot.charge().unwrap());var caps=map(snapshot.capacity().unwrap());
            long total=contents.keySet().stream().mapToLong(key->Math.max(0,caps.getOrDefault(key,0))).sum();
            return new AspectReading(contents,caps,total,0,true,true);
        }
        if(tile instanceof WandPedestalBlockEntity pedestal) {
            var stack=pedestal.getItem(0);var contents=new TreeMap<String,Integer>();
            int capacity=stack.getItem() instanceof CastingWandItem wand?wand.capacity(stack)
                    :stack.getItem() instanceof VisAmuletItem amulet?amulet.capacity(stack):0;
            if(capacity>0)for(var aspect:TCAspects.catalog().primals())contents.put(aspect.tag(),WandVis.stored(stack.getTag(),aspect)/100);
            int whole=capacity/100;
            return new AspectReading(contents,Map.of(),capacity>0?(long)whole*6:25,whole,false,true);
        }
        if(tile instanceof EssentiaVessel vessel) {
            var contents=new TreeMap<String,Integer>();
            if(vessel.aspect()!=null&&vessel.amount()>0)contents.put(vessel.aspect().tag(),vessel.amount());
            return new AspectReading(contents,Map.of(),vessel.capacity(),vessel.capacity(),false,true);
        }
        if(tile instanceof IEssentiaStorage storage) {
            var values=storage.contents();var caps=new TreeMap<String,Integer>();int total=0;
            for(var aspect:values.aspects()){int cap=storage.capacity(aspect);caps.put(aspect.tag(),cap);total=Math.max(total,cap);}
            return new AspectReading(map(values),caps,total,total,false,true);
        }
        if(tile instanceof IAspectContainer container) {
            var values=container.getAspects().unwrap();var caps=new TreeMap<String,Integer>();
            for(var aspect:values.aspects())caps.put(aspect.tag(),container.capacity(aspect));
            long total=caps.values().stream().mapToLong(Integer::longValue).sum();
            return new AspectReading(map(values),caps,total,0,true,true);
        }
        if(tile instanceof RevealedAspectContainer revealed) {
            int capacity=tile instanceof EssentiaReservoirBlockEntity?256:tile instanceof EssentiaTubeBlockEntity?8:64;
            return new AspectReading(map(revealed.revealedAspects()),Map.of(),capacity,capacity,false,true);
        }
        return AspectReading.MISSING;
    }
    public static Map<String,Integer> map(AspectAmounts values) {
        var map=new TreeMap<String,Integer>();values.view().forEach((aspect,amount)->map.put(aspect.tag(),Math.max(0,amount)));return map;
    }
    private AspectReadings(){}
}
