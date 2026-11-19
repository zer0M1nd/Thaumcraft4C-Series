package tc4revival.research;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import tc4revival.network.KnowledgePacket;
import tc4revival.network.TCNetwork;
import tc4revival.research.sync.KnowledgePublisher;

/** Server-thread lifecycle; sends only the owning player's knowledge, at most once per end tick. */
public  class KnowledgeSync {
    private static final Map<Connection, Peer> PEERS = new IdentityHashMap<>();
    private static  class Peer {
        final KnowledgePublisher publisher = new KnowledgePublisher();
        long sourceRevision = -1;
    }
    private KnowledgeSync() { }
    /** Item-driven screens must observe committed knowledge before their following open packet. */
    public static void flushBeforeScreen(ServerPlayer player) {
        if (player.getServer() == null || !player.getServer().isSameThread())
            throw new IllegalStateException("Knowledge screen synchronization requires the server thread");
        sync(player, false);
    }

    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        KnowledgeSavedData.login(event);
        if (event.getEntity() instanceof ServerPlayer player) sync(player, true);
    }
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player, true);
    }
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player, true);
    }
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PEERS.remove(player.connection.connection);
    }
    public static void stopped(ServerStoppedEvent event) { PEERS.clear(); }
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (var player : event.getServer().getPlayerList().getPlayers()) sync(player, false);
    }
    private static void sync(ServerPlayer player, boolean reset) {
        var data = KnowledgeSavedData.get(player.getServer());
        var peer = PEERS.computeIfAbsent(player.connection.connection, ignored -> new Peer());
        long revision = data.revision(player.getUUID());
        if (!reset && peer.sourceRevision == revision) return;
        var state = data.snapshot(player.getUUID());
        // Unsupported world storage is deliberately not replaced by fabricated client knowledge.
        if (state.isEmpty()) return;
        peer.publisher.publish(player.getUUID(), state.get(), reset, frame -> TCNetwork.send(player, new KnowledgePacket(frame)));
        peer.sourceRevision = revision;
    }
}
