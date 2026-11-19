package tc4revival.research;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import tc4revival.content.TCItems;
import tc4revival.network.LensPacket;
import tc4revival.network.TCNetwork;

/** Server publishes the lens display while held; clients cannot request arbitrary hidden world data. */
public  class ThaumometerLens {
    private static final Map<Connection, LensPacket> SENT = new IdentityHashMap<>();
    private ThaumometerLens() {}
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            var connection = player.connection.connection;
            boolean held = player.getMainHandItem().is(TCItems.THAUMOMETER.get())
                    || player.getOffhandItem().is(TCItems.THAUMOMETER.get());
            if (!held && !SENT.containsKey(connection)) continue;
            var selection = held ? ScanTargetSelector.select(player) : java.util.Optional.<ScanTargetSelector.Selection>empty();
            Component name = selection.map(ScanTargetSelector.Selection::name).orElse(Component.empty());
            List<LensPacket.Value> aspects = List.of();
            Component details = Component.empty();
            if (selection.isPresent()) {
                var target = selection.get().target();
                var knowledge = KnowledgeSavedData.get(player.getServer()).snapshot(player.getUUID());
                if (knowledge.isPresent() && target.identity().filter(id -> knowledge.get().scanned(target.kind(), id)).isPresent()) {
                    var node = selection.get().node();
                    var values = node.map(tc4revival.content.NodeJarData::aspects).orElseGet(target::aspects);
                    if (node.isPresent()) {
                        var value = node.get();
                        var description = Component.translatable("nodetype." + value.type() + ".name");
                        if (value.modifier()!=null) description.append(", ").append(Component.translatable("nodemod." + value.modifier() + ".name"));
                        details = description;
                    }
                    // The old scanner uses getAspectsSorted (tag order), not its amount sorter.
                    aspects = values.alphabetical().stream()
                            .map(aspect -> new LensPacket.Value(aspect.tag(), values.amount(aspect))).toList();
                    if(aspects.isEmpty())aspects=List.of(new LensPacket.Value(ScanIdentity.EMPTY_ICON,0));
                }
            }
            var packet = thaumcraft.api.research.ScannerViews.lens(new thaumcraft.api.research.ScannerViews.Lens(player,
                    selection.map(ScanTargetSelector.Selection::target).orElse(null),KnowledgeSavedData.get(player.getServer()).snapshot(player.getUUID()).orElse(null)),
                    new LensPacket(player.level().dimension().location(), name, aspects, details));
            if (!packet.equals(SENT.get(connection))) TCNetwork.send(player, packet);
            if (held) SENT.put(connection, packet); else SENT.remove(connection);
        }
    }
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) SENT.remove(player.connection.connection);
    }
    public static void stopped(ServerStoppedEvent event) { SENT.clear(); }
}
