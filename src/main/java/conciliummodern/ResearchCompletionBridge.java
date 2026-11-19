package conciliummodern;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import tc4revival.research.*;

/** Observes public committed knowledge revisions without mutating research internals. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID)
public final class ResearchCompletionBridge {
    private record Seen(long revision,Set<String> research){}
    private static final Map<ServerPlayer,Seen> SEEN=new WeakHashMap<>();
    private static Seen snapshot(ServerPlayer player){var data=KnowledgeSavedData.get(player.server);return new Seen(data.revision(player.getUUID()),data.snapshot(player.getUUID()).map(k->Set.copyOf(k.researchMarkers())).orElseGet(Set::of));}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer player)SEEN.put(player,snapshot(player));}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer player)SEEN.remove(player);}
    @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent event){SEEN.clear();}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player))return;
        var data=KnowledgeSavedData.get(player.server);var previous=SEEN.get(player);
        if(previous!=null&&previous.revision==data.revision(player.getUUID()))return;
        var current=snapshot(player);SEEN.put(player,current);if(previous==null)return;
        current.research.stream().filter(key->!previous.research.contains(key)&&ResearchCatalogs.catalog().find(key).isPresent()).sorted()
                .forEach(key->com.ilya3point999k.thaumicconcilium.api.event.ThaumcraftResearchCompletedEvent.fireResearchCompletedEvent(player,key));
    }
    private ResearchCompletionBridge(){}
}
