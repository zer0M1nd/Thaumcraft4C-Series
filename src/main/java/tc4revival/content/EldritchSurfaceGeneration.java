package tc4revival.content;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import tc4revival.config.TCConfig;

/** World-generation workers only enqueue values; the tick thread persists and processes candidates. */
public  class EldritchSurfaceGeneration {
    private record Candidate(BlockPos column,int width,int height,long seed) {}
    private record Incoming(ServerLevel level,Candidate candidate) {}
    private static final ConcurrentLinkedQueue<Incoming> INCOMING=new ConcurrentLinkedQueue<>();
    private EldritchSurfaceGeneration() {}
    public static void enqueue(ServerLevel level,BlockPos column,int width,int height,long seed) {
        INCOMING.add(new Incoming(level,new Candidate(column.immutable(),width,height,seed)));
    }
    private static  class Pending extends SavedData {
        final Map<Long,Candidate> entries=new LinkedHashMap<>();
        CompoundTag unreadable;
        static Pending load(CompoundTag tag) {
            var result=new Pending();
            try {
                if (!(tag.get("Candidates") instanceof ListTag list) || !list.isEmpty() && list.getElementType()!=Tag.TAG_COMPOUND)
                    throw new IllegalArgumentException("Malformed natural ring candidates");
                for (int i=0;i<list.size();i++) {
                    var entry=list.getCompound(i);
                    if (!entry.contains("Column",Tag.TAG_LONG) || !entry.contains("Seed",Tag.TAG_LONG)
                            || !entry.contains("Width",Tag.TAG_INT) || !entry.contains("Height",Tag.TAG_INT))
                        throw new IllegalArgumentException("Incomplete natural ring candidate");
                    BlockPos pos=BlockPos.of(entry.getLong("Column")); int width=entry.getInt("Width"),height=entry.getInt("Height");
                    if (width<7 || width>63 || height<7 || height>63 || (width&1)==0 || (height&1)==0)
                        throw new IllegalArgumentException("Invalid natural ring dimensions");
                    if (result.entries.putIfAbsent(ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4),new Candidate(pos,width,height,entry.getLong("Seed")))!=null)
                        throw new IllegalArgumentException("Duplicate natural ring candidate");
                }
            } catch (RuntimeException failure) {
                result.entries.clear(); result.unreadable=tag.copy();
                com.mojang.logging.LogUtils.getLogger().error("Unable to read natural eldritch ring candidates; original data preserved",failure);
            }
            return result;
        }
        @Override public CompoundTag save(CompoundTag tag) {
            if (unreadable!=null) return unreadable.copy();
            var list=new ListTag();
            for (Candidate candidate:entries.values()) {
                var entry=new CompoundTag(); entry.putLong("Column",candidate.column().asLong()); entry.putLong("Seed",candidate.seed());
                entry.putInt("Width",candidate.width()); entry.putInt("Height",candidate.height()); list.add(entry);
            }
            tag.put("Candidates",list); return tag;
        }
    }
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return;
        var level=event.getServer().overworld();
        var pending=drain(level);
        if (pending.unreadable!=null) return;
        if (!TCConfig.SERVER.generates(TCConfig.WorldFeature.STRUCTURES)) return;
        for (var iterator=pending.entries.values().iterator();iterator.hasNext();) {
            var candidate=iterator.next(); BlockPos pos=candidate.column();
            boolean loaded=true;
            for (int cx=(pos.getX()-8)>>4;cx<=(pos.getX()+8)>>4;cx++) for (int cz=(pos.getZ()-8)>>4;cz<=(pos.getZ()+8)>>4;cz++)
                if (level.getChunkSource().getChunkNow(cx,cz)==null) loaded=false;
            if (!loaded) continue;
            int y=LegacySurfaceHeight.at(level,pos.getX(),pos.getZ())-1;
            EldritchRing.generate(level,new BlockPos(pos.getX(),y,pos.getZ()),candidate.width(),candidate.height(),RandomSource.create(candidate.seed()));
            iterator.remove(); pending.setDirty(); break;
        }
    }
    private static Pending drain(ServerLevel level) {
        var pending=level.getDataStorage().computeIfAbsent(Pending::load,Pending::new,"thaumcraft_surface_rings");
        if (pending.unreadable!=null) return pending;
        for (Incoming incoming:INCOMING) {
            if (incoming.level().getServer()!=level.getServer() || !INCOMING.remove(incoming)) continue;
            var candidate=incoming.candidate(); BlockPos pos=candidate.column();
            pending.entries.putIfAbsent(ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4),candidate); pending.setDirty();
        }
        return pending;
    }
    public static void stopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        // Preserve candidates already enqueued since the last END tick before normal shutdown saves.
        drain(event.getServer().overworld());
    }
    public static void stopped(ServerStoppedEvent event) { INCOMING.removeIf(incoming->incoming.level().getServer()==event.getServer()); }
}
