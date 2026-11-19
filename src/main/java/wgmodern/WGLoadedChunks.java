package wgmodern;
import java.util.*;import net.minecraft.world.level.Level;import net.minecraft.world.level.chunk.LevelChunk;import net.minecraftforge.event.level.ChunkEvent;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraft.core.BlockPos;
public class WGLoadedChunks {
 private static final Map<Level,Set<LevelChunk>> CHUNKS=Collections.synchronizedMap(new WeakHashMap<>());
 @SubscribeEvent public static void load(ChunkEvent.Load e){if(e.getChunk() instanceof LevelChunk c&&!c.getLevel().isClientSide)CHUNKS.computeIfAbsent(c.getLevel(),k->java.util.concurrent.ConcurrentHashMap.newKeySet()).add(c);}
 @SubscribeEvent public static void unload(ChunkEvent.Unload e){if(e.getChunk() instanceof LevelChunk c){var set=CHUNKS.get(c.getLevel());if(set!=null)set.remove(c);}}
 public static tc4revival.content.AuraNodeBlockEntity nearest(Level level,BlockPos query){var chunks=CHUNKS.get(level);if(chunks==null)return null;tc4revival.content.AuraNodeBlockEntity result=null;double distance=Double.MAX_VALUE;for(var c:chunks)for(var be:c.getBlockEntities().values())if(be instanceof tc4revival.content.AuraNodeBlockEntity node&&!node.isRemoved()){double d=query.distSqr(node.getBlockPos());if(d<distance){distance=d;result=node;}}return result;}
}
