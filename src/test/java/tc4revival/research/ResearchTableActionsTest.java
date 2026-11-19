package tc4revival.research;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.research.ResearchTableActions.*;
import static org.junit.jupiter.api.Assertions.*;

class ResearchTableActionsTest {
    @Test void recursiveSynthesisReservesAllCostsBeforeCommittingAndConsumesBonusesOnce() {
        var player=player();player.rememberResearch("RESEARCHER2");
        pool(player,"vitreus",0);pool(player,"terra",1);pool(player,"ordo",0);
        var before=KnowledgeNbt.write(player);
        rejected(actions.synthesize(empty(Set.of()),player,"vitreus",100),Rejection.NO_POINTS);
        assertEquals(before,KnowledgeNbt.write(player));
        var result=actions.synthesize(empty(Set.of("ordo")),player,"vitreus",100).change().orElseThrow();
        assertEquals(1,pool(result.knowledge(),"vitreus"));
        assertEquals(0,pool(result.knowledge(),"terra"));assertTrue(result.table().bonuses().isEmpty());
        assertEquals(before,KnowledgeNbt.write(player));
    }
    private final AspectCatalog aspects = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private final ResearchCatalog catalog = catalog();
    private final ResearchTableActions actions = new ResearchTableActions(aspects, catalog, entry -> true);
    private final ResearchHex middle = new ResearchHex(0, 0);
    private ResearchCatalog catalog() {
        try (var reader = new InputStreamReader(java.util.Objects.requireNonNull(getClass().getResourceAsStream("/research-catalog.json")), StandardCharsets.UTF_8)) {
            return ResearchCatalog.decode(JsonParser.parseReader(reader).getAsJsonArray(), aspects);
        } catch (java.io.IOException failure) { throw new AssertionError(failure); }
    }
    private PlayerKnowledge player() { return new PlayerKnowledge(aspects); }
    private ResearchNote board(ResearchNote.Cell cell) {
        return new ResearchNote("THAUMIUM", 123, false, 0, Map.of(middle, cell,
                new ResearchHex(-1, 0), new ResearchNote.Cell(1, "terra"),
                new ResearchHex(1, 0), new ResearchNote.Cell(1, "ordo")));
    }
    private State table(ResearchNote.Cell cell, int ink, Set<String> bonuses) {
        return new State(Optional.of(board(cell)), 1, ink, bonuses);
    }
    private State empty(Set<String> bonuses) { return new State(Optional.empty(), 0, 0, bonuses); }
    private short pool(PlayerKnowledge player, String tag) { return player.pool(aspects.require(tag)); }
    private void pool(PlayerKnowledge player, String tag, int value) { player.setPool(aspects.require(tag), (short) value); }
    private final RandomFloat noRoll = () -> { throw new AssertionError("Unexpected random draw"); };
    private RandomFloat rolls(float... values) {
        var index = new int[] {0};
        return () -> { assertTrue(index[0] < values.length, "Too many random draws"); return values[index[0]++]; };
    }
    private void rejected(Result result, Rejection rejection) {
        assertFalse(result.accepted()); assertEquals(Optional.of(rejection), result.rejection());
    }

    @Test void placementUsesPoolBeforeBonusConsumesLastInkAndCompletesTheWholeNote() {
        var player = player(); pool(player, "vitreus", 1);
        var table = table(ResearchNote.Cell.blank(), 1, Set.of("vitreus"));
        var change = actions.place(table, player, middle, "vitreus", noRoll).change().orElseThrow();
        assertEquals(0, pool(change.knowledge(), "vitreus"));
        assertEquals(Set.of("vitreus"), change.table().bonuses());
        assertEquals(0, change.table().ink());
        assertTrue(change.completedNow());
        assertTrue(change.table().note().orElseThrow().complete());
        assertTrue(change.knowledge().researchMarkers().isEmpty()); // Solving is not right-clicking the discovery.
        assertEquals(1, pool(player, "vitreus"));
        assertFalse(table.note().orElseThrow().complete());
        rejected(actions.place(change.table(), change.knowledge(), middle, null, noRoll), Rejection.COMPLETE_NOTE);
    }

    @Test void placementSpendsBonusAfterNegativeInputIsSanitizedAndRequiresDiscovery() {
        for (int balance : List.of(0, -1)) {
            var player = player(); pool(player, "vitreus", balance);
            var table = table(ResearchNote.Cell.blank(), 4, Set.of("vitreus"));
            var change = actions.place(table, player, middle, "vitreus", noRoll).change().orElseThrow();
            assertEquals(0, pool(change.knowledge(), "vitreus"));
            assertTrue(change.table().bonuses().isEmpty());
            assertEquals(3, change.table().ink());
        }
        rejected(actions.place(table(ResearchNote.Cell.blank(), 4, Set.of("vitreus")), player(), middle, "vitreus", noRoll),
                Rejection.UNKNOWN_ASPECT);
    }

    @Test void researcherTwoFreePlacementStillNeedsInkAndAvailablePointsAndConsumesPitchDraw() {
        var player = player(); player.rememberResearch("RESEARCHER2"); pool(player, "vitreus", 3);
        var table = table(ResearchNote.Cell.blank(), 2, Set.of());
        var free = actions.place(table, player, middle, "vitreus", rolls(Math.nextDown(0.1f), 0.5f)).change().orElseThrow();
        assertEquals(3, pool(free.knowledge(), "vitreus"));
        assertEquals(Optional.of(1.0f), free.orbPitch());
        assertEquals(1, free.table().ink());
        var paid = actions.place(table, player, middle, "vitreus", rolls(0.1f)).change().orElseThrow();
        assertEquals(2, pool(paid.knowledge(), "vitreus")); assertTrue(paid.orbPitch().isEmpty());
        pool(player, "vitreus", 0);
        rejected(actions.place(table, player, middle, "vitreus", noRoll), Rejection.NO_POINTS);
        rejected(actions.place(table(ResearchNote.Cell.blank(), 0, Set.of("vitreus")), player, middle, "vitreus", noRoll), Rejection.NO_INK);
    }

    @Test void erasingAlwaysDrawsAndUpgradeThresholdsAreStrictWithLegacyUnknownAspectRefund() {
        for (int upgrade = 0; upgrade <= 2; upgrade++) {
            var player = player();
            if (upgrade > 0) player.rememberResearch("RESEARCHER" + upgrade);
            float threshold = upgrade == 1 ? 0.25f : 0.5f;
            var table = table(new ResearchNote.Cell(2, "vitreus"), 2, Set.of());
            var misses = actions.place(table, player, middle, null, rolls(threshold)).change().orElseThrow();
            assertEquals(0, pool(misses.knowledge(), "vitreus"));
            assertFalse(misses.knowledge().knows(aspects.require("vitreus")));
            assertTrue(misses.orbPitch().isEmpty());
            assertEquals(1, misses.table().ink());
            if (upgrade == 0) continue;
            var refund = actions.place(table, player, middle, null, rolls(Math.nextDown(threshold), 0.25f)).change().orElseThrow();
            assertEquals(1, pool(refund.knowledge(), "vitreus"));
            assertTrue(refund.knowledge().knows(aspects.require("vitreus"))); // A traded note may contain an unknown aspect.
            assertEquals(Optional.of(0.95f), refund.orbPitch());
            assertTrue(refund.reward().isEmpty()); // Old erase path does not send a discovery reward.
        }
    }

    @Test void invalidEditsAndRandomFailureNeverAlterPlayerOrBoard() {
        var player = player(); pool(player, "terra", 3);
        var table = table(ResearchNote.Cell.blank(), 5, Set.of());
        rejected(actions.place(table, player, new ResearchHex(-1, 0), "terra", noRoll), Rejection.INVALID_CELL);
        rejected(actions.place(table, player, new ResearchHex(4, 0), "terra", noRoll), Rejection.INVALID_CELL);
        rejected(actions.place(table, player, middle, null, noRoll), Rejection.INVALID_CELL);
        rejected(actions.place(table, player, middle, "nonexistent", noRoll), Rejection.UNKNOWN_ASPECT);
        player.rememberResearch("RESEARCHER2");
        assertThrows(IllegalArgumentException.class, () -> actions.place(table, player, middle, "terra", () -> Float.NaN));
        assertEquals(3, pool(player, "terra")); assertEquals(5, table.ink());
        assertEquals(ResearchNote.Cell.blank(), table.note().orElseThrow().grid().get(middle));
    }

    @Test void all42CompoundRecipesWorkBothWaysWithoutPaperInkOrNoteAndDiscoverWithThreePoints() {
        for (var compound : aspects.compounds()) for (boolean reverse : List.of(false, true)) {
            String a = compound.components().get(reverse ? 1 : 0);
            String b = compound.components().get(reverse ? 0 : 1);
            var player = player(); pool(player, a, 1); pool(player, b, 1);
            var change = actions.combine(empty(Set.of()), player, a, b, 100).change().orElseThrow();
            assertEquals(0, pool(change.knowledge(), a)); assertEquals(0, pool(change.knowledge(), b));
            assertEquals(3, pool(change.knowledge(), compound.tag()));
            assertTrue(change.reward().orElseThrow().discovery());
            assertEquals(compound, change.reward().orElseThrow().aspect());
            assertEquals(1, pool(player, a)); assertFalse(player.knows(compound));
            assertEquals(0, change.paper()); assertEquals(0, change.inkSac());
            assertTrue(change.knowledge().researchMarkers().isEmpty());
            for (var kind : PlayerKnowledge.ScanKind.values()) assertTrue(change.knowledge().scannedKeys(kind).isEmpty());
        }
    }

    @Test void combinationsPreferPoolsThenBonusesAndKnownResultsGainOnlyOne() {
        var player = player(); pool(player, "terra", 1); pool(player, "vitreus", 125);
        var change = actions.combine(empty(Set.of("terra", "ordo")), player, "terra", "ordo", 100).change().orElseThrow();
        assertEquals(Set.of("terra"), change.table().bonuses());
        assertEquals(126, pool(change.knowledge(), "vitreus"));
        assertEquals(1, change.reward().orElseThrow().gained()); assertFalse(change.reward().orElseThrow().discovery());
        assertEquals(125, pool(player, "vitreus"));
    }

    @Test void failedCombinationSpendsInputsButIdenticalOperandsRequireTwoPointsAtomically() {
        var player = player(); pool(player, "terra", 1); pool(player, "aer", 1);
        var change = actions.combine(empty(Set.of()), player, "terra", "aer", 100).change().orElseThrow();
        assertTrue(change.reward().isEmpty()); assertEquals(0, pool(change.knowledge(), "terra"));
        assertEquals(0, pool(change.knowledge(), "aer"));
        rejected(actions.combine(change.table(), change.knowledge(), "terra", "aer", 100), Rejection.NO_POINTS);
        var before = KnowledgeNbt.write(player);
        rejected(actions.combine(empty(Set.of()), player, "terra", "terra", 100), Rejection.NO_POINTS);
        assertEquals(before, KnowledgeNbt.write(player));
        pool(player, "terra", 2);
        var identical = actions.combine(empty(Set.of()), player, "terra", "terra", 100).change().orElseThrow();
        assertTrue(identical.reward().isEmpty()); assertEquals(0, pool(identical.knowledge(), "terra"));
        assertEquals(1, pool(identical.knowledge(), "aer"));
        assertTrue(aspects.compounds().stream().allMatch(aspect -> !aspect.components().get(0).equals(aspect.components().get(1))));
    }

    private State completed(int copies, int count) {
        var note = new ResearchNote("THAUMIUM", 123, true, copies, Map.of());
        return new State(Optional.of(note), count, 0, Set.of("terra"));
    }
    private PlayerKnowledge copyPlayer(int extra) {
        var player = player(); player.rememberResearch("RESEARCHDUPE");
        catalog.require("THAUMIUM").aspects().forEach((tag, amount) -> pool(player, tag, amount + extra));
        return player;
    }

    @Test void duplicateChargesEachAspectAndStopsAtNativeStackLimit() {
        for (int count : List.of(1, 63)) {
            var player = copyPlayer(9);
            var table = completed(2, count);
            var change = actions.duplicate(table, player, 1, 1).change().orElseThrow();
            assertEquals(count + 1, change.table().noteCount()); assertEquals(3, change.table().note().orElseThrow().copies());
            assertEquals(1, change.paper()); assertEquals(1, change.inkSac()); assertEquals(0, change.table().ink());
            assertEquals(table.bonuses(), change.table().bonuses());
            catalog.require("THAUMIUM").aspects().forEach((tag, amount) -> {
                assertEquals(7, pool(change.knowledge(), tag)); assertEquals(amount + 9, pool(player, tag));
            });
            assertEquals(2, table.note().orElseThrow().copies());
        }
    }

    @Test void duplicateRejectsMissingResearchMaterialsAndAnyUnaffordableAspectAtomically() {
        var table = completed(2, 1);
        var player = copyPlayer(2);
        player.forgetResearch("RESEARCHDUPE");
        rejected(actions.duplicate(table, player, 1, 1), Rejection.NO_DUPLICATION_RESEARCH);
        player.rememberResearch("RESEARCHDUPE");
        rejected(actions.duplicate(table, player, 0, 1), Rejection.NO_MATERIALS);
        rejected(actions.duplicate(table, player, 1, 0), Rejection.NO_MATERIALS);
        var last = catalog.require("THAUMIUM").aspects().keySet().stream().reduce((a, b) -> b).orElseThrow();
        pool(player, last, 0);
        var before = Map.copyOf(player.knownAspects());
        rejected(actions.duplicate(table, player, 1, 1), Rejection.NO_POINTS);
        assertEquals(before, player.knownAspects()); assertEquals(2, table.note().orElseThrow().copies());
        for (int count : List.of(64, 127)) {
            var full = copyPlayer(9); var snapshot = KnowledgeNbt.write(full);
            rejected(actions.duplicate(completed(2, count), full, 1, 1), Rejection.COPY_LIMIT);
            assertEquals(snapshot, KnowledgeNbt.write(full));
        }
        rejected(actions.duplicate(completed(Integer.MAX_VALUE, 1), player, 1, 1), Rejection.COPY_LIMIT);
        rejected(actions.duplicate(completed(0, Integer.MAX_VALUE), player, 1, 1), Rejection.COPY_LIMIT);
    }
}
