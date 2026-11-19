package tc4revival.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Server-to-client only. The bounded descriptor contains no game actions or arbitrary resources. */
public record LegacyEffectPacket(ResourceLocation dimension,String style,Vec3 from,Vec3 to,int color,float scale,int life,int count,float spread,float gravity) {
    public void encode(FriendlyByteBuf b) {b.writeResourceLocation(dimension);b.writeUtf(style,32);b.writeDouble(from.x);b.writeDouble(from.y);b.writeDouble(from.z);b.writeDouble(to.x);b.writeDouble(to.y);b.writeDouble(to.z);b.writeInt(color);b.writeFloat(scale);b.writeVarInt(life);b.writeVarInt(count);b.writeFloat(spread);b.writeFloat(gravity);}
    public static LegacyEffectPacket decode(FriendlyByteBuf b) {return new LegacyEffectPacket(b.readResourceLocation(),b.readUtf(32),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readInt(),b.readFloat(),b.readVarInt(),b.readVarInt(),b.readFloat(),b.readFloat());}
    public boolean valid() {return finite(from)&&finite(to)&&Float.isFinite(scale)&&scale>0&&scale<=16&&life>0&&life<=200&&count>0&&count<=128&&Float.isFinite(spread)&&spread>=0&&spread<=16&&Float.isFinite(gravity)&&Math.abs(gravity)<=4;}
    private static boolean finite(Vec3 p) {return Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z);}
}
