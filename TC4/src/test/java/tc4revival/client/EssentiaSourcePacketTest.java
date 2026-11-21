package tc4revival.client;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import tc4revival.network.EssentiaSourcePacket;
import static org.junit.jupiter.api.Assertions.*;

class EssentiaSourcePacketTest {
    @Test void sourceOffsetsRoundTripWithBothSignsAndDimension() {
        var target=new BlockPos(-800,90,700);
        var packet=new EssentiaSourcePacket(ResourceLocation.parse("minecraft:the_nether"),target,target.offset(-127,12,127),0x80a0ff);
        var buffer=new FriendlyByteBuf(Unpooled.buffer());
        try { packet.encode(buffer); assertEquals(packet,EssentiaSourcePacket.decode(buffer)); assertFalse(buffer.isReadable()); }
        finally { buffer.release(); }
    }
    @Test void distantSourcesCannotWrapTheirByteOffsets() {
        assertThrows(IllegalArgumentException.class,()->new EssentiaSourcePacket(ResourceLocation.parse("minecraft:overworld"),BlockPos.ZERO,new BlockPos(128,0,0),0));
    }
}
