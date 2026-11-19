package tc4revival.network;

public record RunicChargePacket(int entityId,int charge,int maximum) {
    public void encode(net.minecraft.network.FriendlyByteBuf buffer) { buffer.writeVarInt(entityId); buffer.writeVarInt(charge); buffer.writeVarInt(maximum); }
    public static RunicChargePacket decode(net.minecraft.network.FriendlyByteBuf buffer) { return new RunicChargePacket(buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt()); }
}
