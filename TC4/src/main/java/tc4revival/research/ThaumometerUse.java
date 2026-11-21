package tc4revival.research;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import tc4revival.content.TCItems;
import tc4revival.network.ScanFeedbackPacket;
import tc4revival.network.TCNetwork;
import tc4revival.network.ScanEffectPacket;

/** One non-transferable use per actual player object; release, death and changing hands revoke it. */
public  class ThaumometerUse {
    private static  class Use {
        final InteractionHand hand;
        final ItemStack stack;
        final ServerScanSession session;
        long lastVisualTick = Long.MIN_VALUE;
        Use(InteractionHand hand, ItemStack stack, ServerScanSession session) { this.hand = hand; this.stack = stack; this.session = session; }
    }
    private static final Map<ServerPlayer, Use> USES = new IdentityHashMap<>();
    private ThaumometerUse() {}
    public static void begin(ServerPlayer player, InteractionHand hand) {
        cancel(player);
        var stack = player.getItemInHand(hand);
        if (!stack.is(TCItems.THAUMOMETER.get()) || !player.isUsingItem() || player.getUsedItemHand() != hand) return;
        var session = new ServerScanSession(player);
        session.begin();
        var use = new Use(hand, stack, session);
        USES.put(player, use);
        emit(player, use, visibleRunes(player), false);
    }
    public static void tick(ServerPlayer player, ItemStack stack, int remaining) {
        var use = USES.get(player);
        if (use == null) return;
        if (!matches(player, use) || stack != use.stack) { cancel(player); return; }
        var visible = visibleRunes(player);
        var update = use.session.tick();
        emit(player, use, visible, remaining % 2 == 0
                && (update.state() == ScanProgress.State.TRACKING || update.state() == ScanProgress.State.COMPLETED));
        update.result().ifPresent(result -> TCNetwork.send(player, ScanFeedbackPacket.from(result)));
        update.clue().ifPresent(key -> TCNetwork.send(player, new tc4revival.network.CluePacket(key)));
        if (update.state() == ScanProgress.State.COMPLETED) {
            USES.remove(player);
            player.stopUsingItem();
        }
    }
    public static void cancel(ServerPlayer player) {
        var use = USES.remove(player);
        if (use != null) use.session.cancel();
    }
    private static boolean matches(ServerPlayer player, Use use) {
        return player.isAlive() && !player.isRemoved() && !player.isSpectator() && player.isUsingItem()
                && player.getUsedItemHand() == use.hand && player.getItemInHand(use.hand) == use.stack
                && player.getUseItem() == use.stack && use.stack.is(TCItems.THAUMOMETER.get());
    }
    private static java.util.Optional<ScanTargetSelector.Selection> visibleRunes(ServerPlayer player) {
        var knowledge = KnowledgeSavedData.get(player.getServer()).snapshot(player.getUUID());
        return knowledge.isEmpty() ? java.util.Optional.empty() : ScanTargetSelector.select(player)
                .filter(selection -> selection.target().canTrack(knowledge.get(), PlayerKnowledge.ScanMode.THAUMOMETER, ResearchCatalogs.rules()));
    }
    private static void emit(ServerPlayer player, Use use, java.util.Optional<ScanTargetSelector.Selection> selection, boolean sound) {
        long tick = player.level().getGameTime();
        if (use.lastVisualTick == tick) return;
        use.lastVisualTick = tick;
        selection.ifPresent(value -> TCNetwork.send(player, new ScanEffectPacket(player.level().dimension().location(),
                value.runeOrigin(), value.runeLifetime(), sound)));
    }
    public static void endTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var iterator = USES.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!matches(entry.getKey(), entry.getValue())) { entry.getValue().session.cancel(); iterator.remove(); }
        }
    }
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }
    public static void stopped(ServerStoppedEvent event) { USES.clear(); }
}
