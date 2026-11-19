package tc4revival.client;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.TC4Revival;
import tc4revival.aspect.TCAspects;
import tc4revival.research.PlayerKnowledge;
import tc4revival.research.sync.KnowledgeTransfer;

/** Client-thread display cache only; it never awards knowledge or writes world storage. */
@Mod.EventBusSubscriber(modid = TC4Revival.MOD_ID, value = Dist.CLIENT)
public  class ClientKnowledge {
    private static Connection connection;
    private static KnowledgeTransfer.Receiver receiver;
    private static PlayerKnowledge knowledge;
    private static volatile java.util.Set<String> knownAspectTags=java.util.Set.of();
    private static volatile thaumcraft.api.aspects.AspectViews.Reader presentationReader=thaumcraft.api.aspects.AspectViews.Reader.empty();
    public static thaumcraft.api.aspects.AspectViews.Reader presentationReader(){return presentationReader;}
    public static Optional<PlayerKnowledge> displaySnapshot(){return snapshot().map(thaumcraft.api.aspects.AspectViews::knowledge);}
    private static long revision;
    public static long revision(){return revision;}
    private ClientKnowledge() { }
    public static Optional<PlayerKnowledge> snapshot() {
        requireThread();
        return Optional.ofNullable(knowledge).map(PlayerKnowledge::copy);
    }
    /** Immutable display-only names for recipe viewers that index tooltips on a worker thread. */
    public static java.util.Set<String> knownAspectTags(){return knownAspectTags;}
    @SubscribeEvent
    public static void login(ClientPlayerNetworkEvent.LoggingIn event) {
        requireThread();
        connection = event.getConnection();
        receiver = new KnowledgeTransfer.Receiver(event.getPlayer().getUUID());
        knowledge = null;presentationReader=thaumcraft.api.aspects.AspectViews.Reader.empty();
        knownAspectTags=java.util.Set.of();
    }
    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        requireThread();
        if (event.getConnection() == null || event.getConnection() == connection) clear();
    }
    public static void receive(KnowledgeTransfer.Frame frame, Connection source) {
        requireThread();
        var minecraft = Minecraft.getInstance();
        if (receiver == null || source != connection || minecraft.getConnection() == null
                || minecraft.getConnection().getConnection() != source || !source.isConnected()) return;
        try {
            if (receiver.accept(frame)) {
                knowledge = receiver.snapshot().orElseThrow().toKnowledge(TCAspects.catalog());
                revision++;presentationReader=thaumcraft.api.aspects.AspectViews.Reader.of(knowledge);
                knownAspectTags=knowledge.knownAspects().keySet().stream().map(tc4revival.aspect.Aspect::tag)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
            }
        } catch (RuntimeException invalid) {
            TC4Revival.LOGGER.error("Invalid Thaumcraft knowledge synchronization", invalid);
            clear();
            source.disconnect(Component.literal("Unable to synchronize Thaumcraft knowledge."));
        }
    }
    private static void clear() { connection = null; receiver = null; knowledge = null; presentationReader=thaumcraft.api.aspects.AspectViews.Reader.empty();knownAspectTags=java.util.Set.of();revision++; }
    private static void requireThread() {
        if (!Minecraft.getInstance().isSameThread()) throw new IllegalStateException("Client knowledge must be accessed on the client thread");
    }
}
