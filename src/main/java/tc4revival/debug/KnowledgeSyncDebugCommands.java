package tc4revival.debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import tc4revival.aspect.TCAspects;
import tc4revival.network.KnowledgePacket;
import tc4revival.network.TCNetwork;
import tc4revival.research.KnowledgeNbt;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.PlayerKnowledge;
import tc4revival.research.PlayerKnowledge.ScanKind;
import tc4revival.research.PlayerKnowledge.WarpKind;
import tc4revival.research.sync.KnowledgeDiagnostics;
import tc4revival.research.sync.KnowledgePublisher;
import tc4revival.research.sync.KnowledgeTransfer;
import tc4revival.research.sync.KnowledgeTransfer.Frame;

/** Uses the registered network encoder and actual saved knowledge, with an in-memory receiver. */
 class KnowledgeSyncDebugCommands {
    private static final UUID FIXTURE = UUID.nameUUIDFromBytes("tc4revival:validation:knowledge:v1".getBytes(StandardCharsets.UTF_8));
    private KnowledgeSyncDebugCommands() { }
    static ArgumentBuilder<CommandSourceStack, ?> tree() {
        return Commands.literal("knowledge_sync").then(Commands.literal("verify").executes(context -> verify(context.getSource())))
                .then(Commands.literal("inspect").then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                    var player = EntityArgument.getPlayer(context, "player");
                    var state = KnowledgeSavedData.get(player.getServer()).snapshot(player.getUUID()).orElseThrow();
                    context.getSource().sendSuccess(() -> Component.literal("Server knowledge: " + KnowledgeDiagnostics.describe(state)), false);
                    return 1;
                })))
                .then(Commands.literal("exercise").then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                    var player = EntityArgument.getPlayer(context, "player");
                    if (!player.getGameProfile().getName().equals("TC4Tester")) throw new IllegalArgumentException("Exercise is restricted to the designated TC4Tester test account");
                    KnowledgeSavedData.get(player.getServer()).update(player.getUUID(), player.getRandom()::nextInt, state -> {
                        state.wipe();
                        state.setPool(TCAspects.catalog().require("aer"), (short) 17);
                        state.setPool(TCAspects.catalog().require("vitreus"), Short.MIN_VALUE);
                        state.rememberResearch("@TC4REVIVAL_NETWORK");
                        state.rememberScan(ScanKind.OBJECT, "@network_object");
                        state.rememberScan(ScanKind.ENTITY, "#network_entity");
                        for (int i = 0; i < 700; i++) state.rememberScan(ScanKind.PHENOMENON, "#network_node" + i);
                        state.setWarp(WarpKind.PERMANENT, 3); state.setWarp(WarpKind.STICKY, 5); state.setWarp(WarpKind.TEMPORARY, 7);
                        state.setWarpCounter(11);
                        return true;
                    });
                    context.getSource().sendSuccess(() -> Component.literal("TC4Tester network fixture applied; requires saved knowledge backup before testing."), false);
                    return 1;
                })))
                .then(Commands.literal("wipe_fixture").then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                    var player = EntityArgument.getPlayer(context, "player");
                    if (!player.getGameProfile().getName().equals("TC4Tester")) throw new IllegalArgumentException("Exercise is restricted to TC4Tester");
                    KnowledgeSavedData.get(player.getServer()).update(player.getUUID(), player.getRandom()::nextInt, state -> { state.wipe(); return true; });
                    context.getSource().sendSuccess(() -> Component.literal("TC4Tester knowledge wiped; legacy counter preserved."), false);
                    return 1;
                })));
    }
    private static int verify(CommandSourceStack source) {
        var saved = KnowledgeSavedData.get(source.getServer()).snapshot(FIXTURE).orElseThrow();
        var original = KnowledgeNbt.write(saved);
        var receiver = new KnowledgeTransfer.Receiver(FIXTURE);
        var publisher = new KnowledgePublisher();
        var frames = new ArrayList<Frame>();
        var bytes = new AtomicInteger();
        publisher.publish(FIXTURE, saved, false, frames::add);
        deliver(frames, receiver, bytes);
        same(saved, receiver);
        require(publisher.publish(FIXTURE, saved, false, ignored -> { throw new IllegalStateException("No-op sent data"); }) == 0, "No-op not suppressed");

        for (int i = 0; i < 700; i++) saved.rememberScan(ScanKind.PHENOMENON, "#validation_network_node" + i);
        saved.setPool(TCAspects.catalog().require("vitreus"), Short.MIN_VALUE);
        frames.clear(); publisher.publish(FIXTURE, saved, false, frames::add);
        require(frames.size() > 5 && !frames.get(0).reset(), "Expected paged delta");
        var before = receiver.snapshot().orElseThrow();
        for (int i = 0; i < frames.size(); i++) {
            receiver.accept(wire(frames.get(i), bytes));
            if (i < frames.size() - 1) require(receiver.snapshot().orElseThrow() == before, "Partial update became visible");
        }
        same(saved, receiver);
        long revision = receiver.revision();
        frames.clear(); publisher.publish(FIXTURE, saved, true, frames::add);
        require(frames.get(0).reset(), "Respawn replacement not sent");
        deliver(frames, receiver, bytes);
        require(receiver.revision() == revision + 1, "Connection revision restarted on forced sync");
        same(saved, receiver);
        saved.wipe();
        frames.clear(); publisher.publish(FIXTURE, saved, false, frames::add);
        deliver(frames, receiver, bytes); same(saved, receiver);
        var reconnected = new KnowledgeTransfer.Receiver(FIXTURE);
        require(reconnected.snapshot().isEmpty(), "Fresh connection kept old world knowledge");
        frames.clear(); new KnowledgePublisher().publish(FIXTURE, new PlayerKnowledge(TCAspects.catalog()), false, frames::add);
        deliver(frames, reconnected, bytes);
        same(new PlayerKnowledge(TCAspects.catalog()), reconnected);
        boolean rejected = false;
        try { reconnected.accept(frames.get(0)); } catch (IllegalArgumentException expected) { rejected = true; }
        require(rejected, "Replayed revision accepted");
        require(original.equals(KnowledgeNbt.write(KnowledgeSavedData.get(source.getServer()).snapshot(FIXTURE).orElseThrow())), "Protocol test mutated saved knowledge");
        source.sendSuccess(() -> Component.literal("Knowledge synchronization checks passed: registered S2C codec, stored fixture, "
                + "700-record atomic delta, negative short pool, no-op suppression, forced replacement, wipe/counter, fresh connection and replay rejection; "
                + bytes.get() + " encoded bytes; world data unchanged. Actual client connection remains pending."), false);
        return 1;
    }
    private static void deliver(ArrayList<Frame> frames, KnowledgeTransfer.Receiver receiver, AtomicInteger bytes) {
        frames.forEach(frame -> receiver.accept(wire(frame, bytes)));
    }
    private static Frame wire(Frame frame, AtomicInteger bytes) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            TCNetwork.CHANNEL.encodeMessage(new KnowledgePacket(frame), buffer);
            bytes.addAndGet(buffer.readableBytes());
            require(buffer.readUnsignedByte() == 0, "Wrong registered packet discriminator");
            var decoded = KnowledgePacket.decode(buffer).frame();
            require(decoded.equals(frame) && !buffer.isReadable(), "Registered codec round trip changed data");
            return decoded;
        } finally { buffer.release(); }
    }
    private static void same(PlayerKnowledge expected, KnowledgeTransfer.Receiver receiver) {
        require(KnowledgeNbt.write(expected).equals(KnowledgeNbt.write(receiver.snapshot().orElseThrow().toKnowledge(TCAspects.catalog()))), "Knowledge changed over network encoding");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
