package tc4revival.debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;
import tc4revival.research.KnowledgeNbt;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.PlayerKnowledge;
import tc4revival.research.PlayerKnowledge.ScanKind;
import tc4revival.research.PlayerKnowledge.ScanMode;
import tc4revival.research.PlayerKnowledge.WarpKind;
import tc4revival.research.ScanRules;

/** Persistent test data belongs to a dedicated non-player UUID, never the connected player. */
 class KnowledgeDebugCommands {
    private static final UUID FIXTURE = UUID.nameUUIDFromBytes("tc4revival:validation:knowledge:v1".getBytes(StandardCharsets.UTF_8));
    private KnowledgeDebugCommands() {}

    static ArgumentBuilder<CommandSourceStack, ?> tree() {
        return Commands.literal("knowledge")
                .then(Commands.literal("verify_scan_reset").executes(context -> verifyScanReset(context.getSource())))
                .then(Commands.literal("clear_scans")
                        .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                .executes(context -> {
                                    var player = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "player");
                                    int count = KnowledgeSavedData.get(player.getServer()).update(player.getUUID(),
                                            player.getRandom()::nextInt, PlayerKnowledge::clearScans);
                                    context.getSource().sendSuccess(() -> Component.literal("Cleared " + count
                                            + " scan records for " + player.getGameProfile().getName()
                                            + "; research, aspect pools and warp preserved."), true);
                                    return 1;
                                })))
                .then(Commands.literal("verify").executes(context -> verify(context.getSource())))
                .then(Commands.literal("fixture")
                        .then(Commands.literal("write").executes(context -> writeFixture(context.getSource())))
                        .then(Commands.literal("verify").executes(context -> verifyFixture(context.getSource()))));
    }

    private static int verify(CommandSourceStack source) {
        var catalog = TCAspects.catalog();
        var player = PlayerKnowledge.firstLogin(catalog, bound -> 2);
        if (player.knownAspects().size() != 6 || player.knownAspects().values().stream().anyMatch(value -> value != 17)) {
            return fail(source, "Initial research pools differ");
        }
        var metal = new AspectAmounts().add(catalog.require("metallum"), 4);
        if (ScanRules.apply(player, catalog, ScanKind.OBJECT, "metal", ScanMode.THAUMOMETER, metal, 100).status()
                != ScanRules.Status.MISSING_PARENT) return fail(source, "Missing-parent scan accepted");
        var glass = new AspectAmounts().add(catalog.require("vitreus"), 2);
        ScanRules.apply(player, catalog, ScanKind.OBJECT, "glass", ScanMode.THAUMOMETER, glass, 100);
        ScanRules.apply(player, catalog, ScanKind.OBJECT, "metal", ScanMode.THAUMOMETER, metal, 100);
        var repeated = ScanRules.apply(player, catalog, ScanKind.OBJECT, "metal", ScanMode.THAUMOMETER, metal, 100);
        if (player.pool(catalog.require("vitreus")) != 4 || player.pool(catalog.require("metallum")) != 6
                || repeated.status() != ScanRules.Status.ALREADY_SCANNED) return fail(source, "Discovery or repeated scan rewards differ");
        var air = catalog.require("aer");
        player.setPool(air, (short) 100);
        if (ScanRules.grant(player, air, 16, 100).gained() != 4) return fail(source, "Soft cap sqrt differs");
        player.setPool(air, (short) 125);
        if (ScanRules.grant(player, air, 16, 100).gained() != 1) return fail(source, "Soft cap one-point threshold differs");
        player.setPool(air, Short.MAX_VALUE);
        ScanRules.grant(player, air, 4, 100);
        if (player.pool(air) != Short.MIN_VALUE) return fail(source, "Legacy short overflow changed");
        var loaded = KnowledgeNbt.read(KnowledgeNbt.write(player), catalog);
        if (!loaded.problems().isEmpty() || !loaded.knowledge().knownAspects().equals(player.knownAspects())) {
            return fail(source, "Knowledge NBT round trip differs");
        }
        source.sendSuccess(() -> Component.literal("Knowledge checks passed: initial pools 15..19; discovery +2; "
                + "parent gating; repeated-scan rejection; sqrt/1-point soft caps; legacy short overflow; NBT round trip."), false);
        return 1;
    }

    private static int verifyScanReset(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var id = UUID.nameUUIDFromBytes("tc4revival:validation:scan_reset:v1".getBytes(StandardCharsets.UTF_8));
        var data = KnowledgeSavedData.get(source.getServer());
        var fake = net.minecraftforge.common.util.FakePlayerFactory.get(source.getLevel(), new com.mojang.authlib.GameProfile(id, "TC4ScanReset"));
        data.update(id, bound -> 2, knowledge -> {
            knowledge.clearScans();
            knowledge.rememberResearch("@RESET_PRESERVE");
            knowledge.setPool(TCAspects.catalog().require("aer"), (short) 39);
            knowledge.setWarp(WarpKind.STICKY, 5);
            knowledge.setWarpCounter(17);
            for (var kind : ScanKind.values()) {
                knowledge.rememberScan(kind, "@normal");
                knowledge.rememberScan(kind, "#enhanced");
            }
            return true;
        });
        var expected = KnowledgeNbt.write(data.snapshot(id).orElseThrow());
        for (var kind : ScanKind.values()) expected.put("scans_" + kind.name().toLowerCase(java.util.Locale.ROOT), new net.minecraft.nbt.ListTag());
        int result = source.getServer().getCommands().getDispatcher().execute("tcdebug knowledge clear_scans @s", source.withEntity(fake).withPermission(2));
        var restored = KnowledgeSavedData.load(data.save(new net.minecraft.nbt.CompoundTag()), TCAspects.catalog()).snapshot(id).orElseThrow();
        if (result != 1 || !expected.equals(KnowledgeNbt.write(restored))) throw new IllegalStateException("Scan reset altered unrelated knowledge or did not persist");
        for (var kind : ScanKind.values()) for (var mode : ScanMode.values())
            if (!restored.canScan(kind, "normal", mode) || !restored.canScan(kind, "enhanced", mode)) throw new IllegalStateException("Scan remains blocked");
        for (var aspect : TCAspects.catalog().all()) aspect.texture();
        source.sendSuccess(() -> Component.literal("Scan reset passed: real command, all three kinds and both modes, persistence, preserved knowledge; aspect textures valid."), false);
        return 1;
    }

    private static int writeFixture(CommandSourceStack source) {
        var catalog = TCAspects.catalog();
        try {
            KnowledgeSavedData.get(source.getServer()).update(FIXTURE, bound -> 2, player -> {
                player.wipe();
                catalog.primals().forEach(aspect -> player.setPool(aspect, (short) 17));
                player.setPool(catalog.require("vitreus"), (short) 4);
                player.rememberResearch("@TC4REVIVAL_VALIDATION");
                player.rememberScan(ScanKind.OBJECT, "#validation_stone");
                player.rememberScan(ScanKind.ENTITY, "@validation_pig");
                player.rememberScan(ScanKind.PHENOMENON, "#validation_node");
                player.setWarp(WarpKind.PERMANENT, 3);
                player.setWarp(WarpKind.STICKY, 5);
                player.setWarp(WarpKind.TEMPORARY, 7);
                player.setWarpCounter(11);
                return true;
            });
        } catch (IllegalStateException exception) {
            return fail(source, exception.getMessage());
        }
        source.sendSuccess(() -> Component.literal("Knowledge fixture written for " + FIXTURE + "; run save-all flush, restart, then fixture verify."), false);
        return 1;
    }

    private static int verifyFixture(CommandSourceStack source) {
        var catalog = TCAspects.catalog();
        var saved = KnowledgeSavedData.get(source.getServer()).snapshot(FIXTURE);
        if (saved.isEmpty()) return fail(source, "Knowledge fixture not present");
        var player = saved.get();
        if (player.knownAspects().size() != 7 || catalog.primals().stream().anyMatch(aspect -> player.pool(aspect) != 17)
                || player.pool(catalog.require("vitreus")) != 4 || !player.researchMarkers().contains("@TC4REVIVAL_VALIDATION")
                || !player.scannedKeys(ScanKind.OBJECT).contains("#validation_stone")
                || !player.scannedKeys(ScanKind.ENTITY).contains("@validation_pig")
                || !player.scannedKeys(ScanKind.PHENOMENON).contains("#validation_node")
                || player.warp(WarpKind.PERMANENT) != 3 || player.warp(WarpKind.STICKY) != 5
                || player.warp(WarpKind.TEMPORARY) != 7 || player.warpCounter() != 11) {
            return fail(source, "Knowledge fixture values differ");
        }
        source.sendSuccess(() -> Component.literal("Knowledge fixture verified: " + FIXTURE
                + "; 7 aspects, research marker, 3 scan categories, warp 3/5/7 and counter 11."), false);
        return 1;
    }

    private static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
}
