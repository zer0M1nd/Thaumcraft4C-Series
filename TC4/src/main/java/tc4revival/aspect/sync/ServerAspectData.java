package tc4revival.aspect.sync;

import com.google.gson.JsonElement;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import tc4revival.network.*;

@Mod.EventBusSubscriber(modid="thaumcraft")
public class ServerAspectData {
    private static final Map<String,Map<String,String>> directories=new HashMap<>();
    private static AspectDataSet snapshot;private static int revision;
    private static int encodedRevision=-1,encodedResourceCount;
    private static List<byte[]> encodedInputs=List.of();
    public static synchronized void capture(String directory,Map<ResourceLocation,JsonElement> files){
        var copy=new TreeMap<String,String>();files.forEach((id,json)->copy.put(directory+"/"+id,json.toString()));
        directories.put(directory,Map.copyOf(copy));snapshot=null;revision++;
    }
    public static synchronized AspectDataSet snapshot(){
        if(snapshot==null){
            if(!directories.keySet().containsAll(AspectDataSet.DIRECTORIES))throw new IllegalStateException("Aspect resources not ready");
            var all=new TreeMap<String,String>();directories.values().forEach(all::putAll);snapshot=new AspectDataSet(all);
        }
        return snapshot;
    }
    @SubscribeEvent public static void sync(OnDatapackSyncEvent event){
        if(event.getPlayer()!=null)manifest(event.getPlayer());else event.getPlayerList().getPlayers().forEach(ServerAspectData::manifest);
    }
    private static synchronized void manifest(ServerPlayer player){
        if(encodedRevision!=revision){var data=inputs();encodedInputs=AspectDataTransfer.encode(data);encodedResourceCount=data.size()-1;encodedRevision=revision;}
        tc4revival.TC4Revival.LOGGER.info("Aspect inputs sent to {}: {} custom resources and authoritative inference inputs, {} compressed bytes; no generated tables",
                player.getGameProfile().getName(),encodedResourceCount,encodedInputs.stream().mapToInt(a->a.length).sum());
        for(int i=0;i<encodedInputs.size();i++)TCNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new AspectDataFrame(revision,true,i,encodedInputs.size(),encodedInputs.get(i)));
    }
    public static Map<String,String> inputs(){
        var delta=AspectInputDelta.between(BundledAspectData.all(),snapshot().files());
        delta.put(AspectInputDelta.SETTINGS,tc4revival.aspect.inference.AdvancedAspectInference.inputs().encode().toString());
        return delta;
    }
    /** Kept for API callers: one ordinary reload supplies vanilla recipes/tags in their normal order. */
    public static void refresh(net.minecraft.server.MinecraftServer server){
        server.reloadResources(server.getPackRepository().getSelectedIds());
    }
    /** Clients never request generated tables or bundled resource corrections. */
    public static void reply(ServerPlayer player,AspectDataRequest request) {}
    @SubscribeEvent public static synchronized void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){directories.clear();snapshot=null;revision++;encodedRevision=-1;encodedInputs=List.of();}
}
