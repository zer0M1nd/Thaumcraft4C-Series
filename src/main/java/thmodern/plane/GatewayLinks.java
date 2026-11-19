package thmodern.plane;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
/** Four exclusive gateway slots per allocation; stale holders cannot release a newer binding. */
public final class GatewayLinks extends SavedData {
 public record Link(UUID id,int plane,UUID allocation,int side,ResourceLocation dimension,BlockPos slot,boolean xAligned){public Link{Objects.requireNonNull(id);Objects.requireNonNull(allocation);Objects.requireNonNull(dimension);slot=slot.immutable();if(plane<0||side<0||side>3)throw new IllegalArgumentException("Invalid portal link");}}
 private final Map<UUID,Link> links=new LinkedHashMap<>();
 public static GatewayLinks get(MinecraftServer server){if(!server.isSameThread())throw new IllegalStateException("Gateway edits require the server thread");return server.overworld().getDataStorage().computeIfAbsent(GatewayLinks::load,GatewayLinks::new,"thaumichorizons_gateways");}
 public Optional<Link> get(UUID id){return Optional.ofNullable(links.get(id));}
 public Optional<Link> at(int plane,UUID allocation,int side){return links.values().stream().filter(l->l.plane==plane&&l.allocation.equals(allocation)&&l.side==side).findFirst();}
 public Optional<Link> reserve(UUID request,PocketPlanes.Plane plane,ResourceLocation dimension,BlockPos slot,boolean xAligned){
  if(!plane.ready())return Optional.empty();var existing=links.get(request);if(existing!=null)return existing.plane==plane.id()&&existing.allocation.equals(plane.allocation())&&existing.dimension.equals(dimension)&&existing.slot.equals(slot)&&existing.xAligned==xAligned?Optional.of(existing):Optional.empty();
  if(links.values().stream().anyMatch(l->l.dimension.equals(dimension)&&l.slot.equals(slot)))return Optional.empty();
  for(int side=0;side<4;side++)if(at(plane.id(),plane.allocation(),side).isEmpty()){var link=new Link(request,plane.id(),plane.allocation(),side,dimension,slot,xAligned);links.put(request,link);setDirty();return Optional.of(link);}return Optional.empty();
 }
 public boolean release(UUID id){if(links.remove(id)==null)return false;setDirty();return true;}
 @Override public CompoundTag save(CompoundTag tag){var list=new ListTag();for(var link:links.values()){var row=new CompoundTag();row.putUUID("Id",link.id);row.putInt("Plane",link.plane);row.putUUID("Allocation",link.allocation);row.putInt("Side",link.side);row.putString("Dimension",link.dimension.toString());row.putLong("Slot",link.slot.asLong());row.putBoolean("XAligned",link.xAligned);list.add(row);}tag.put("Links",list);return tag;}
 public static GatewayLinks load(CompoundTag tag){var data=new GatewayLinks();var list=tag.getList("Links",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){var row=list.getCompound(i);var link=new Link(row.getUUID("Id"),row.getInt("Plane"),row.getUUID("Allocation"),row.getInt("Side"),ResourceLocation.parse(row.getString("Dimension")),BlockPos.of(row.getLong("Slot")),row.getBoolean("XAligned"));if(data.links.containsKey(link.id)||data.at(link.plane,link.allocation,link.side).isPresent()||data.links.values().stream().anyMatch(l->l.dimension.equals(link.dimension)&&l.slot.equals(link.slot)))throw new IllegalArgumentException("Duplicate gateway binding");data.links.put(link.id,link);}return data;}
}
