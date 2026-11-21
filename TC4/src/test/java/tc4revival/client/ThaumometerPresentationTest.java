package tc4revival.client;

import io.netty.buffer.Unpooled;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import tc4revival.network.LensPacket;
import tc4revival.network.ScanFeedbackPacket;
import tc4revival.research.ScanRules;
import static org.junit.jupiter.api.Assertions.*;

class ThaumometerPresentationTest {
    @Test void runeFadeAndDiscreteGravityPreserveTheOldThresholdDiscontinuity() {
        var motion = new RuneMotion(15, 200);
        assertEquals(0, motion.alpha());
        for (int i = 0; i < 10; i++) motion.tick();
        assertEquals(1, motion.alpha(), 0.00001);
        assertEquals(200 - 0.04 * (double) 0.03f * 55, motion.y(1), 0.00000001);
        motion.tick();
        assertEquals(35f / 45, motion.alpha(), 0.00001, "The old transition from fade-in to fade-out has a visible step");
        assertEquals(200 - 0.04 * (double) 0.03f * (55 + 5.5), motion.y(0.5f), 0.00000001);
        for (int i = 11; i < 45; i++) motion.tick();
        assertFalse(motion.removed());
        motion.tick();
        assertTrue(motion.removed());
        var zero = new RuneMotion(0, 0);
        zero.tick();
        assertTrue(zero.removed(), "Zero-duration runes disappear before rendering a zero-denominator opacity");
        assertThrows(IllegalArgumentException.class, () -> new RuneMotion(-1, 0));
        assertThrows(ArithmeticException.class, () -> new RuneMotion(Integer.MAX_VALUE, 0));
    }
    @Test void runeWireCarriesOnlyFiniteCosmeticSamplesAndRejectsOverflowAndTrailingBytes() {
        var packet = new tc4revival.network.ScanEffectPacket(ResourceLocation.parse("minecraft:overworld"),
                new net.minecraft.world.phys.Vec3(0.5, 200.25, -0.5), 15, true);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.encode(buffer);
            assertEquals(packet, tc4revival.network.ScanEffectPacket.decode(buffer));
            buffer.clear(); packet.encode(buffer); buffer.writeByte(0);
            assertThrows(IllegalArgumentException.class, () -> tc4revival.network.ScanEffectPacket.decode(buffer));
        } finally { buffer.release(); }
        assertThrows(IllegalArgumentException.class, () -> new tc4revival.network.ScanEffectPacket(packet.dimension(),
                new net.minecraft.world.phys.Vec3(Double.NaN, 0, 0), 15, false));
        assertThrows(IllegalArgumentException.class, () -> new tc4revival.network.ScanEffectPacket(packet.dimension(), packet.origin(), Integer.MAX_VALUE, false));
    }
    @Test void notificationQueuePreservesFirstBonusRenewalAndExpiredFirstSlotRule() {
        var timeline = new NotificationTimeline();
        timeline.add("first", null, 0xffffff, 10000, 5000);
        timeline.add("second", "aer", 0xffff7e, 10000, 5000);
        var snapshot = timeline.notices(14000, 5000);
        assertEquals(11250, snapshot.get(0).created());
        assertEquals(17500, snapshot.get(0).expires());
        assertEquals(19000, snapshot.get(1).expires());
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        var next = timeline.notices(17501, 5000);
        assertEquals(1, next.size());
        assertEquals("second", next.get(0).text());
        assertEquals(22501, next.get(0).expires(), "A surviving former second slot is renewed even when the first expired");
        assertEquals(1, timeline.notices(22501, 5000).size());
        assertTrue(timeline.notices(22502, 5000).isEmpty());
        timeline.add("third", null, 0xffffff, 23000, 5000);
        assertEquals(30500, timeline.notices(23000, 5000).get(0).expires());
        timeline.clear();
        assertTrue(timeline.notices(23000, 5000).isEmpty());
    }
    @Test void flightsRetainDelayedStartAndInclusiveExpiryAndAreClearedOnDisconnect() {
        var timeline = new NotificationTimeline();
        timeline.fly("aer", 0.4f, 0.6f, 10999);
        var flight = timeline.flights(10000).get(0);
        assertEquals(12499, flight.expires());
        assertEquals(flight, timeline.flights(12499).get(0));
        assertTrue(timeline.flights(12500).isEmpty());
        timeline.fly("ignis", 0.5f, 0.5f, 20000);
        timeline.clear();
        assertTrue(timeline.flights(20000).isEmpty());
    }
    @Test void retainedScannerMeshHas24QuadsAndExactBoundsAndUvConvention() throws Exception {
        var stream = tc4revival.resources.ExternalTestResources.open("/assets/thaumcraft/models/scanner.obj");
        assertNotNull(stream);
        var mesh = ScannerMesh.read(new InputStreamReader(stream, StandardCharsets.UTF_8));
        assertEquals(96, mesh.vertices().size());
        assertEquals(-1.4, mesh.vertices().stream().mapToDouble(ScannerMesh.Vertex::z).min().orElseThrow(), 0.0001);
        assertEquals(1.4, mesh.vertices().stream().mapToDouble(ScannerMesh.Vertex::z).max().orElseThrow(), 0.0001);
        assertEquals(0.2, mesh.vertices().stream().mapToDouble(ScannerMesh.Vertex::y).max().orElseThrow(), 0.0001);
        assertThrows(UnsupportedOperationException.class, () -> mesh.vertices().clear());
        var tiny = "v 0 0 0\nv 1 0 0\nv 1 1 0\nv 0 1 0\nvt 0.25 0.75\nvn 0 0 -1\nf 1/1/1 2/1/1 3/1/1 4/1/1\n";
        assertEquals(0.25f, ScannerMesh.read(new StringReader(tiny)).vertices().get(0).v());
        assertThrows(java.io.IOException.class, () -> ScannerMesh.read(new StringReader(tiny.replace("4/1/1", "5/1/1"))));
        assertThrows(java.io.IOException.class, () -> ScannerMesh.read(new StringReader(tiny.replace(" 4/1/1", ""))));
        assertThrows(java.io.IOException.class, () -> ScannerMesh.read(new StringReader("v NaN 0 0")));
    }
    @Test void feedbackWirePreservesOrderDiscoveryAndFullIntegerRewardsAndRejectsMalformedPackets() {
        var packet = new ScanFeedbackPacket(ScanRules.Status.SUCCESS, "", List.of(
                new ScanFeedbackPacket.Reward("vitreus", 65536, true), new ScanFeedbackPacket.Reward("aer", (short) 17, false)));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.encode(buffer);
            assertEquals(packet, ScanFeedbackPacket.decode(buffer));
            buffer.clear();
            packet.encode(buffer);
            buffer.writeByte(1);
            assertThrows(IllegalArgumentException.class, () -> ScanFeedbackPacket.decode(buffer));
            buffer.clear();
            buffer.writeEnum(ScanRules.Status.SUCCESS).writeUtf("").writeVarInt(1025);
            assertThrows(IllegalArgumentException.class, () -> ScanFeedbackPacket.decode(buffer));
        } finally { buffer.release(); }
        assertEquals(65536, packet.rewards().get(0).gainedAmount());
        assertEquals(Short.MAX_VALUE, packet.rewards().get(0).gained());
        assertThrows(IllegalArgumentException.class, () -> new ScanFeedbackPacket(ScanRules.Status.SUCCESS, "", List.of(new ScanFeedbackPacket.Reward("aer", -1, false))));
        assertThrows(IllegalArgumentException.class, () -> new ScanFeedbackPacket(ScanRules.Status.NO_ASPECTS, "", packet.rewards()));
        assertThrows(IllegalArgumentException.class, () -> new ScanFeedbackPacket(ScanRules.Status.MISSING_PARENT, "", List.of()));
    }
    @Test void lensWirePreservesLocalizedNameAndAspectOrderWithoutLeakingMutableComponents() {
        var name = Component.translatable("block.minecraft.glass");
        var values = new ArrayList<LensPacket.Value>();
        values.add(new LensPacket.Value("vitreus", 3));
        values.add(new LensPacket.Value("aer", 1));
        var packet = new LensPacket(ResourceLocation.parse("minecraft:overworld"), name, values);
        values.clear();
        name.append("changed");
        packet.name().copy().append("also changed");
        assertEquals(Component.translatable("block.minecraft.glass"), packet.name());
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.encode(buffer);
            assertEquals(packet, LensPacket.decode(buffer));
            buffer.clear();
            packet.encode(buffer);
            buffer.writeByte(1);
            assertThrows(IllegalArgumentException.class, () -> LensPacket.decode(buffer));
        } finally { buffer.release(); }
    }
}
