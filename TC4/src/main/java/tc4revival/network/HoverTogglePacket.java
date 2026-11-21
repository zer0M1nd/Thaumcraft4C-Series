package tc4revival.network;

import net.minecraft.network.FriendlyByteBuf;

/** No entity id or desired permissions supplied by the client. */
public record HoverTogglePacket() {
    public void encode(FriendlyByteBuf buffer) {}
    public static HoverTogglePacket decode(FriendlyByteBuf buffer) { return new HoverTogglePacket(); }
}
