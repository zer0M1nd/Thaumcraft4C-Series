package tc4revival.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.PacketDistributor;

/** Ephemeral visual hint, emitted only after a real remote-container drain. */
public record EssentiaSourcePacket(ResourceLocation dimension,BlockPos consumer,BlockPos source,int color) {
    public EssentiaSourcePacket {
        consumer=consumer.immutable(); source=source.immutable();
        if (Math.abs((long)consumer.getX()-source.getX())>127 || Math.abs((long)consumer.getY()-source.getY())>127
                || Math.abs((long)consumer.getZ()-source.getZ())>127) throw new IllegalArgumentException("Essentia source offset outside packet range");
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(dimension); buffer.writeBlockPos(consumer); buffer.writeInt(color);
        buffer.writeByte(consumer.getX()-source.getX()); buffer.writeByte(consumer.getY()-source.getY()); buffer.writeByte(consumer.getZ()-source.getZ());
    }
    public static EssentiaSourcePacket decode(FriendlyByteBuf buffer) {
        var dimension=buffer.readResourceLocation(); var consumer=buffer.readBlockPos(); int color=buffer.readInt();
        return new EssentiaSourcePacket(dimension,consumer,consumer.offset(-buffer.readByte(),-buffer.readByte(),-buffer.readByte()),color);
    }
    public static void send(ServerLevel level,BlockPos consumer,BlockPos source,int color) {
        var packet=new EssentiaSourcePacket(level.dimension().location(),consumer,source,color);
        TCNetwork.CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(consumer.getX(),consumer.getY(),consumer.getZ(),32,level.dimension())),packet);
    }
}
