package tc4revival.research;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import tc4revival.content.ResearchTableBlockEntity.Action;
import tc4revival.network.ResearchTableActionPacket;
import static org.junit.jupiter.api.Assertions.*;

class ResearchTablePacketTest {
    @Test void validIntentsRoundTripAndTrailingOrTruncatedBytesFail() {
        for (var packet : java.util.List.of(new ResearchTableActionPacket(100, Action.PLACE, -4, 4, "aer", ""),
                new ResearchTableActionPacket(1, Action.ERASE, 0, -4, "", ""),
                new ResearchTableActionPacket(2, Action.COMBINE, 0, 0, "aer", "ignis"),
                new ResearchTableActionPacket(3, Action.DUPLICATE, 0, 0, "", ""))) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                packet.encode(buffer); int length = buffer.writerIndex();
                assertEquals(packet, ResearchTableActionPacket.decode(buffer));
                buffer.readerIndex(0); buffer.writeByte(1);
                assertThrows(IllegalArgumentException.class, () -> ResearchTableActionPacket.decode(buffer));
                buffer.readerIndex(0); buffer.writerIndex(length - 1);
                assertThrows(IndexOutOfBoundsException.class, () -> ResearchTableActionPacket.decode(buffer));
            } finally { buffer.release(); }
        }
    }
    @Test void contradictoryIntentsAndUnboundedCoordinatesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableActionPacket(101, Action.PLACE, 0, 0, "aer", ""));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableActionPacket(1, Action.PLACE, 4, 4, "aer", ""));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableActionPacket(1, Action.PLACE, 0, 0, "", ""));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableActionPacket(1, Action.ERASE, 0, 0, "aer", ""));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableActionPacket(1, Action.COMBINE, 0, 0, "aer", ""));
        assertThrows(IllegalArgumentException.class, () -> new ResearchTableActionPacket(1, Action.DUPLICATE, 0, 0, "aer", ""));
    }
}
