package tc4revival.research;

import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import tc4revival.network.*;
import static org.junit.jupiter.api.Assertions.*;

class BookPacketTest {
    @Test void requestsRoundTripAndRejectExtraOrTruncatedData() {
        for (var packet : java.util.List.of(new BookActionPacket(UUID.randomUUID(), 0, BookActionPacket.Action.REQUEST, "HUNGRYCHEST"),
                new BookActionPacket(UUID.randomUUID(), Long.MAX_VALUE, BookActionPacket.Action.CLOSE, ""))) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                packet.encode(buffer); int length = buffer.writerIndex(); assertEquals(packet, BookActionPacket.decode(buffer));
                buffer.readerIndex(0); buffer.writeByte(1); assertThrows(IllegalArgumentException.class, () -> BookActionPacket.decode(buffer));
                buffer.readerIndex(0); buffer.writerIndex(length - 1); assertThrows(IndexOutOfBoundsException.class, () -> BookActionPacket.decode(buffer));
            } finally { buffer.release(); }
        }
    }
    @Test void ambiguousAndUnboundedIntentsAreRejected() {
        var token = UUID.randomUUID();
        for (String key : java.util.List.of("", "@THAUMIUM", "x".repeat(129), "a b", "../x/"))
            assertThrows(IllegalArgumentException.class, () -> new BookActionPacket(token, 0, BookActionPacket.Action.REQUEST, key));
        assertThrows(IllegalArgumentException.class, () -> new BookActionPacket(token, -1, BookActionPacket.Action.REQUEST, "THAUMIUM"));
        assertThrows(IllegalArgumentException.class, () -> new BookActionPacket(token, 0, BookActionPacket.Action.CLOSE, "THAUMIUM"));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeUUID(token).writeVarLong(0).writeVarInt(999).writeUtf("THAUMIUM");
            assertThrows(RuntimeException.class, () -> BookActionPacket.decode(buffer));
        } finally { buffer.release(); }
    }
    @Test void resultCodesAndOpeningTokenRoundTripWithStrictBoundaries() {
        for (var status : BookResultPacket.Status.values()) {
            var packet = new BookResultPacket(UUID.randomUUID(), 42, status, "POINTS");
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                packet.encode(buffer); assertEquals(packet, BookResultPacket.decode(buffer));
                buffer.readerIndex(0); buffer.writeByte(0); assertThrows(IllegalArgumentException.class, () -> BookResultPacket.decode(buffer));
            } finally { buffer.release(); }
        }
        var opening = new BookOpenPacket(UUID.randomUUID()); var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            opening.encode(buffer); assertEquals(opening, BookOpenPacket.decode(buffer));
            buffer.readerIndex(0); buffer.writeByte(0); assertThrows(IllegalArgumentException.class, () -> BookOpenPacket.decode(buffer));
        } finally { buffer.release(); }
    }
    @Test void authorizationAndGameplayRejectionsRemainDistinct() {
        var token = UUID.randomUUID();
        assertEquals(BookResultPacket.Status.AUTH_REJECTED, BookResultPacket.from(token, 0,
                new ServerResearchBook.Denied(ServerResearchBookSession.Rejection.TOKEN)).status());
        var result = new ResearchBookActions.Result(java.util.Optional.empty(), java.util.Optional.of(ResearchBookActions.Rejection.POINTS));
        var packet = BookResultPacket.from(token, 0, new ServerResearchBook.Purchase(result));
        assertEquals(BookResultPacket.Status.PURCHASE_REJECTED, packet.status()); assertEquals("POINTS", packet.reason());
    }
}
