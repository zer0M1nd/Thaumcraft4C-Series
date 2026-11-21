package tc4revival.research;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.junit.jupiter.api.Test;
import tc4revival.network.WarpGainPacket;
import static org.junit.jupiter.api.Assertions.*;

class BrainConsumptionTest {
    @Test void stickyBoundaryAndTemporaryAmountsUseTheOldDrawOrder() {
        for (float chance : new float[]{0, Math.nextDown(0.1f), 0.1f, Math.nextDown(1.0f)}) {
            for (int amount = 0; amount < 3; amount++) {
                int next = amount;
                var draws = new int[1];
                var random = new LegacyRandomSource(0) {
                    @Override public float nextFloat() { assertEquals(0, draws[0]++); return chance; }
                    @Override public int nextInt(int bound) { assertEquals(3, bound); assertEquals(1, draws[0]++); return next; }
                };
                var gain = BrainConsumption.roll(random);
                assertEquals(chance < 0.1f ? PlayerKnowledge.WarpKind.STICKY : PlayerKnowledge.WarpKind.TEMPORARY, gain.kind());
                assertEquals(chance < 0.1f ? 1 : amount + 1, gain.amount());
                assertEquals(chance < 0.1f ? 1 : 2, draws[0]);
            }
        }
    }
    @Test void consumptionAddsOnlyTheSelectedKindAndResetsCounterToTotal() {
        var catalog = new tc4revival.aspect.AspectCatalog(java.util.Arrays.stream(tc4revival.aspect.LegacyAspect.values())
                .map(tc4revival.aspect.LegacyAspect::create).toList());
        var knowledge = new PlayerKnowledge(catalog);
        knowledge.setWarp(PlayerKnowledge.WarpKind.PERMANENT, 5);
        knowledge.setWarpCounter(100);
        BrainConsumption.apply(knowledge, new BrainConsumption.Gain(PlayerKnowledge.WarpKind.STICKY, 1));
        BrainConsumption.apply(knowledge, new BrainConsumption.Gain(PlayerKnowledge.WarpKind.TEMPORARY, 3));
        assertEquals(5, knowledge.warp(PlayerKnowledge.WarpKind.PERMANENT));
        assertEquals(1, knowledge.warp(PlayerKnowledge.WarpKind.STICKY));
        assertEquals(3, knowledge.warp(PlayerKnowledge.WarpKind.TEMPORARY));
        assertEquals(9, knowledge.warpCounter());
    }
    @Test void gainPacketsRoundTripAndRejectMalformedOrNonPositiveChanges() {
        for (var kind : PlayerKnowledge.WarpKind.values()) {
            var packet = new WarpGainPacket(kind, 3);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                packet.encode(buffer); int length = buffer.writerIndex();
                assertEquals(packet, WarpGainPacket.decode(buffer));
                buffer.readerIndex(0); buffer.writeByte(0);
                assertThrows(IllegalArgumentException.class, () -> WarpGainPacket.decode(buffer));
                buffer.readerIndex(0); buffer.writerIndex(length - 1);
                assertThrows(IndexOutOfBoundsException.class, () -> WarpGainPacket.decode(buffer));
                buffer.clear(); buffer.writeVarInt(99).writeVarInt(1);
                assertThrows(RuntimeException.class, () -> WarpGainPacket.decode(buffer));
            } finally { buffer.release(); }
            assertThrows(IllegalArgumentException.class, () -> new WarpGainPacket(kind, 0));
            var removal = new WarpGainPacket(kind, -1);
            var removalBuffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                removal.encode(removalBuffer);
                assertEquals(removal, WarpGainPacket.decode(removalBuffer));
            } finally { removalBuffer.release(); }
        }
    }
}
