package tc4revival.content;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.level.ChunkDataEvent;
import tc4revival.config.TCConfig;
/** Old save marker and seed formula, with durable server-thread regeneration. */
public  class ChunkRegeneration {
    private record Incoming(ServerLevel level,ChunkPos chunk) {}
    private static final ConcurrentLinkedQueue<Incoming> INCOMING=new ConcurrentLinkedQueue<>();
    private ChunkRegeneration() {}
    static boolean enabled(){for(var feature:TCConfig.WorldFeature.values())if(feature!=TCConfig.WorldFeature.TAINT&&TCConfig.SERVER.regenerates(feature))return true;return false;}
    static boolean eligible(CompoundTag data,String key,boolean enabled){return enabled&&!data.getCompound("Thaumcraft").contains(key);}
    static long seed(long worldSeed,ChunkPos chunk){var random=new java.util.Random(worldSeed);long x=random.nextLong()>>3,z=random.nextLong()>>3;return x*chunk.x+z*chunk.z^worldSeed;}
    public static void load(ChunkDataEvent.Load event){
        if(event.getStatus()!=net.minecraft.world.level.chunk.ChunkStatus.ChunkType.LEVELCHUNK||!eligible(event.getData(),TCConfig.SERVER.regenerationKey.get(),enabled()))return;
        var chunk=event.getChunk();if(chunk instanceof net.minecraft.world.level.chunk.ImposterProtoChunk wrapper)chunk=wrapper.getWrapped();
        if(chunk.getWorldForge() instanceof ServerLevel level)INCOMING.add(new Incoming(level,chunk.getPos()));
    }
    public static void save(ChunkDataEvent.Save event){
        if(!(event.getLevel() instanceof ServerLevel))return;var marker=new CompoundTag();marker.putBoolean(TCConfig.SERVER.regenerationKey.get(),true);event.getData().put("Thaumcraft",marker);
    }
    static NaturalAuraQueue.Pending drain(ServerLevel level){
        var pending=level.getDataStorage().computeIfAbsent(NaturalAuraQueue.Pending::load,NaturalAuraQueue.Pending::new,"thaumcraft_regeneration");if(pending.unreadable!=null)return pending;
        for(var item:INCOMING)if(item.level()==level&&INCOMING.remove(item)){pending.entries.putIfAbsent(item.chunk().toLong(),seed(level.getSeed(),item.chunk()));pending.setDirty();}return pending;
    }
    public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        for(var level:event.getServer().getAllLevels())process(level);
    }
    static void process(ServerLevel level){
            var pending=drain(level);if(pending.unreadable!=null||!TempleAuraMigration.ready(level))return;int count=0;
            for(var iterator=pending.entries.entrySet().iterator();iterator.hasNext()&&count<10;){
                var row=iterator.next();var chunk=new ChunkPos(row.getKey());boolean loaded=true;
                for(int x=chunk.x-1;x<=chunk.x+2;x++)for(int z=chunk.z-1;z<=chunk.z+2;z++)if(level.getChunkSource().getChunkNow(x,z)==null)loaded=false;
                if(!loaded)continue;var random=net.minecraft.util.RandomSource.create(row.getValue());
                LegacyVegetationFeature.decorate(level,chunk,random,false);NaturalAuraStructures.generate(level,chunk,random,false);
                level.getChunkSource().getChunkNow(chunk.x,chunk.z).setUnsaved(true);iterator.remove();pending.setDirty();count++;
            }
    }
    public static void stopping(net.minecraftforge.event.server.ServerStoppingEvent event){for(var level:event.getServer().getAllLevels())drain(level);}
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){INCOMING.removeIf(i->i.level().getServer()==event.getServer());}
}
