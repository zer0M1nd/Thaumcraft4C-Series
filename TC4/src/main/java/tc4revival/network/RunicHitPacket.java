package tc4revival.network;

public record RunicHitPacket(int source,int target) {
    public void encode(net.minecraft.network.FriendlyByteBuf buffer) { buffer.writeVarInt(source); buffer.writeVarInt(target); }
    public static RunicHitPacket decode(net.minecraft.network.FriendlyByteBuf buffer) { return new RunicHitPacket(buffer.readVarInt(),buffer.readVarInt()); }
}
