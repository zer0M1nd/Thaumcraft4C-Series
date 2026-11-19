package thaumicfutures.devices;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import tc4revival.content.AuraNodeBlockEntity;

/** TC5's 13 by 13 chunks, through the full build height. Reading saved chunks never loads them. */
public final class NodeSurvey {
    public static final int CHUNK_RADIUS=6;
    private final Map<ChunkPos,List<NodeReading>> readings=new HashMap<>();
    private final Map<ChunkPos,CompletableFuture<Optional<CompoundTag>>> pending=new LinkedHashMap<>();
    private final Map<ChunkPos,Long> lastRead=new HashMap<>();
    private long nextScan;
    public void tick(ServerLevel level,BlockPos center) {
        long now=level.getGameTime();
        for(var iterator=pending.entrySet().iterator();iterator.hasNext();) {
            var entry=iterator.next();if(!entry.getValue().isDone())continue;
            iterator.remove();var cp=entry.getKey();
            if(level.getChunkSource().getChunkNow(cp.x,cp.z)!=null)continue;
            try {
                var nodes=new ArrayList<NodeReading>();
                entry.getValue().join().ifPresent(chunk->{
                    for(var raw:chunk.getList("block_entities",Tag.TAG_COMPOUND)) {
                        var tag=(CompoundTag)raw;
                        if(tag.contains("nodeId",Tag.TAG_STRING) && tag.contains("AspectsBase",Tag.TAG_LIST)) {
                            var node=NodeReading.read(tag,false);
                            if(new ChunkPos(node.pos()).equals(cp))nodes.add(node);
                        }
                    }
                });
                readings.put(cp,List.copyOf(nodes));
            } catch(java.util.concurrent.CompletionException error) {
                // A transient disk failure must not erase the last valid survey or block the server.
                lastRead.put(cp,now-1100);
            }
        }
        if(now<nextScan)return;nextScan=now+20;
        var origin=new ChunkPos(center);int scheduled=0;
        for(int x=origin.x-CHUNK_RADIUS;x<=origin.x+CHUNK_RADIUS;x++)for(int z=origin.z-CHUNK_RADIUS;z<=origin.z+CHUNK_RADIUS;z++) {
            var cp=new ChunkPos(x,z);var chunk=level.getChunkSource().getChunkNow(x,z);
            if(chunk!=null) {
                var nodes=new ArrayList<NodeReading>();
                for(var entity:chunk.getBlockEntities().values())if(entity instanceof AuraNodeBlockEntity node && !node.isRemoved())nodes.add(NodeReading.of(node));
                readings.put(cp,List.copyOf(nodes));lastRead.remove(cp);
            } else if(scheduled<24 && !pending.containsKey(cp) && now-lastRead.getOrDefault(cp,now-1200)>=1200) {
                pending.put(cp,level.getChunkSource().chunkMap.read(cp));lastRead.put(cp,now);scheduled++;
            }
        }
    }
    public List<NodeReading> nodes() {
        return readings.values().stream().flatMap(Collection::stream).sorted(Comparator.comparingLong(n->n.pos().asLong())).toList();
    }
    public int surveyedChunks() { return readings.size(); }
}
