package tc4revival.client;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.TC4Revival;
import tc4revival.network.*;

/** Connection-bound book protocol state. The book screen consumes results; this class never grants knowledge. */
@Mod.EventBusSubscriber(modid = TC4Revival.MOD_ID, value = Dist.CLIENT)
public  class ClientBookSession {
    private static Connection connection;
    private static final tc4revival.research.BookClientState state = new tc4revival.research.BookClientState();
    private ClientBookSession() {}
    public static void open(BookOpenPacket packet, Connection source) {
        var lifecycle=thaumcraft.api.research.ResearchBookLifecycle.Stage.PACKET_RECEIVED;
        thaumcraft.api.research.ResearchBookLifecycle.observe(lifecycle,null,packet.token(),"received");
        if (!current(source)) {
            thaumcraft.api.research.ResearchBookLifecycle.observe(thaumcraft.api.research.ResearchBookLifecycle.Stage.REJECTED,null,packet.token(),"stale connection");return;
        }
        try{openAccepted(packet,source);}catch(RuntimeException failure){
            thaumcraft.api.research.ResearchBookLifecycle.observe(thaumcraft.api.research.ResearchBookLifecycle.Stage.FAILED,null,packet.token(),failure.toString());throw failure;
        }
        var screen=Minecraft.getInstance().screen;
        thaumcraft.api.research.ResearchBookLifecycle.observe(thaumcraft.api.research.ResearchBookLifecycle.Stage.SCREEN_CREATED,null,packet.token(),"active="+active()+", screen="+(screen==null?"null":screen.getClass().getName()));
    }
    private static void openAccepted(BookOpenPacket packet,Connection source){
        connection = source; state.open(packet.token());
        var navigation = new tc4revival.research.ResearchBookNavigation(
                new tc4revival.research.ResearchBrowser(tc4revival.research.ResearchCatalogs.bundled(),
                        tc4revival.research.ResearchCatalogs::enabled,
                        () -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.getAbilities().instabuild), tc4revival.research.ResearchPages.bundled());
        // Restore pan in the actual viewport. Applying the normal-to-maximum resize
        // afterwards would shift the saved position again on every book opening.
        var window=Minecraft.getInstance().getWindow();
        var layout=tc4revival.research.ResearchBrowserLayout.of(window.getGuiScaledWidth(),window.getGuiScaledHeight(),
                tc4revival.config.TCConfig.CLIENT.maximumResearchBrowser.get());
        navigation.viewport(layout.viewportWidth(),layout.viewportHeight());
        ClientKnowledge.displaySnapshot().ifPresent(knowledge->{var discovered=new java.util.LinkedHashMap<String,Integer>();knowledge.knownAspects().forEach((a,n)->discovered.put(a.tag(),n));navigation.refresh(knowledge,discovered,tc4revival.config.TCConfig.SERVER.researchDifficulty.get());});
        try {locations().load(locationKey()).ifPresent(navigation::restoreLocation);}catch(java.io.IOException error){org.slf4j.LoggerFactory.getLogger(ClientBookSession.class).warn("Cannot load book location",error);}
        Minecraft.getInstance().setScreen(new ResearchBrowserScreen(packet.token(), navigation,
                (tree, nav) -> new ResearchReaderScreen(tree, nav, (reader, graphics, page, side, x, y, mx, my) -> {
                    if (!ResearchCraftingArtwork.draw(reader, graphics, page, side, x, y, mx, my)
                            && page.type() != tc4revival.research.ResearchPage.Type.IMAGE)
                        throw new IllegalArgumentException("Unsupported book page " + page.type());
                    // The legacy reader has no IMAGE dispatch; inline illustrations are drawn with text.
                })));
        if(!packet.research().isEmpty() && Minecraft.getInstance().screen instanceof ResearchBrowserScreen tree)tree.locateRecipe(packet.research());
        var player = Minecraft.getInstance().player;
        if (player != null) player.playSound(tc4revival.content.TCSounds.PAGE.get(), 1f, 1f);
    }
    public static boolean request(String key) {
        return token().map(owner -> request(owner, key)).orElse(false);
    }
    private static tc4revival.research.BookLocationStore locations(){return new tc4revival.research.BookLocationStore(Minecraft.getInstance().gameDirectory.toPath().resolve("config/thaumcraft/book-locations.json"));}
    private static String locationKey(){
        var mc=Minecraft.getInstance();var local=mc.getSingleplayerServer();var remote=mc.getCurrentServer();
        String world=local!=null?local.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize().toString():remote!=null?remote.ip:"local-network";
        return world+"#"+mc.player.getUUID();
    }
    public static void remember(tc4revival.research.ResearchBookNavigation navigation){
        if(Minecraft.getInstance().player==null)return;
        try{locations().save(locationKey(),navigation.location());}catch(java.io.IOException error){org.slf4j.LoggerFactory.getLogger(ClientBookSession.class).warn("Cannot save book location",error);}
    }
    public static boolean request(UUID owner, String key) {
        if (!current(connection)) return false;
        var packet = state.request(owner, key);
        packet.ifPresent(TCNetwork.CHANNEL::sendToServer); return packet.isPresent();
    }
    public static boolean listen(UUID owner, String key) {
        if (!current(connection)) return false;
        var packet = state.request(owner, key, BookActionPacket.Action.LISTEN);
        packet.ifPresent(TCNetwork.CHANNEL::sendToServer); return packet.isPresent();
    }
    public static boolean creative(UUID owner,String key) {
        if(!current(connection) || !Minecraft.getInstance().player.getAbilities().instabuild)return false;
        var packet=state.request(owner,key,BookActionPacket.Action.CREATIVE_COMPLETE);
        packet.ifPresent(TCNetwork.CHANNEL::sendToServer);return packet.isPresent();
    }
    public static void receive(BookResultPacket packet, Connection source) {
        if (current(source) && source == connection) state.receive(packet);
    }
    public static Optional<BookResultPacket> takeResult(UUID owner) { return state.takeResult(owner); }
    public static Optional<UUID> token() { return current(connection) ? state.token() : Optional.empty(); }
    public static boolean active() { return token().isPresent(); }
    public static boolean active(UUID owner) { return current(connection) && state.active(owner); }
    public static boolean waiting() { return state.waiting(); }
    public static void close() {
        token().ifPresent(ClientBookSession::close);
        clear();
    }
    public static void close(UUID owner) {
        if (current(connection)) state.close(owner).ifPresent(TCNetwork.CHANNEL::sendToServer);
        thaumcraft.api.research.ResearchBookLifecycle.observe(thaumcraft.api.research.ResearchBookLifecycle.Stage.CLOSED,null,owner,"client closed");
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    private static void clear() { connection = null; state.clear(); }
    private static boolean current(Connection source) {
        var minecraft = Minecraft.getInstance();
        return source != null && source.isConnected() && minecraft.player != null && minecraft.getConnection() != null
                && minecraft.getConnection().getConnection() == source;
    }
}
