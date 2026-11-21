package thmodern.plane;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thmodern.Horizons;
/** Durable outbox for a pearl whose source block was removed before generation committed. */
@Mod.EventBusSubscriber(modid=Horizons.ID)
public final class PlaneRefunds extends SavedData {
 private record Refund(UUID request,ResourceLocation dimension,BlockPos pos,ItemStack stack){}
 private final Map<UUID,Refund> pending=new LinkedHashMap<>();
 public static PlaneRefunds get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(PlaneRefunds::load,PlaneRefunds::new,"thaumichorizons_plane_refunds");}
 public int pendingCount(){return pending.size();}
 public void enqueue(UUID request,ResourceLocation dimension,BlockPos pos,ItemStack stack){if(stack.isEmpty())return;if(pending.putIfAbsent(request,new Refund(request,dimension,pos.immutable(),stack.copy()))==null)setDirty();}
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){if(event.phase==TickEvent.Phase.END)get(event.getServer()).deliver(event.getServer());}
 public void deliver(MinecraftServer server){for(var refund:List.copyOf(pending.values())){
  var level=server.getLevel(ResourceKey.create(Registries.DIMENSION,refund.dimension));if(level==null||!level.hasChunkAt(refund.pos))continue;
  UUID entityId=UUID.nameUUIDFromBytes((refund.request+":pearl-refund").getBytes(java.nio.charset.StandardCharsets.UTF_8));
  if(level.getEntity(entityId)==null){var item=new ItemEntity(level,refund.pos.getX()+.5,refund.pos.getY()+.5,refund.pos.getZ()+.5,refund.stack.copy());item.setUUID(entityId);item.setInvulnerable(true);item.setUnlimitedLifetime();item.setDefaultPickUpDelay();if(!level.addFreshEntity(item))continue;}
  pending.remove(refund.request);setDirty();break;
 }}
 @Override public CompoundTag save(CompoundTag tag){var list=new ListTag();for(var refund:pending.values()){var row=new CompoundTag();row.putUUID("Request",refund.request);row.putString("Dimension",refund.dimension.toString());row.putLong("Position",refund.pos.asLong());row.put("Stack",refund.stack.save(new CompoundTag()));list.add(row);}tag.put("Refunds",list);return tag;}
 public static PlaneRefunds load(CompoundTag tag){var data=new PlaneRefunds();var list=tag.getList("Refunds",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){var row=list.getCompound(i);data.enqueue(row.getUUID("Request"),ResourceLocation.parse(row.getString("Dimension")),BlockPos.of(row.getLong("Position")),ItemStack.of(row.getCompound("Stack")));}return data;}
}
