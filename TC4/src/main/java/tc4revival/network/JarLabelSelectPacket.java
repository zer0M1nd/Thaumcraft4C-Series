package tc4revival.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

public record JarLabelSelectPacket(InteractionHand hand,int slot,String aspect,boolean all) {
    public void encode(FriendlyByteBuf buf) { buf.writeEnum(hand);buf.writeVarInt(slot);buf.writeUtf(aspect,64);buf.writeBoolean(all); }
    public static JarLabelSelectPacket decode(FriendlyByteBuf buf) { return new JarLabelSelectPacket(buf.readEnum(InteractionHand.class),buf.readVarInt(),buf.readUtf(64),buf.readBoolean()); }
    public void handle(ServerPlayer player) { if(player!=null)tc4revival.content.JarLabelItem.mark(player,hand,slot,aspect,all); }
}
