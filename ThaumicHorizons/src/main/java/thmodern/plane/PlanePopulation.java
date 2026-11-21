package thmodern.plane;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
/** Successful generation slots persist independently of live mobs; deaths never refill the generation quota. */
public final class PlanePopulation extends SavedData {
 private final Map<Integer,Set<UUID>> spawned=new HashMap<>();
 public static PlanePopulation get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(PlanePopulation::load,PlanePopulation::new,"thaumichorizons_plane_population");}
 public boolean contains(int plane,UUID slot){return spawned.getOrDefault(plane,Set.of()).contains(slot);}
 public int count(int plane){return spawned.getOrDefault(plane,Set.of()).size();}
 public void record(int plane,UUID slot){if(spawned.computeIfAbsent(plane,k->new HashSet<>()).add(slot))setDirty();}
 @Override public CompoundTag save(CompoundTag tag){var rows=new ListTag();spawned.forEach((id,slots)->{var row=new CompoundTag();row.putInt("Plane",id);var list=new ListTag();slots.forEach(uuid->{var item=new CompoundTag();item.putUUID("Slot",uuid);list.add(item);});row.put("Slots",list);rows.add(row);});tag.put("Planes",rows);return tag;}
 public static PlanePopulation load(CompoundTag tag){var result=new PlanePopulation();var rows=tag.getList("Planes",Tag.TAG_COMPOUND);for(int i=0;i<rows.size();i++){var row=rows.getCompound(i);var slots=row.getList("Slots",Tag.TAG_COMPOUND);for(int j=0;j<slots.size();j++)result.record(row.getInt("Plane"),slots.getCompound(j).getUUID("Slot"));}return result;}
}
