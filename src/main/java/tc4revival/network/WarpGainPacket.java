package tc4revival.network;

import net.minecraft.network.FriendlyByteBuf;
import tc4revival.research.PlayerKnowledge.WarpKind;

/** Transient legacy warp change notification; authoritative totals arrive through knowledge synchronization. */
public record WarpGainPacket(WarpKind kind, int amount) {
    public WarpGainPacket {
        java.util.Objects.requireNonNull(kind);
        if (amount == 0) throw new IllegalArgumentException("Warp change must be nonzero");
    }
    public void encode(FriendlyByteBuf buffer) { buffer.writeEnum(kind).writeVarInt(amount); }
    public static WarpGainPacket decode(FriendlyByteBuf buffer) {
        var result = new WarpGainPacket(buffer.readEnum(WarpKind.class), buffer.readVarInt());
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing warp gain data");
        return result;
    }
}
