package tc4revival.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import tc4revival.aspect.Aspect;

/** Loaded source lists cached by geometry and refreshed using world ticks. */
public  class EssentiaSources {
    private record Search(List<BlockPos> positions,long retryAt) {}
    private record SearchKey(BlockPos origin,Direction direction,int range,boolean ignoreMirror) {}
    private static final Map<Level,Map<SearchKey,Search>> SEARCHES=new WeakHashMap<>();
    private EssentiaSources() {}
    /** Model mouth is 14/16 high; its full-block collision must not constrain visual particles. */
    public static net.minecraft.world.phys.Vec3 visualOrigin(Level level,BlockPos source) {
        for(var located:thaumcraft.api.aspects.MobileEssentiaSources.find(level,new net.minecraft.world.phys.AABB(source)))return located.position();
        for(var entity:level.getEntities((net.minecraft.world.entity.Entity)null,new net.minecraft.world.phys.AABB(source),e->e instanceof EssentiaSource&&!e.isRemoved()))
            return entity.position().add(0,entity.getBbHeight()*.8,0);
        return source.getCenter().add(0,level.getBlockState(source).getBlock() instanceof EssentiaJarBlock ? 14/16d+.01-.5 : 0,0);
    }
    public static Optional<BlockPos> drain(BlockEntity consumer,Aspect aspect,Direction direction,int range,boolean ignoreMirror) {
        return find(consumer,aspect,direction,range,ignoreMirror,true);
    }
    public static boolean contains(BlockEntity consumer,Aspect aspect,Direction direction,int range) {
        return find(consumer,aspect,direction,range,false,false).isPresent();
    }
    /** Matches extraction geometry without changing retry caches or loading chunks. */
    public static List<EssentiaSource> availableSources(BlockEntity consumer,Direction direction,int range,boolean ignoreMirrors) {
        var level=consumer.getLevel();if(level==null||level.isClientSide||consumer.isRemoved())return List.of();
        if(range<0||range>127)throw new IllegalArgumentException("Essentia range exceeds source packet offsets");
        // Availability needs only block entities. Iterate already loaded chunk maps, not every block in the cube.
        var server=(net.minecraft.server.level.ServerLevel)level;var origin=consumer.getBlockPos();var result=new ArrayList<EssentiaSource>();
        var axis=direction==null?Direction.UP:direction;int start=direction==null?-range:0;
        for(int cx=(origin.getX()-range)>>4;cx<=(origin.getX()+range)>>4;cx++)for(int cz=(origin.getZ()-range)>>4;cz<=(origin.getZ()+range)>>4;cz++){
            var chunk=server.getChunkSource().getChunkNow(cx,cz);if(chunk==null)continue;
            for(var tile:chunk.getBlockEntities().values()){
                if(tile.isRemoved()||tile==consumer||!(tile instanceof EssentiaSource source)||ignoreMirrors&&source.mirror())continue;
                int dx=tile.getBlockPos().getX()-origin.getX(),dy=tile.getBlockPos().getY()-origin.getY(),dz=tile.getBlockPos().getZ()-origin.getZ();
                int a=axis.getAxis()==Direction.Axis.X?dy:dx,b=axis.getAxis()==Direction.Axis.Z?dy:dz,c=dx*axis.getStepX()+dy*axis.getStepY()+dz*axis.getStepZ();
                if(Math.abs(a)>range||Math.abs(b)>range||c<start||c>range)continue;
                var destination=source.mirrorDestination();if(consumer instanceof EssentiaSource self&&self.mirror()&&source.mirror()&&destination!=null&&destination.dimension().equals(level.dimension())&&destination.pos().equals(origin))continue;
                result.add(source);
            }
        }
        for(var entity:mobileSources(consumer,direction,range,ignoreMirrors))result.add((EssentiaSource)entity);
        for(var located:locatedSources(consumer,direction,range,ignoreMirrors))result.add(located.source());
        return result;
    }
    public static tc4revival.aspect.AspectAmounts available(BlockEntity consumer,tc4revival.aspect.AspectAmounts required) {
        var reservoirs=new java.util.LinkedHashMap<Object,EssentiaSource>();
        var visited=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<EssentiaSource,Boolean>());
        for(var source:availableSources(consumer,null,12,false))collect(source,visited,reservoirs);
        var result=new tc4revival.aspect.AspectAmounts();
        for(var aspect:required.aspects()) {
            long amount=0;for(var source:reservoirs.values())amount=Math.min(Integer.MAX_VALUE,amount+source.availableAmount(aspect));
            result.add(aspect,(int)amount);
        }
        return result;
    }
    private static void collect(EssentiaSource source,java.util.Set<EssentiaSource> visited,Map<Object,EssentiaSource> result) {
        if(!visited.add(source))return;
        for(var child:source.availabilitySources()) {
            if(child==source)result.putIfAbsent(source.availabilityIdentity(),source);
            else collect(child,visited,result);
        }
    }
    private static Optional<BlockPos> find(BlockEntity consumer,Aspect aspect,Direction direction,int range,boolean ignoreMirror,boolean drain) {
        var level=consumer.getLevel(); if (level==null || level.isClientSide || consumer.isRemoved()) return Optional.empty();
        // Moving reservoirs are queried at their current position, never stored in the static five-second cache.
        for(var located:locatedSources(consumer,direction,range,ignoreMirror)) {
            var source=located.source();
            if(drain?source.takeFromContainer(aspect,1):source.contains(aspect,1))return Optional.of(BlockPos.containing(located.position()));
        }
        for(var entity:mobileSources(consumer,direction,range,ignoreMirror)){
            var source=(EssentiaSource)entity;
            if(drain?source.takeFromContainer(aspect,1):source.contains(aspect,1))return Optional.of(entity.blockPosition());
        }
        var cache=SEARCHES.computeIfAbsent(level,key->new HashMap<>());
        var key=new SearchKey(consumer.getBlockPos().immutable(),direction,range,ignoreMirror);
        long now=level.getGameTime(); var search=cache.get(key);
        if (search==null || search.retryAt()<=now) {
            search=scan(consumer,direction,range,ignoreMirror,now); cache.put(key,search);
        }
        for (var pos:search.positions()) {
            if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof EssentiaSource source)) continue;
            if (ignoreMirror && source.mirror()) continue;
            if (drain ? source.takeFromContainer(aspect,1) : source.contains(aspect,1)) return Optional.of(pos);
        }
        return Optional.empty();
    }

    private static List<thaumcraft.api.aspects.MobileEssentiaSources.Located> locatedSources(BlockEntity consumer,Direction direction,int range,boolean ignoreMirrors) {
        if(range<0||range>127)throw new IllegalArgumentException("Essentia range exceeds source packet offsets");
        var origin=consumer.getBlockPos();var axis=direction==null?Direction.UP:direction;int start=direction==null?-range:0;
        return thaumcraft.api.aspects.MobileEssentiaSources.find(consumer.getLevel(),new net.minecraft.world.phys.AABB(origin).inflate(range)).stream().filter(located->{
            if(ignoreMirrors&&located.source().mirror())return false;
            var pos=BlockPos.containing(located.position());int dx=pos.getX()-origin.getX(),dy=pos.getY()-origin.getY(),dz=pos.getZ()-origin.getZ();
            int a=axis.getAxis()==Direction.Axis.X?dy:dx,b=axis.getAxis()==Direction.Axis.Z?dy:dz,c=dx*axis.getStepX()+dy*axis.getStepY()+dz*axis.getStepZ();
            return Math.abs(a)<=range&&Math.abs(b)<=range&&c>=start&&c<=range;
        }).toList();
    }
    private static List<net.minecraft.world.entity.Entity> mobileSources(BlockEntity consumer,Direction direction,int range,boolean ignoreMirrors) {
        if(range<0||range>127)throw new IllegalArgumentException("Essentia range exceeds source packet offsets");
        var origin=consumer.getBlockPos();var axis=direction==null?Direction.UP:direction;int start=direction==null?-range:0;
        return consumer.getLevel().getEntities((net.minecraft.world.entity.Entity)null,new net.minecraft.world.phys.AABB(origin).inflate(range),entity->{
            if(entity.isRemoved()||!(entity instanceof EssentiaSource source)||ignoreMirrors&&source.mirror())return false;
            var pos=entity.blockPosition();int dx=pos.getX()-origin.getX(),dy=pos.getY()-origin.getY(),dz=pos.getZ()-origin.getZ();
            int a=axis.getAxis()==Direction.Axis.X?dy:dx,b=axis.getAxis()==Direction.Axis.Z?dy:dz,c=dx*axis.getStepX()+dy*axis.getStepY()+dz*axis.getStepZ();
            return Math.abs(a)<=range&&Math.abs(b)<=range&&c>=start&&c<=range;
        });
    }
    private static Search scan(BlockEntity consumer,Direction direction,int range,boolean ignoreMirror,long now) {
        var origin=consumer.getBlockPos();
        var positions=availableSources(consumer,direction,range,ignoreMirror).stream()
                .filter(source->source instanceof BlockEntity).map(source->((BlockEntity)source).getBlockPos().immutable())
                .distinct().sorted(java.util.Comparator.comparingDouble((BlockPos pos)->pos.distSqr(origin))
                        .thenComparing(BlockPos::compareTo)).toList();
        return new Search(positions,now+100);
    }
    public static void refresh(BlockEntity consumer) {
        var cache=SEARCHES.get(consumer.getLevel());
        if(cache!=null)cache.keySet().removeIf(key->key.origin().equals(consumer.getBlockPos()));
    }
}
