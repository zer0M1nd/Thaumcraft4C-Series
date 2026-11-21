package thmodern.plane;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
/** Persistent plane allocation independent of loaded rifts; IDs are never recycled. */
public final class PocketPlanes extends SavedData {
 public static final ResourceLocation DIMENSION=ResourceLocation.parse("thaumichorizons:pocket_plane");
 public record Origin(ResourceLocation dimension,BlockPos pos){public Origin{pos=pos.immutable();}}
 public record Plane(int id,UUID allocation,String name,Map<String,Integer> aspects,int color,long seed,Origin origin,String stage,long cursor,boolean ready){
  public Plane{aspects=Collections.unmodifiableMap(new LinkedHashMap<>(aspects));Objects.requireNonNull(allocation);Objects.requireNonNull(origin);Objects.requireNonNull(stage);if(id<0||id>116999||cursor<0)throw new IllegalArgumentException("Invalid plane extent/progress");}
  public BlockPos center(){return new BlockPos(0,128,id*256);}public PlaneParameters parameters(){return new PlaneParameters(aspects);}
 }
 private final Map<Integer,Plane> planes=new LinkedHashMap<>();private int next;
 public static PocketPlanes get(MinecraftServer server){if(!server.isSameThread())throw new IllegalStateException("Plane edits belong to the server thread");return server.overworld().getDataStorage().computeIfAbsent(PocketPlanes::load,PocketPlanes::new,"thaumichorizons_planes");}
 public Optional<Plane> get(int id){return Optional.ofNullable(planes.get(id));}public List<Plane> all(){return List.copyOf(planes.values());}
 public Plane reserve(UUID allocation,String name,Map<String,Integer> aspects,int color,long seed,Origin origin){if(next>116999)throw new IllegalStateException("Pocket plane world border capacity exhausted");var existing=planes.values().stream().filter(p->p.allocation.equals(allocation)).findFirst();if(existing.isPresent())return existing.get();var plan=new Plane(next++,allocation,name,new PlaneParameters(aspects).aspects(),color,seed,origin,"biomes",0,false);planes.put(plan.id,plan);setDirty();return plan;}
 public void progress(int id,String stage,long cursor,boolean ready){var before=planes.get(id);if(before==null)throw new IllegalArgumentException("Unknown plane "+id);if(before.ready&&!ready)throw new IllegalStateException("Completed plane cannot restart generation");planes.put(id,new Plane(before.id,before.allocation,before.name,before.aspects,before.color,before.seed,before.origin,stage,cursor,ready));setDirty();}
 @Override public CompoundTag save(CompoundTag tag){tag.putInt("Version",1);tag.putInt("Next",next);var list=new ListTag();for(var p:planes.values()){var row=new CompoundTag();row.putInt("Id",p.id);row.putUUID("Allocation",p.allocation);row.putString("Name",p.name);row.putInt("Color",p.color);row.putLong("Seed",p.seed);row.putString("Dimension",p.origin.dimension.toString());row.putLong("Origin",p.origin.pos.asLong());row.putString("Stage",p.stage);row.putLong("Cursor",p.cursor);row.putBoolean("Ready",p.ready);var amounts=new CompoundTag();p.aspects.forEach(amounts::putInt);row.put("Aspects",amounts);list.add(row);}tag.put("Planes",list);return tag;}
 public static PocketPlanes load(CompoundTag tag){if(tag.contains("Version")&&tag.getInt("Version")!=1)throw new IllegalArgumentException("Unsupported pocket plane data version");var result=new PocketPlanes();var list=tag.getList("Planes",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){var row=list.getCompound(i);int id=row.getInt("Id");var amounts=new LinkedHashMap<String,Integer>();var saved=row.getCompound("Aspects");for(var key:saved.getAllKeys())if(saved.getInt(key)>0)amounts.put(key,saved.getInt(key));var plane=new Plane(id,row.getUUID("Allocation"),row.getString("Name"),amounts,row.getInt("Color"),row.getLong("Seed"),new Origin(ResourceLocation.parse(row.getString("Dimension")),BlockPos.of(row.getLong("Origin"))),row.getString("Stage"),row.getLong("Cursor"),row.getBoolean("Ready"));if(result.planes.putIfAbsent(id,plane)!=null)throw new IllegalArgumentException("Duplicate plane ID");result.next=Math.max(result.next,id+1);}result.next=Math.max(result.next,tag.getInt("Next"));return result;}
}
