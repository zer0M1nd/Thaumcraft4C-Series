package tc4revival.content;

import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

/** Only trunks that have actually observed their owner join the following list. */
public  class TravelingTrunkFollowers {
    private static final Map<String,List<WeakReference<TravelingTrunkEntity>>> LINKS=new HashMap<>();
    public static void track(TravelingTrunkEntity trunk) {
        var list=LINKS.computeIfAbsent(trunk.ownerName(),ignored->new ArrayList<>());
        list.removeIf(reference->reference.get()==null || reference.get().isRemoved());
        if (list.stream().noneMatch(reference->reference.get()==trunk)) list.add(new WeakReference<>(trunk));
    }
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) follow(player);
    }
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) follow(player);
    }
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) follow(player);
    }
    // Legacy EntityJoinWorld handled login and respawn as well as dimension transfers.
    // Modern player lifecycle events run after the destination has registered its player.
    private static void follow(ServerPlayer player) {
        var list=LINKS.get(player.getGameProfile().getName()); if (list==null) return;
        for (var reference:List.copyOf(list)) {
            var trunk=reference.get();
            if (trunk==null || trunk.isRemoved() || trunk.isOnPortalCooldown() || trunk.getServer()!=player.getServer()) continue;
            trunk.setPortalCooldown(); trunk.changeDimension(player.serverLevel());
        }
        list.removeIf(reference->reference.get()==null || reference.get().isRemoved());
    }
    public static void stopped(ServerStoppedEvent event) { LINKS.clear(); }
    private TravelingTrunkFollowers() {}
}
