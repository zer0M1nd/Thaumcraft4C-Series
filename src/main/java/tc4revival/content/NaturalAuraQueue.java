package tc4revival.content;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
/** Generation workers enqueue immutable candidates; world writes happen on the server tick thread. */
public  class NaturalAuraQueue {
    private record Incoming(ServerLevel level,long chunk,long seed) {}
    private static final ConcurrentLinkedQueue<Incoming> INCOMING=new ConcurrentLinkedQueue<>();
    private NaturalAuraQueue() {}
    public static void enqueue(ServerLevel level,ChunkPos chunk,long seed) { INCOMING.add(new Incoming(level,chunk.toLong(),seed)); }
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
    private static Pending drain(ServerLevel level) {
        var p=level.getDataStorage().computeIfAbsent(Pending::load,Pending::new,"thaumcraft_natural_aura");if(p.unreadable!=null)return p;
        for(var incoming:INCOMING)if(incoming.level()==level&&INCOMING.remove(incoming)){p.entries.putIfAbsent(incoming.chunk(),incoming.seed());p.setDirty();}return p;
    }
    public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        for(var level:event.getServer().getAllLevels()) {
            var pending=drain(level);if(pending.unreadable!=null||!TempleAuraMigration.ready(level))continue;
            for(var iterator=pending.entries.entrySet().iterator();iterator.hasNext();) {
                var row=iterator.next();var chunk=new ChunkPos(row.getKey());boolean loaded=true;
                // Random offsets plus the 19x19 mound footprint and node survey can cross these chunks.
                for(int x=chunk.x-1;x<=chunk.x+2;x++)for(int z=chunk.z-1;z<=chunk.z+2;z++)if(level.getChunkSource().getChunkNow(x,z)==null)loaded=false;
                if(!loaded)continue;
                // Keep completed chunks after consuming their pending entry, including across reloads.
                if(!pending.completed.contains(row.getKey().longValue())){
                    NaturalAuraStructures.generate(level,chunk,net.minecraft.util.RandomSource.create(row.getValue()),true);
                    pending.completed.add(row.getKey().longValue());
                }
                iterator.remove();pending.setDirty();break;
            }
        }
    }
    public static void stopping(net.minecraftforge.event.server.ServerStoppingEvent event){for(var level:event.getServer().getAllLevels())drain(level);}
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){INCOMING.removeIf(i->i.level().getServer()==event.getServer());}
}
