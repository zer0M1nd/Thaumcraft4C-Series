package thmodern.plane;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thmodern.Horizons;
/** One job advances per server tick, with persistent stage-local cursors. */
@Mod.EventBusSubscriber(modid=Horizons.ID)
public final class PlaneGeneration {
 private static final Map<MinecraftServer,Job> ACTIVE=new IdentityHashMap<>();
 private static final org.slf4j.Logger LOG=com.mojang.logging.LogUtils.getLogger();
 private record Job(int id,PlaneStrataWriter layers,PlaneUndergroundWriter underground,PlaneEnvelopeWriter envelope){Job(PocketPlanes.Plane plane){this(plane.id(),new PlaneStrataWriter(plane),new PlaneUndergroundWriter(plane),new PlaneEnvelopeWriter(plane));}}
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){if(event.phase!=TickEvent.Phase.END||!thmodern.LegacyConfig.pocket())return;var server=event.getServer();var data=PocketPlanes.get(server);var pending=data.all().stream().filter(p->!p.ready()&&!p.stage().equals("cancelled")&&!p.stage().startsWith("failed/")).findFirst();if(pending.isEmpty()){ACTIVE.remove(server);return;}var plane=pending.get();var level=server.getLevel(ResourceKey.create(Registries.DIMENSION,PocketPlanes.DIMENSION));if(level==null)return;
  try{var job=ACTIVE.get(server);if(job==null||job.id()!=plane.id()){job=new Job(plane);ACTIVE.put(server,job);}advance(level,data,plane,job);}
  catch(RuntimeException ex){data.progress(plane.id(),"failed/"+plane.stage(),plane.cursor(),false);ACTIVE.remove(server);LOG.error("Pocket plane {} paused at {}:{}",plane.id(),plane.stage(),plane.cursor(),ex);}
 }
 private static void advance(ServerLevel level,PocketPlanes data,PocketPlanes.Plane plane,Job job){long cursor=plane.cursor(),next;
  switch(plane.stage()){
   case "arrival_portal"->{PlaneTravel.complete(level,data,plane);}
   case "biomes"->{job.envelope.biome(level,(int)cursor);next=cursor+1;data.progress(plane.id(),next>=job.envelope.biomeCount()?"layers":"biomes",next>=job.envelope.biomeCount()?0:next,false);}
   case "layers"->{next=job.layers.write(level,cursor,16);data.progress(plane.id(),next>=job.layers.size()?"caves":"layers",next>=job.layers.size()?0:next,false);}
   case "caves"->{if(cursor<job.underground.caveCount())job.underground.writeCave(level,(int)cursor);next=cursor+1;data.progress(plane.id(),next>=job.underground.caveCount()?"pockets":"caves",next>=job.underground.caveCount()?0:next,false);}
   case "pockets"->{next=Math.min(job.underground.pocketCount(),cursor+32);for(long i=cursor;i<next;i++)job.underground.writePocket(level,(int)i);data.progress(plane.id(),next>=job.underground.pocketCount()?"rings":"pockets",next>=job.underground.pocketCount()?0:next,false);}
   case "rings"->{next=job.envelope.rings(level,cursor,2048);data.progress(plane.id(),next>=job.envelope.ringCount()?"boundary":"rings",next>=job.envelope.ringCount()?0:next,false);}
   case "boundary"->{next=job.envelope.boundary(level,cursor,2048);data.progress(plane.id(),next>=job.envelope.boundaryCount()?"arrival_portal":"boundary",next>=job.envelope.boundaryCount()?0:next,false);}
   default->throw new IllegalStateException("Unknown generation stage "+plane.stage());
  }
 }
 public static void retry(MinecraftServer server,int id){var data=PocketPlanes.get(server);var plane=data.get(id).orElseThrow();if(plane.stage().startsWith("failed/")){data.progress(id,plane.stage().substring(7),plane.cursor(),false);ACTIVE.remove(server);}}
 @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){ACTIVE.remove(event.getServer());}
}
