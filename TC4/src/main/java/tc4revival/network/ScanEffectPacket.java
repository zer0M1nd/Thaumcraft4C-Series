package tc4revival.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** One server-observed cosmetic sample, delivered only to the scanning player. */
public record ScanEffectPacket(ResourceLocation dimension, Vec3 origin, int duration, boolean tickSound) {
    public ScanEffectPacket {
        java.util.Objects.requireNonNull(dimension);
        java.util.Objects.requireNonNull(origin);
        if (!Double.isFinite(origin.x) || !Double.isFinite(origin.y) || !Double.isFinite(origin.z)
                || duration < 0 || duration > Integer.MAX_VALUE / 3) throw new IllegalArgumentException("Invalid rune sample");
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(dimension);
        buffer.writeDouble(origin.x).writeDouble(origin.y).writeDouble(origin.z);
        buffer.writeVarInt(duration).writeBoolean(tickSound);
    }
    public static ScanEffectPacket decode(FriendlyByteBuf buffer) {
        var packet = new ScanEffectPacket(buffer.readResourceLocation(), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readVarInt(), buffer.readBoolean());
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing rune sample data");
        return packet;
    }
}
