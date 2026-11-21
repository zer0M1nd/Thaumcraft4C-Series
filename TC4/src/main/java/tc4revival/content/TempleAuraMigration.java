package tc4revival.content;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
/** Reads existing region headers and uses Minecraft's I/O worker; no chunk tickets or terrain generation. */
public  class TempleAuraMigration {
    private static final Map<ServerLevel,Scan> ACTIVE=new WeakHashMap<>();
    private static final Set<ServerLevel> ENUM_FAILED=Collections.newSetFromMap(new WeakHashMap<>());
    private TempleAuraMigration(){}
    static boolean applies(ServerLevel level){return !level.dimension().equals(Level.NETHER)&&!level.dimension().equals(Level.END)&&!level.dimension().equals(TCEldritch.OUTER_LANDS);}
    public static boolean ready(ServerLevel level){if(!applies(level))return true;var index=TempleAura.index(level);return index.unreadable==null&&index.diskScanComplete;}
    static List<ChunkPos> positions(Path region)throws IOException{
        var match=java.util.regex.Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca").matcher(region.getFileName().toString());
        if(!match.matches())throw new IOException("Invalid region filename "+region);int rx=Integer.parseInt(match.group(1)),rz=Integer.parseInt(match.group(2));
        var bytes=ByteBuffer.allocate(4096);try(var channel=FileChannel.open(region,StandardOpenOption.READ)){
            // Minecraft opens region files before the first chunk write; these empty files
            // have no historical structures. Live chunk observations cover subsequent writes.
            if(channel.size()==0)return List.of();
            while(bytes.hasRemaining())if(channel.read(bytes)<0)throw new EOFException("Truncated region header "+region);
        }
        bytes.flip();var result=new ArrayList<ChunkPos>();for(int slot=0;slot<1024;slot++){int entry=bytes.getInt();if(entry==0)continue;if((entry>>>8)<2||(entry&255)==0)throw new IOException("Invalid chunk sector entry in "+region);result.add(new ChunkPos(Math.addExact(Math.multiplyExact(rx,32),slot%32),Math.addExact(Math.multiplyExact(rz,32),slot/32)));}return result;
    }
    static void accept(CompoundTag chunk,TempleAura.Index index){
        var root=chunk.contains("Level",Tag.TAG_COMPOUND)?chunk.getCompound("Level"):chunk;var structures=root.getCompound(root.contains("structures",Tag.TAG_COMPOUND)?"structures":"Structures");var starts=structures.getCompound(structures.contains("starts",Tag.TAG_COMPOUND)?"starts":"Starts");
        for(String name:starts.getAllKeys()){
            var id=net.minecraft.resources.ResourceLocation.tryParse(name);if(!TempleAura.temple(id))continue;var start=starts.getCompound(name);if(start.getString("id").equals("INVALID"))continue;
            if(!name.equals(start.getString("id"))||!start.contains("ChunkX",Tag.TAG_INT)||!start.contains("ChunkZ",Tag.TAG_INT))throw new IllegalArgumentException("Malformed temple start "+name);
            var children=start.getList("Children",Tag.TAG_COMPOUND);if(children.isEmpty())throw new IllegalArgumentException("Missing temple pieces "+name);int[] box=children.getCompound(0).getIntArray("BB");
            if(box.length!=6||box[3]<box[0]||box[4]<box[1]||box[5]<box[2])throw new IllegalArgumentException("Invalid temple bounds "+name);
            var center=TempleAura.center(new net.minecraft.world.level.levelgen.structure.BoundingBox(box[0],box[1],box[2],box[3],box[4],box[5]));String key=name+"@"+ChunkPos.asLong(start.getInt("ChunkX"),start.getInt("ChunkZ"));
            // A live chunk observation is newer than a disk snapshot from before its last save.
            if(!index.observed.contains(key)&&!center.equals(index.centers.put(key,center)))index.setDirty();
        }
    }
    static  class Scan {
        final Iterator<Path> regions;Iterator<ChunkPos> chunks=Collections.emptyIterator();final Function<ChunkPos,CompletableFuture<Optional<CompoundTag>>> reader;
        final List<CompletableFuture<Optional<CompoundTag>>> pending=new ArrayList<>();boolean exhausted,failed,done;int read;
        Scan(Path folder,Function<ChunkPos,CompletableFuture<Optional<CompoundTag>>> reader)throws IOException{
            this.reader=reader;if(!Files.isDirectory(folder)){regions=Collections.emptyIterator();return;}
            try(var files=Files.list(folder)){regions=files.filter(p->p.getFileName().toString().matches("r\\.-?\\d+\\.-?\\d+\\.mca")).sorted().toList().iterator();}
        }
        void step(TempleAura.Index index){
            if(failed||done)return;try{
                for(var iterator=pending.iterator();iterator.hasNext();){var future=iterator.next();if(!future.isDone())continue;future.join().ifPresent(tag->accept(tag,index));iterator.remove();read++;}
                while(pending.size()<32&&!exhausted){
                    if(!chunks.hasNext()){if(!regions.hasNext()){exhausted=true;break;}chunks=positions(regions.next()).iterator();if(!chunks.hasNext())continue;}
                    pending.add(reader.apply(chunks.next()));
                }
                if(exhausted&&pending.isEmpty()){done=true;index.diskScanComplete=true;index.setDirty();}
            }catch(Exception failure){failed=true;com.mojang.logging.LogUtils.getLogger().error("Temple disk indexing failed; partial index retained and completion left false for retry next start",failure);}
        }
    }
    public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        for(var level:event.getServer().getAllLevels()){
            if(!applies(level)||ENUM_FAILED.contains(level))continue;var index=TempleAura.index(level);if(index.unreadable!=null||index.diskScanComplete)continue;
            var scan=ACTIVE.get(level);if(scan==null){try{var root=level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);var region=net.minecraft.world.level.dimension.DimensionType.getStorageFolder(level.dimension(),root).resolve("region");scan=new Scan(region,level.getChunkSource().chunkMap::read);ACTIVE.put(level,scan);}catch(IOException failure){ENUM_FAILED.add(level);com.mojang.logging.LogUtils.getLogger().error("Cannot enumerate temple region files; retry next start",failure);continue;}}
            scan.step(index);if(scan.done){com.mojang.logging.LogUtils.getLogger().info("Temple disk index complete: dimension={}, chunks={}, temples={}",level.dimension().location(),scan.read,index.centers.size());ACTIVE.remove(level);}
        }
    }
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){ACTIVE.keySet().removeIf(level->level.getServer()==event.getServer());ENUM_FAILED.removeIf(level->level.getServer()==event.getServer());}
}
