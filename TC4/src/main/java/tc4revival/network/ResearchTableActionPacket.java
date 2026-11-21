package tc4revival.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import tc4revival.content.ResearchTableBlockEntity.Action;
import tc4revival.content.ResearchTableMenu;

/** Intent only: table, dimension, player, bonus availability and costs come from the active server menu. */
public record ResearchTableActionPacket(int containerId, Action action, int q, int r, String first, String second) {
    public ResearchTableActionPacket {
        if (containerId < 0 || containerId > 100 || action == null || first == null || second == null
                || q < -4 || q > 4 || r < -4 || r > 4 || Math.abs(q + r) > 4
                || !first.matches("[a-z0-9_:./-]{0,64}") || !second.matches("[a-z0-9_:./-]{0,64}"))
            throw new IllegalArgumentException("Invalid table intent");
        switch (action) {
            case PLACE -> { if (first.isEmpty() || !second.isEmpty()) throw new IllegalArgumentException("Invalid placement"); }
            case ERASE -> { if (!first.isEmpty() || !second.isEmpty()) throw new IllegalArgumentException("Invalid erase"); }
            case COMBINE -> { if (first.isEmpty() || second.isEmpty() || q != 0 || r != 0) throw new IllegalArgumentException("Invalid combination"); }
            case SYNTHESIZE -> { if (first.isEmpty() || !second.isEmpty() || q != 0 || r != 0) throw new IllegalArgumentException("Invalid synthesis"); }
            case DUPLICATE -> { if (!first.isEmpty() || !second.isEmpty() || q != 0 || r != 0) throw new IllegalArgumentException("Invalid duplicate"); }
        }
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId).writeEnum(action);
        buffer.writeByte(q).writeByte(r);
        buffer.writeUtf(first, 64).writeUtf(second, 64);
    }
    public static ResearchTableActionPacket decode(FriendlyByteBuf buffer) {
        var result = new ResearchTableActionPacket(buffer.readVarInt(), buffer.readEnum(Action.class),
                buffer.readByte(), buffer.readByte(), buffer.readUtf(64), buffer.readUtf(64));
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing table intent data");
        return result;
    }
    public void handle(ServerPlayer player) {
        if (player == null || !(player.containerMenu instanceof ResearchTableMenu menu)
                || menu.containerId != containerId || menu.table() == null || !menu.stillValid(player)) return;
        menu.table().act(player, action, q, r, first.isEmpty() ? null : first, second.isEmpty() ? null : second);
    }
}
