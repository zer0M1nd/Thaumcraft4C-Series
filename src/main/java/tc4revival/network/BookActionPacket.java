package tc4revival.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import tc4revival.research.ResearchBookSessions;

/** Intent only; costs, route, player knowledge and book ownership never come from the wire. */
public record BookActionPacket(UUID token, long sequence, Action action, String key) {
    public enum Action { REQUEST, CLOSE, CREATIVE_COMPLETE, LISTEN }
    public BookActionPacket {
        if (token == null || sequence < 0 || action == null || key == null
                || key.contains("..") || !key.matches("[A-Za-z0-9_:./-]{0,128}") || (action == Action.CLOSE) != key.isEmpty())
            throw new IllegalArgumentException("Invalid book intent");
    }
    public void encode(FriendlyByteBuf buffer) { buffer.writeUUID(token).writeVarLong(sequence).writeEnum(action).writeUtf(key, 128); }
    public static BookActionPacket decode(FriendlyByteBuf buffer) {
        var result = new BookActionPacket(buffer.readUUID(), buffer.readVarLong(), buffer.readEnum(Action.class), buffer.readUtf(128));
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing book intent data");
        return result;
    }
    public BookResultPacket dispatch(ServerPlayer player, ResearchBookSessions.Registry registry) {
        if (action == Action.CLOSE) {
            registry.close(player, token);
            return new BookResultPacket(token, sequence, BookResultPacket.Status.CLOSED, "");
        }
        return BookResultPacket.from(token, sequence, registry.request(player, token, sequence, key, action == Action.CREATIVE_COMPLETE, action == Action.LISTEN));
    }
    public void handle(ServerPlayer player) {
        if (player != null) TCNetwork.send(player, dispatch(player, ResearchBookSessions.LIVE));
    }
}
