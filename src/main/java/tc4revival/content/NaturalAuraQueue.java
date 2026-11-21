package tc4revival.content;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
/** Generation workers enqueue immutable candidates; world writes happen on the server tick thread. */
public  class NaturalAuraQueue {
    private record Incoming(long chunk,long seed) {}
    private static final Map<ServerLevel,ConcurrentLinkedQueue<Incoming>> INCOMING=new java.util.concurrent.ConcurrentHashMap<>();
    private static final int PROBES_PER_TICK=48;
    private NaturalAuraQueue() {}
    public static void enqueue(ServerLevel level,ChunkPos chunk,long seed) { INCOMING.computeIfAbsent(level,k->new ConcurrentLinkedQueue<>()).add(new Incoming(chunk.toLong(),seed)); }
    static  class Pending extends SavedData {
        final it.unimi.dsi.fastutil.longs.LongOpenHashSet completed=new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        final LinkedHashMap<Long,Long> entries=new LinkedHashMap<>();CompoundTag unreadable;
        static Pending load(CompoundTag tag) {
            var p=new Pending();try {
                if(tag.contains("Completed")&&!tag.contains("Completed",Tag.TAG_LONG_ARRAY))throw new IllegalArgumentException("Malformed completed aura chunks");
                p.completed.addAll(it.unimi.dsi.fastutil.longs.LongArrayList.wrap(tag.getLongArray("Completed")));
                if(!(tag.get("Candidates") instanceof ListTag list)||!list.isEmpty()&&list.getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("Malformed natural aura queue");
                for(int i=0;i<list.size();i++){var row=list.getCompound(i);if(!row.contains("Chunk",Tag.TAG_LONG)||!row.contains("Seed",Tag.TAG_LONG)||p.entries.putIfAbsent(row.getLong("Chunk"),row.getLong("Seed"))!=null)throw new IllegalArgumentException("Invalid natural aura candidate");}
            }catch(RuntimeException failure){p.entries.clear();p.unreadable=tag.copy();com.mojang.logging.LogUtils.getLogger().error("Natural aura queue unreadable; preserving original data",failure);}return p;
        }
        @Override public CompoundTag save(CompoundTag tag){if(unreadable!=null)return unreadable.copy();var list=new ListTag();entries.forEach((chunk,seed)->{var row=new CompoundTag();row.putLong("Chunk",chunk);row.putLong("Seed",seed);list.add(row);});tag.put("Candidates",list);tag.putLongArray("Completed",completed.toLongArray());return tag;}
    }
    private static Pending drain(ServerLevel level,int budget) {
        var p=level.getDataStorage().computeIfAbsent(Pending::load,Pending::new,"thaumcraft_natural_aura");if(p.unreadable!=null)return p;
        var queue=INCOMING.get(level);
        if(queue!=null)for(int i=0;i<budget;i++) {
            var incoming=queue.poll();if(incoming==null)break;
            if(!p.completed.contains(incoming.chunk()) && p.entries.putIfAbsent(incoming.chunk(),incoming.seed())==null)p.setDirty();
        }
        return p;
    }
    public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        for(var level:event.getServer().getAllLevels()) {
            var pending=drain(level,256);if(pending.unreadable!=null||!TempleAuraMigration.ready(level))continue;
            int probes=Math.min(PROBES_PER_TICK,pending.entries.size());
            for(int i=0;i<probes;i++) {
                var first=pending.entries.entrySet().iterator();
                var row=first.next();
                long key=row.getKey(),seed=row.getValue();
                if(!pending.completed.contains(key)) {
                    var chunk=new ChunkPos(key);
                    if(!neighborhoodLoaded(level,chunk)) {
                        first.remove();
                        pending.entries.put(key,seed);
                        continue;
                    }
                    NaturalAuraStructures.generate(level,chunk,net.minecraft.util.RandomSource.create(seed),true);
                    pending.completed.add(key);
                }
                first.remove();pending.setDirty();break;
            }
        }
    }
    private static boolean neighborhoodLoaded(ServerLevel level,ChunkPos chunk) {
        for(int x=chunk.x-1;x<=chunk.x+2;x++)for(int z=chunk.z-1;z<=chunk.z+2;z++)
            if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }
    public static void stopping(net.minecraftforge.event.server.ServerStoppingEvent event){for(var level:event.getServer().getAllLevels())drain(level,Integer.MAX_VALUE);}
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){INCOMING.keySet().removeIf(level->level.getServer()==event.getServer());}
}
