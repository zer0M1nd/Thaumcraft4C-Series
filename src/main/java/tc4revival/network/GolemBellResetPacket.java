package tc4revival.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import tc4revival.content.GolemBellItem;

/** Resolve the held bell and selected entity in the sender's world, never a supplied player id. */
public record GolemBellResetPacket(InteractionHand hand) {
    public static void encode(GolemBellResetPacket packet,FriendlyByteBuf buffer) { buffer.writeEnum(packet.hand); }
    public static GolemBellResetPacket decode(FriendlyByteBuf buffer) { return new GolemBellResetPacket(buffer.readEnum(InteractionHand.class)); }
    public void handle(ServerPlayer player) {
        if (player!=null && player.getItemInHand(hand).getItem() instanceof GolemBellItem) GolemBellItem.resetMarkers(player.getItemInHand(hand),player);
    }
}
