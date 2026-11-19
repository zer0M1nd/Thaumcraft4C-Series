package timodern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** World-position data; ordinary blocks and their block entities remain untouched. */
public class InfusionData extends SavedData {
    public static class Entry {
        public final LinkedHashSet<String> aspects=new LinkedHashSet<>();
        public final CompoundTag settings=new CompoundTag();
        public boolean has(String id){return InfusionConfig.enabled(id)&&aspects.stream().anyMatch(a->InfusionEffects.local(a).equals(id));}
        public CompoundTag save(){var tag=new CompoundTag();var list=new ListTag();aspects.forEach(a->list.add(StringTag.valueOf(a)));tag.put("aspects",list);tag.put("settings",settings.copy());return tag;}
        public static Entry load(CompoundTag tag){var e=new Entry();tag.getList("aspects",Tag.TAG_STRING).forEach(a->{if(InfusionEffects.get(a.getAsString())!=null)e.aspects.add(a.getAsString());});e.settings.merge(tag.getCompound("settings"));return e;}
    }
    public final Map<Long,Entry> entries=new LinkedHashMap<>();
    public static final Map<Long,Entry> CLIENT=new java.util.concurrent.ConcurrentHashMap<>();
    public static InfusionData get(ServerLevel level){return level.getDataStorage().computeIfAbsent(InfusionData::load,InfusionData::new,"thaumicinfusion");}
    public static Entry at(Level level,BlockPos pos){return level==null||pos==null?null:level instanceof ServerLevel server?get(server).entries.get(pos.asLong()):CLIENT.get(pos.asLong());}
    public static boolean has(net.minecraft.world.level.BlockGetter level,BlockPos pos,String aspect){
        // Shape precomputation (including Canary pistons) has no world position to infuse.
        if(level==null||pos==null)return false;
        Entry e=level instanceof Level l?at(l,pos):level instanceof net.minecraft.world.level.chunk.LevelChunk c?at(c.getLevel(),pos):CLIENT.get(pos.asLong());return e!=null&&e.has(aspect);
    }
    public static InfusionData load(CompoundTag tag){var d=new InfusionData();for(var t:tag.getList("blocks",Tag.TAG_COMPOUND)){var row=(CompoundTag)t;var e=Entry.load(row);if(!e.aspects.isEmpty())d.entries.put(row.getLong("pos"),e);}return d;}
    @Override public CompoundTag save(CompoundTag tag){var list=new ListTag();entries.forEach((p,e)->{var t=e.save();t.putLong("pos",p);list.add(t);});tag.put("blocks",list);return tag;}
    public static void changed(ServerLevel level,BlockPos pos){var data=get(level);data.setDirty();InfusionNetwork.sync(level,pos,data.entries.get(pos.asLong()));level.getChunkSource().getLightEngine().checkBlock(pos);level.updateNeighborsAt(pos,level.getBlockState(pos).getBlock());}
    public static void remove(ServerLevel level,BlockPos pos){if(get(level).entries.remove(pos.asLong())!=null)changed(level,pos);}
}
