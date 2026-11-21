package tc4revival.research;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.network.KnowledgePacket;
import tc4revival.research.PlayerKnowledge.ScanKind;
import tc4revival.research.PlayerKnowledge.WarpKind;
import tc4revival.research.sync.KnowledgePublisher;
import tc4revival.research.sync.KnowledgeSnapshot;
import tc4revival.research.sync.KnowledgeSnapshot.Field;
import tc4revival.research.sync.KnowledgeSnapshot.Key;
import tc4revival.research.sync.KnowledgeTransfer;
import tc4revival.research.sync.KnowledgeTransfer.Edit;
import tc4revival.research.sync.KnowledgeTransfer.Frame;
import static org.junit.jupiter.api.Assertions.*;

class KnowledgeSyncTest {
    private final AspectCatalog catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private final UUID owner = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
    private PlayerKnowledge fixture() {
        var state = PlayerKnowledge.firstLogin(catalog, bound -> 2);
        state.setPool(catalog.require("vitreus"), Short.MIN_VALUE);
        state.rememberResearch("@知识测试");
        state.rememberGuidance("TARGET", "RESEARCH:PARENT");
        for (ScanKind kind : ScanKind.values()) state.rememberScan(kind, "@old_" + kind);
        state.setWarp(WarpKind.PERMANENT, 3); state.setWarp(WarpKind.STICKY, 5); state.setWarp(WarpKind.TEMPORARY, 7);
        state.setWarpCounter(-21);
        return state;
    }
    private Frame wire(Frame frame) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new KnowledgePacket(frame).encode(buffer);
            assertTrue(buffer.readableBytes() < 131072);
            var decoded = KnowledgePacket.decode(buffer).frame();
            assertEquals(frame, decoded);
            assertFalse(buffer.isReadable());
            return decoded;
        } finally { buffer.release(); }
    }
    private void equalKnowledge(PlayerKnowledge expected, KnowledgeTransfer.Receiver receiver) {
        var view = receiver.snapshot().orElseThrow();
        assertEquals(KnowledgeNbt.write(expected), KnowledgeNbt.write(view.toKnowledge(catalog)));
        var actual = KnowledgeSnapshot.capture(view.toKnowledge(catalog));
        var wanted = KnowledgeSnapshot.capture(expected);
        for (var field : Field.values()) assertEquals(wanted.keys(field), actual.keys(field));
    }

    @Test void fullSnapshotPreservesAllCategoriesAndFullIntegerBalancesAndIsImmutable() {
        var state = fixture();
        state.setPool(catalog.require("aer"), Short.MAX_VALUE);
        state.changePool(catalog.require("aer"), (short) 1);
        var captured = KnowledgeSnapshot.capture(state);
        assertThrows(UnsupportedOperationException.class, () -> captured.entries().clear());
        var receiver = new KnowledgeTransfer.Receiver(owner);
        KnowledgeTransfer.create(owner, 1, null, captured).forEach(frame -> receiver.accept(wire(frame)));
        equalKnowledge(state, receiver);
        assertEquals(Short.MAX_VALUE, receiver.snapshot().orElseThrow().toKnowledge(catalog).pool(catalog.require("aer")));
        assertEquals(32768, receiver.snapshot().orElseThrow().toKnowledge(catalog).poolAmount(catalog.require("aer")));
        captured.toKnowledge(catalog).wipe();
        equalKnowledge(state, receiver);
    }

    @Test void moreThanSignedShortManyRecordsArePagedAndNeverBecomePartiallyVisible() {
        var state = fixture();
        for (int i = 0; i < 33000; i++) state.rememberScan(ScanKind.PHENOMENON, "#node" + i);
        var frames = KnowledgeTransfer.create(owner, 1, null, KnowledgeSnapshot.capture(state));
        assertTrue(frames.size() > 250);
        var receiver = new KnowledgeTransfer.Receiver(owner);
        for (int i = 0; i < frames.size() - 1; i++) {
            assertFalse(receiver.accept(wire(frames.get(i))));
            assertTrue(receiver.snapshot().isEmpty());
        }
        assertTrue(receiver.accept(wire(frames.get(frames.size() - 1))));
        equalKnowledge(state, receiver);
    }

    @Test void deltasRemoveUpgradedScanMarkersAndWipedCompoundsAndClearCounter() {
        var state = fixture();
        var publisher = new KnowledgePublisher();
        var receiver = new KnowledgeTransfer.Receiver(owner);
        publisher.publish(owner, state, false, frame -> receiver.accept(wire(frame)));
        var before = receiver.snapshot().orElseThrow();
        state.rememberScan(ScanKind.OBJECT, "#old_OBJECT");
        state.forgetResearch("@知识测试");
        state.setPool(catalog.require("vitreus"), (short) 42);
        var sent = new ArrayList<Frame>();
        publisher.publish(owner, state, false, sent::add);
        assertEquals(4, sent.stream().mapToInt(frame -> frame.edits().size()).sum());
        assertFalse(sent.get(0).reset());
        assertSame(before, receiver.snapshot().orElseThrow());
        sent.forEach(frame -> receiver.accept(wire(frame)));
        equalKnowledge(state, receiver);
        state.wipe();
        publisher.publish(owner, state, false, frame -> receiver.accept(wire(frame)));
        equalKnowledge(state, receiver);
        assertFalse(receiver.snapshot().orElseThrow().entries().containsKey(new Key(Field.ASPECT, "vitreus")));
        assertEquals(0, receiver.snapshot().orElseThrow().toKnowledge(catalog).warpCounter());
    }

    @Test void unchangedUpdatesAreSilentRespawnResendsAndFreshConnectionStartsEmpty() {
        var publisher = new KnowledgePublisher();
        var state = fixture();
        var receiver = new KnowledgeTransfer.Receiver(owner);
        publisher.publish(owner, state, false, frame -> receiver.accept(wire(frame)));
        assertEquals(0, publisher.publish(owner, state, false, ignored -> fail("Unchanged knowledge sent")));
        assertEquals(1, publisher.revision());
        publisher.publish(owner, state, true, frame -> { assertTrue(frame.reset()); receiver.accept(wire(frame)); });
        assertEquals(2, receiver.revision());
        equalKnowledge(state, receiver);
        var reconnect = new KnowledgeTransfer.Receiver(owner);
        assertTrue(reconnect.snapshot().isEmpty());
        new KnowledgePublisher().publish(owner, new PlayerKnowledge(catalog), false, frame -> reconnect.accept(wire(frame)));
        equalKnowledge(new PlayerKnowledge(catalog), reconnect);
    }

    @Test void markerReorderingUsesReplacementAndPreservesExactOldOrder() {
        var state = fixture(); state.rememberResearch("B");
        var old = KnowledgeSnapshot.capture(state);
        state.forgetResearch("@知识测试"); state.rememberResearch("@知识测试");
        var frames = KnowledgeTransfer.create(owner, 2, old, KnowledgeSnapshot.capture(state));
        assertTrue(frames.get(0).reset());
        var receiver = new KnowledgeTransfer.Receiver(owner);
        KnowledgeTransfer.create(owner, 1, null, old).forEach(receiver::accept);
        frames.forEach(frame -> receiver.accept(wire(frame)));
        equalKnowledge(state, receiver);
    }

    @Test void missingDuplicateWrongOwnerAndInvalidPagesCannotReplaceCommittedState() {
        var original = fixture();
        var receiver = new KnowledgeTransfer.Receiver(owner);
        KnowledgeTransfer.create(owner, 1, null, KnowledgeSnapshot.capture(original)).forEach(receiver::accept);
        var state = fixture();
        for (int i = 0; i < 500; i++) state.rememberResearch("R" + i);
        var frames = KnowledgeTransfer.create(owner, 2, KnowledgeSnapshot.capture(original), KnowledgeSnapshot.capture(state));
        receiver.accept(frames.get(0));
        equalKnowledge(original, receiver);
        assertThrows(IllegalArgumentException.class, () -> receiver.accept(frames.get(2)));
        equalKnowledge(original, receiver);
        assertThrows(IllegalArgumentException.class, () -> receiver.accept(new Frame(UUID.randomUUID(), 2, 0, true, true, List.of())));
        assertThrows(IllegalArgumentException.class, () -> receiver.accept(new Frame(owner, 1, 0, true, true, List.of())));
        assertThrows(IllegalArgumentException.class, () -> receiver.accept(new Frame(owner, 2, 0, false, true,
                List.of(new Edit(new Key(Field.ASPECT, "aer"), -1)))));
        equalKnowledge(original, receiver);
        frames.forEach(receiver::accept);
        equalKnowledge(state, receiver);
    }

    @Test void longUnicodeKeysRespectBytePageBudgetAndRemainLossless() {
        var state = fixture();
        state.rememberResearch("字".repeat(32767));
        for (int i = 0; i < 12; i++) state.rememberResearch(i + "魔".repeat(4000));
        var frames = KnowledgeTransfer.create(owner, 1, null, KnowledgeSnapshot.capture(state));
        assertTrue(frames.size() > 6);
        var receiver = new KnowledgeTransfer.Receiver(owner);
        frames.forEach(frame -> receiver.accept(wire(frame)));
        equalKnowledge(state, receiver);
    }

    @Test void malformedCountsTrailingBytesAndTruncatedBuffersAreRejected() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeUUID(owner); buffer.writeVarLong(1); buffer.writeVarInt(0);
            buffer.writeBoolean(true); buffer.writeBoolean(true); buffer.writeVarInt(129);
            assertThrows(IllegalArgumentException.class, () -> KnowledgePacket.decode(buffer));
            buffer.clear();
            var frame = KnowledgeTransfer.create(owner, 1, null, KnowledgeSnapshot.capture(fixture())).get(0);
            new KnowledgePacket(frame).encode(buffer); buffer.writeByte(0);
            assertThrows(IllegalArgumentException.class, () -> KnowledgePacket.decode(buffer));
            buffer.clear();
            new KnowledgePacket(frame).encode(buffer); buffer.writerIndex(buffer.writerIndex() - 1);
            assertThrows(IndexOutOfBoundsException.class, () -> KnowledgePacket.decode(buffer));
        } finally { buffer.release(); }
    }

    @Test void failedPublishAndFailedWorldTransactionDoNotAdvanceTheirRevisions() {
        var publisher = new KnowledgePublisher();
        assertThrows(IllegalStateException.class, () -> publisher.publish(owner, fixture(), false, ignored -> { throw new IllegalStateException(); }));
        assertEquals(0, publisher.revision());
        var receiver = new KnowledgeTransfer.Receiver(owner);
        publisher.publish(owner, fixture(), false, receiver::accept);
        equalKnowledge(fixture(), receiver);
        var world = new KnowledgeSavedData(catalog);
        world.ensure(owner, bound -> 2);
        long before = world.revision(owner);
        assertThrows(IllegalStateException.class, () -> world.update(owner, bound -> 2, state -> { state.wipe(); throw new IllegalStateException(); }));
        assertEquals(before, world.revision(owner));
        world.update(owner, bound -> 2, state -> { state.wipe(); return true; });
        assertEquals(before + 1, world.revision(owner));
    }
}
