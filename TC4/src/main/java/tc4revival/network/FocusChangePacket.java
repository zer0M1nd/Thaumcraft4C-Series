package tc4revival.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/** The sender supplies only a desired sorting key; all stacks and ownership are resolved server-side. */
public record FocusChangePacket(InteractionHand hand, String key) {
    public FocusChangePacket { if (key.length() > 128) throw new IllegalArgumentException("Focus key too long"); }
    public static void encode(FocusChangePacket value, FriendlyByteBuf buffer) { buffer.writeEnum(value.hand); buffer.writeUtf(value.key, 128); }
    public static FocusChangePacket decode(FriendlyByteBuf buffer) { return new FocusChangePacket(buffer.readEnum(InteractionHand.class), buffer.readUtf(128)); }
    public void handle(ServerPlayer sender) { tc4revival.content.FocusSelection.change(sender, hand, key); }
}
