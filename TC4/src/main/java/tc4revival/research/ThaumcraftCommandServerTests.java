package tc4revival.research;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.TC4Revival;
import tc4revival.aspect.TCAspects;
import tc4revival.config.TCConfig;
import tc4revival.research.PlayerKnowledge.WarpKind;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ThaumcraftCommandServerTests {
    @GameTest(template = "wand_test_empty")
    public static void legacyAdministrationCommands(GameTestHelper test) throws CommandSyntaxException {
        var player = new FakePlayer(test.getLevel(), new GameProfile(UUID.randomUUID(), "TCCommandTest"));
        var source = player.createCommandSourceStack().withPermission(2);
        var dispatcher = player.server.getCommands().getDispatcher();
        var data = KnowledgeSavedData.get(player.server);
        boolean disabled = TCConfig.SERVER.disableWarp.get();
        try {
            TCConfig.SERVER.disableWarp.set(false);
            data.update(player.getUUID(), bound -> 0, knowledge -> {
                knowledge.rememberScan(PlayerKnowledge.ScanKind.OBJECT, "@command-test");
                knowledge.rememberResearch("@command-clue");
                knowledge.setWarpCounter(37);
                return null;
            });
            test.assertTrue(dispatcher.execute("tc warp @s set 12 permanent", source) == 1, "Alias, typed actions and self selector execute");
            dispatcher.execute("thaum warp @s add -4 permanent", source);
            dispatcher.execute("thaumcraft warp @s set 6 temporary", source);
            dispatcher.execute("tc warp @s add 7", source);
            dispatcher.execute("tc warp @s add -3", source);
            var state = data.snapshot(player.getUUID()).orElseThrow();
            test.assertTrue(state.warp(WarpKind.PERMANENT) == 8 && state.warp(WarpKind.TEMPORARY) == 6
                    && state.warp(WarpKind.STICKY) == 4 && state.warpCounter() == 37,
                    "Admin warp categories and removal retain the event counter");
            dispatcher.execute("tc aspect @s all 5", source);
            dispatcher.execute("tc aspect @s Praecantatio 2", source);
            state = data.snapshot(player.getUUID()).orElseThrow();
            test.assertTrue(state.knownAspects().size() == TCAspects.catalog().all().size()
                    && state.pool(TCAspects.catalog().require("praecantatio")) == 7, "All aspects and case-insensitive name lookup");
            String key = ResearchCatalogs.catalog().all().stream().filter(ResearchCatalogs::enabled)
                    .filter(entry -> entry.warp() > 0 && !entry.parents().isEmpty()).findFirst().orElseThrow().key();
            dispatcher.execute("tc research @s " + key, source);
            state = data.snapshot(player.getUUID()).orElseThrow();
            var expected = new HashSet<String>();
            collect(key, expected);
            test.assertTrue(state.researchMarkers().containsAll(expected), "Grant includes transitive parents, hidden parents and siblings");
            int expectedWarp = expected.stream().map(ResearchCatalogs.catalog()::find)
                    .mapToInt(entry -> entry.map(ResearchDefinition::warp).orElse(0)).sum();
            test.assertTrue(state.totalWarp() == 18 + expectedWarp, "Each newly completed research awards its warp once");
            dispatcher.execute("tc research @s " + key, source);
            test.assertTrue(data.snapshot(player.getUUID()).orElseThrow().totalWarp() == state.totalWarp(), "Repeated grant does not award twice");
            dispatcher.execute("tc research @s reset", source);
            var reset = data.snapshot(player.getUUID()).orElseThrow();
            var automatic = ResearchCatalogs.catalog().all().stream().filter(ResearchCatalogs::enabled)
                    .filter(entry -> !entry.mirror() && entry.has(ResearchDefinition.Flag.AUTO_UNLOCK)).map(ResearchDefinition::key).collect(java.util.stream.Collectors.toSet());
            test.assertTrue(reset.researchMarkers().equals(automatic) && reset.knownAspects().equals(state.knownAspects())
                    && reset.scannedKeys(PlayerKnowledge.ScanKind.OBJECT).contains("@command-test")
                    && reset.totalWarp() == state.totalWarp(), "Reset restores auto research while preserving pools, scans and warp");
            TCConfig.SERVER.disableWarp.set(true);
            dispatcher.execute("tc research @s all", source);
            var all = data.snapshot(player.getUUID()).orElseThrow();
            var incomplete = ResearchCatalogs.catalog().all().stream().filter(ResearchCatalogs::enabled)
                    .filter(entry -> ResearchCatalogs.enabled(ResearchCatalogs.catalog().original(entry.key())))
                    .filter(entry -> !ResearchCatalogs.rules().completed(all, entry.key()))
                    .map(entry -> entry.key() + "[original=" + ResearchCatalogs.catalog().original(entry.key()).key()
                            + ",originalEnabled=" + ResearchCatalogs.enabled(ResearchCatalogs.catalog().original(entry.key())) + "]")
                    .toList();
            test.assertTrue(incomplete.isEmpty(), "Grant all completes enabled catalog: " + incomplete);
            test.assertTrue(ResearchCatalogs.catalog().all().stream().filter(ResearchDefinition::mirror)
                    .filter(entry -> !ResearchCatalogs.enabled(ResearchCatalogs.catalog().original(entry.key())))
                    .noneMatch(entry -> ResearchCatalogs.rules().completed(all, entry.key())),
                    "Mirrors of disabled original research remain incomplete");
            test.assertTrue(all.totalWarp() == reset.totalWarp(),
                    "Grant all respects disable-warp: before=" + reset.totalWarp() + ",after=" + all.totalWarp());
            test.assertTrue(ResearchCatalogs.catalog().all().stream().filter(ResearchDefinition::mirror)
                    .noneMatch(entry -> all.researchMarkers().contains(entry.key())),
                    "Grant all completes original research without adding independent mirror markers");
            dispatcher.execute("tc warp @s add 1", source);
            test.assertTrue(data.snapshot(player.getUUID()).orElseThrow().warp(WarpKind.STICKY) == all.warp(WarpKind.STICKY) + 1,
                    "Explicit admin warp remains available when gameplay warp is disabled");
            long revision = data.revision(player.getUUID());
            for (String invalid : java.util.List.of("tc warp @s add 101", "tc warp @s set -1", "tc aspect @s all 0",
                    "tc research @s DOES_NOT_EXIST", "tc aspect @s nonexistent 1", "tc research @s all extra")) {
                boolean rejected = false;
                try { dispatcher.execute(invalid, source); } catch (CommandSyntaxException expectedFailure) { rejected = true; }
                test.assertTrue(rejected && data.revision(player.getUUID()) == revision, "Invalid command rejected without mutation: " + invalid);
            }
            boolean denied = false;
            try { dispatcher.execute("tc warp @s set 0", source.withPermission(0)); }
            catch (CommandSyntaxException expectedFailure) { denied = true; }
            test.assertTrue(denied && data.revision(player.getUUID()) == revision, "Non-operator cannot mutate knowledge");
            var restored = KnowledgeSavedData.load(data.save(new CompoundTag()), TCAspects.catalog()).snapshot(player.getUUID()).orElseThrow();
            test.assertTrue(KnowledgeNbt.write(restored).equals(KnowledgeNbt.write(data.snapshot(player.getUUID()).orElseThrow())),
                    "Command changes survive actual saved-data serialization");
        } finally {
            TCConfig.SERVER.disableWarp.set(disabled);
            KnowledgeSync.logout(new PlayerEvent.PlayerLoggedOutEvent(player));
        }
        TC4Revival.LOGGER.info("Legacy administration commands PASS: dispatcher-aliases-permission-research-aspects-warp-reset-persistence");
        test.succeed();
    }

    private static void collect(String key, java.util.Set<String> keys) {
        if (!keys.add(key)) return;
        ResearchCatalogs.catalog().find(key).filter(ResearchCatalogs::enabled).ifPresent(entry -> {
            entry.parents().forEach(parent -> collect(parent, keys));
            entry.hiddenParents().forEach(parent -> collect(parent, keys));
            entry.siblings().forEach(sibling -> collect(sibling, keys));
        });
    }
}
