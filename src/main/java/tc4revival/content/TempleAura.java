package tc4revival.content;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
/** Dimension-local persisted structure index and node-generation deduplication. */
public  class TempleAura {
    private record Incoming(ServerLevel level,String key,BlockPos center){}
    private static final ConcurrentLinkedQueue<Incoming> INCOMING=new ConcurrentLinkedQueue<>();
    private TempleAura(){}
    static boolean temple(ResourceLocation id){return id!=null&&id.getNamespace().equals("minecraft")&&Set.of("desert_pyramid","jungle_pyramid","swamp_hut").contains(id.getPath());}
    static BlockPos center(BoundingBox box){return new BlockPos(box.minX()+box.getXSpan()/2,box.minY()+box.getYSpan()/2,box.minZ()+box.getZSpan()/2);}
    static int hash(BlockPos pos){return pos.getX()*8976890+pos.getY()*981131+pos.getZ();}
    static Optional<BlockPos> nearest(Collection<BlockPos> centers,BlockPos query){
        double best=Double.MAX_VALUE;BlockPos result=null;
        for(var pos:centers){double distance=pos.distSqr(query);if(distance<best){best=distance;result=pos;}}
        return Optional.ofNullable(result);
    }
    static  class Index extends SavedData {
        final Map<String,BlockPos> centers=new LinkedHashMap<>();final Set<String> observed=new HashSet<>();
        final Set<Long> attempted=new HashSet<>();CompoundTag unreadable;boolean diskScanComplete;
        static Index load(CompoundTag tag){var index=new Index();index.diskScanComplete=tag.getBoolean("DiskScanComplete");
            for(long pos:tag.getLongArray("AttemptedNodes"))index.attempted.add(pos);try{
            if(!(tag.get("Temples") instanceof ListTag list)||!list.isEmpty()&&list.getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("Invalid temple index");
            for(int i=0;i<list.size();i++){var row=list.getCompound(i);if(!row.contains("Key",Tag.TAG_STRING)||!row.contains("Center",Tag.TAG_LONG)||index.centers.putIfAbsent(row.getString("Key"),BlockPos.of(row.getLong("Center")))!=null)throw new IllegalArgumentException("Invalid temple record");}
        }catch(RuntimeException failure){index.centers.clear();index.unreadable=tag.copy();com.mojang.logging.LogUtils.getLogger().error("Temple index unreadable; retaining original data",failure);}return index;}
        @Override public CompoundTag save(CompoundTag tag){if(unreadable!=null)return unreadable.copy();var list=new ListTag();centers.forEach((key,pos)->{var row=new CompoundTag();row.putString("Key",key);row.putLong("Center",pos.asLong());list.add(row);});tag.put("Temples",list);tag.putBoolean("DiskScanComplete",diskScanComplete);tag.putLongArray("AttemptedNodes",attempted.stream().mapToLong(Long::longValue).toArray());return tag;}
    }
    public static void chunkLoaded(net.minecraftforge.event.level.ChunkEvent.Load event){
        var chunk=event.getChunk();if(chunk instanceof net.minecraft.world.level.chunk.ImposterProtoChunk wrapper)chunk=wrapper.getWrapped();
        if(!(chunk.getWorldForge() instanceof ServerLevel level))return;var registry=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        for(var row:chunk.getAllStarts().entrySet()){var id=registry.getKey(row.getKey());var start=row.getValue();if(!temple(id)||!start.isValid()||start.getPieces().isEmpty())continue;
            INCOMING.add(new Incoming(level,id+"@"+start.getChunkPos().toLong(),center(start.getPieces().get(0).getBoundingBox())));
        }
    }
    static Index index(ServerLevel level){var index=level.getDataStorage().computeIfAbsent(Index::load,Index::new,"thaumcraft_temple_index");if(index.unreadable!=null)return index;
        for(var row:INCOMING)if(row.level()==level&&INCOMING.remove(row)){index.observed.add(row.key());if(!row.center().equals(index.centers.put(row.key(),row.center())))index.setDirty();}return index;
    }
    public static boolean generate(ServerLevel level,ChunkPos chunk,RandomSource random){
        var index=index(level);if(index.unreadable!=null)return false;
        var centers=index.centers;var query=new BlockPos(chunk.getMinBlockX()+8,LegacySurfaceHeight.at(level,chunk.getMinBlockX()+8,chunk.getMinBlockZ()+8),chunk.getMinBlockZ()+8);
        var closest=nearest(centers.values(),query);if(closest.isEmpty())return false;var center=closest.get();
        if(!new ChunkPos(center).equals(chunk)||!level.hasChunkAt(center)||!index.attempted.add(center.asLong()))return false;
        index.setDirty();
        var target=new BlockPos(center.getX(),LegacySurfaceHeight.at(level,center.getX(),center.getZ())+3,center.getZ());AuraNodeGeneration.createRandom(level,target,random,false,false,false);return true;
    }
    public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event){if(event.phase==net.minecraftforge.event.TickEvent.Phase.END)for(var level:event.getServer().getAllLevels())index(level);}
    public static void stopping(net.minecraftforge.event.server.ServerStoppingEvent event){for(var level:event.getServer().getAllLevels())index(level);}
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){INCOMING.removeIf(row->row.level().getServer()==event.getServer());}
}
