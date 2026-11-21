package tc4revival.research;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import tc4revival.network.ResearchCompletionPacket;
import tc4revival.network.TCNetwork;

/** Owns live openings by player instance, so a respawn/login cannot inherit an earlier connection's book. */
public  class ResearchBookSessions {
    public static final Registry LIVE = new Registry((player, change) -> {
        for (var completion : change.completions()) TCNetwork.send(player, ResearchCompletionPacket.from(completion));
        // KnowledgeSync observes the committed revision and publishes the owning player's state at end tick.
    });
    private ResearchBookSessions() {}
    public static  class Registry {
        private final Map<ServerPlayer, ServerResearchBook> sessions = new IdentityHashMap<>();
        private final BiConsumer<ServerPlayer, ResearchBookActions.Change> completed;
        public Registry(BiConsumer<ServerPlayer, ResearchBookActions.Change> completed) { this.completed = completed; }
        public ServerResearchBook replace(ServerPlayer player, Supplier<ServerResearchBook> opening) {
            close(player);
            var book = opening.get();
            if (!book.ownedBy(player)) throw new IllegalArgumentException("Book opening belongs to another player");
            sessions.put(player, book);
            // Opening the old ordinary/cheat book synchronized knowledge, without research-completion popups.
            return book;
        }
        public ServerResearchBook.Outcome request(ServerPlayer player, UUID token, long sequence, String key) {
            return request(player,token,sequence,key,false);
        }
        public ServerResearchBook.Outcome request(ServerPlayer player, UUID token, long sequence, String key, boolean creative) {
            return request(player, token, sequence, key, creative, false);
        }
        public ServerResearchBook.Outcome request(ServerPlayer player, UUID token, long sequence, String key, boolean creative, boolean listen) {
            requireThread(player);
            var book = sessions.get(player);
            if (book == null) return new ServerResearchBook.Denied(ServerResearchBookSession.Rejection.CLOSED);
            var outcome = book.request(player, token, sequence, key, creative, listen);
            if (outcome instanceof ServerResearchBook.Purchase purchase)
                purchase.result().change().ifPresent(change -> completed.accept(player, change));
            if (!book.valid()) close(player);
            return outcome;
        }
        public void close(ServerPlayer player) {
            requireThread(player);
            var book = sessions.remove(player); if (book != null) {
                book.close();
                thaumcraft.api.research.ResearchBookLifecycle.observe(thaumcraft.api.research.ResearchBookLifecycle.Stage.CLOSED,player.getUUID(),book.token(),"server closed");
            }
        }
        public void close(ServerPlayer player, UUID token) {
            requireThread(player);
            var book = sessions.get(player);
            if (book != null && book.token().equals(token)) close(player);
        }
        public void sweep(net.minecraft.server.MinecraftServer server) {
            if (!server.isSameThread()) throw new IllegalStateException("Book registry must use the server thread");
            var expired=sessions.entrySet().stream().filter(entry->entry.getKey().getServer()==server&&!entry.getValue().valid()).map(Map.Entry::getKey).toList();
            expired.forEach(this::close);
        }
        public void clear(net.minecraft.server.MinecraftServer server) {
            if (!server.isSameThread()) throw new IllegalStateException("Book registry must use the server thread");
            var players = sessions.keySet().stream().filter(player -> player.getServer() == server).toList();
            players.forEach(this::close);
        }
        private static void requireThread(ServerPlayer player) {
            if (player.getServer() == null || !player.getServer().isSameThread()) throw new IllegalStateException("Book registry must use the server thread");
        }
    }
    public static void tick(TickEvent.ServerTickEvent event) { if (event.phase == TickEvent.Phase.END) LIVE.sweep(event.getServer()); }
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { if (event.getEntity() instanceof ServerPlayer player) LIVE.close(player); }
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) { if (event.getEntity() instanceof ServerPlayer player) LIVE.close(player); }
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { if (event.getEntity() instanceof ServerPlayer player) LIVE.close(player); }
    public static void death(LivingDeathEvent event) { if (event.getEntity() instanceof ServerPlayer player) LIVE.close(player); }
    public static void stopped(ServerStoppedEvent event) { LIVE.clear(event.getServer()); }
}
