package tc4revival.research;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.research.PlayerKnowledge.ScanKind;
import tc4revival.research.PlayerKnowledge.ScanMode;
import static org.junit.jupiter.api.Assertions.*;
import static tc4revival.research.ScanProgress.State.*;

class ScanProgressTest {
    @Test void inventoryScanningRequiresFiftyRealIntervalsAndRejectsInvalidDurations() {
        var timer=new ScanProgress(50);var target=block("glass","vitreus",2);
        assertEquals(TRACKING,timer.begin(10,"overworld",target,knowledge));
        for(int tick=10;tick<60;tick++)for(int repeat=0;repeat<8;repeat++)
            assertEquals(TRACKING,timer.observe(tick,"overworld",target,knowledge));
        assertEquals(COMPLETED,timer.observe(60,"overworld",target,knowledge));
        assertEquals(IDLE,timer.observe(60,"overworld",target,knowledge));
        assertThrows(IllegalArgumentException.class,()->new ScanProgress(0));
        assertThrows(IllegalArgumentException.class,()->new ScanProgress(-1));
    }
    private final AspectCatalog catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private final PlayerKnowledge knowledge = new PlayerKnowledge(catalog);
    private final ScanProgress progress = new ScanProgress();
    private ScanTarget block(String item, String aspect, int amount) {
        return target(new ScanTarget.Block(item, 0), item, aspect, amount);
    }
    private ScanTarget target(ScanTarget.Continuity token, String id, String aspect, int amount) {
        return new ScanTarget(token, ScanKind.OBJECT, Optional.ofNullable(id),
                aspect == null ? new AspectAmounts() : new AspectAmounts().add(catalog.require(aspect), amount));
    }
    private ScanProgress.State observe(long tick, ScanTarget target) { return progress.observe(tick, "overworld", target, knowledge); }

    @Test void duplicateCallsCannotShortenTwentyConsecutiveGameTickIntervals() {
        var glass = block("glass", "vitreus", 2);
        assertEquals(TRACKING, progress.begin(100, "overworld", glass, knowledge));
        for (int tick = 100; tick < 120; tick++) {
            for (int duplicate = 0; duplicate < 30; duplicate++) assertEquals(TRACKING, observe(tick, glass));
            assertFalse(knowledge.knows(catalog.require("vitreus")));
        }
        assertEquals(COMPLETED, observe(120, glass));
        assertEquals(IDLE, observe(120, glass));
        assertEquals(IDLE, observe(121, glass));
    }

    @Test void aMissedTickOrReversedClockInterruptsUntilAnotherUse() {
        var glass = block("glass", "vitreus", 2);
        progress.begin(100, "overworld", glass, knowledge);
        assertEquals(INTERRUPTED, observe(102, glass));
        assertEquals(IDLE, observe(103, glass));
        progress.begin(200, "overworld", glass, knowledge);
        assertEquals(INTERRUPTED, observe(199, glass));
    }

    @Test void lostTargetDimensionChangeAndReleaseDoNotResumeTheOldAttempt() {
        var glass = block("glass", "vitreus", 2);
        progress.begin(0, "overworld", glass, knowledge);
        assertEquals(INTERRUPTED, observe(1, null));
        assertEquals(IDLE, observe(2, glass));
        progress.begin(2, "overworld", glass, knowledge);
        assertEquals(INTERRUPTED, progress.observe(3, "nether", glass, knowledge));
        progress.begin(3, "overworld", glass, knowledge);
        progress.cancel();
        assertEquals(IDLE, observe(4, glass));
    }

    @Test void matchingGroupedBlockIdentitiesContinueWithoutRememberingPositions() {
        var first = block("anvil", "metallum", 64);
        var another = block("anvil", "metallum", 64);
        progress.begin(0, "overworld", first, knowledge);
        for (int tick = 1; tick < 20; tick++) assertEquals(TRACKING, observe(tick, another));
        assertEquals(COMPLETED, observe(20, another));
        // Parent rejection occurs after completion, not while moving between these targets.
        assertEquals(ScanRules.Status.MISSING_PARENT, another.complete(knowledge, catalog, ScanMode.THAUMOMETER, 100).status());
    }

    @Test void distinctBlocksCannotContinueMerelyBecauseTheirPersistedHashesCollide() {
        var first = target(new ScanTarget.Block("Aa", 0), "collision", "aer", 1);
        var second = target(new ScanTarget.Block("BB", 0), "collision", "aer", 1);
        progress.begin(0, "overworld", first, knowledge);
        assertEquals(INTERRUPTED, observe(1, second));
        assertEquals(IDLE, observe(2, first));
    }

    @Test void entityContinuityUsesTheEntityNotTheStackOrItsOldHash() {
        var id = UUID.randomUUID();
        var first = target(new ScanTarget.Entity(id), "map_42", null, 0);
        var changed = target(new ScanTarget.Entity(id), "glass", "vitreus", 2);
        progress.begin(0, "overworld", first, knowledge);
        for (int tick = 1; tick < 20; tick++) assertEquals(TRACKING, observe(tick, tick < 10 ? first : changed));
        assertEquals(COMPLETED, observe(20, changed));
        assertTrue(changed.complete(knowledge, catalog, ScanMode.THAUMOMETER, 100).succeeded());
        assertEquals(4, knowledge.pool(catalog.require("vitreus")));
        assertTrue(knowledge.scanned(ScanKind.OBJECT, "glass"));
        assertFalse(knowledge.scanned(ScanKind.OBJECT, "map_42"));
        progress.begin(30, "overworld", first, knowledge);
        var different = target(new ScanTarget.Entity(UUID.randomUUID()), "map_42", null, 0);
        assertEquals(INTERRUPTED, observe(31, different));
    }

    @Test void emptyAndUnmappedTargetsReachCompletionWithoutInventingKnowledge() {
        for (String identity : Arrays.asList("old_empty", null)) {
            var empty = target(new ScanTarget.Block("empty", 0), identity, null, 0);
            assertEquals(TRACKING, progress.begin(0, "overworld", empty, knowledge));
            for (int tick = 1; tick < 20; tick++) assertEquals(TRACKING, observe(tick, empty));
            assertEquals(COMPLETED, observe(20, empty));
            assertEquals(ScanRules.Status.NO_ASPECTS, empty.complete(knowledge, catalog, ScanMode.THAUMOMETER, 100).status());
            assertEquals(java.util.Set.of("@old_empty"), knowledge.scannedKeys(ScanKind.OBJECT));
            if (identity != null) assertEquals(ScanRules.Status.ALREADY_SCANNED, empty.complete(knowledge, catalog, ScanMode.THAUMOMETER, 100).status());
        }
    }

    @Test void alreadyScannedTargetsCannotStartOrContinueAndCannotRewardTwice() {
        var glass = block("glass", "vitreus", 2);
        progress.begin(0, "overworld", glass, knowledge);
        assertTrue(glass.complete(knowledge, catalog, ScanMode.ENHANCED, 100).succeeded());
        assertEquals(INTERRUPTED, observe(1, glass));
        assertEquals(IDLE, progress.begin(2, "overworld", glass, knowledge));
        assertEquals(ScanRules.Status.ALREADY_SCANNED, glass.complete(knowledge, catalog, ScanMode.THAUMOMETER, 100).status());
        assertEquals(5, knowledge.pool(catalog.require("vitreus")));
    }

    @Test void targetAmountsAreIsolatedFromBothCallerAndReaders() {
        var amounts = new AspectAmounts().add(catalog.require("aer"), 1);
        var target = new ScanTarget(new ScanTarget.Block("one", 0), ScanKind.OBJECT, Optional.of("one"), amounts);
        amounts.add(catalog.require("aer"), 99);
        target.aspects().add(catalog.require("aer"), 50);
        assertEquals(1, target.aspects().amount(catalog.require("aer")));
        assertThrows(IllegalArgumentException.class, () -> new ScanTarget(new ScanTarget.Block("new", 0), ScanKind.OBJECT, Optional.empty(), amounts));
    }

    @Test void playersHaveIndependentProgressAndCurrentParentsAreCheckedAtCompletion() {
        var other = new ScanProgress();
        var metal = block("metal", "metallum", 4);
        var glass = block("glass", "vitreus", 2);
        progress.begin(0, "overworld", metal, knowledge);
        other.begin(10, "overworld", glass, knowledge);
        for (int tick = 1; tick < 20; tick++) observe(tick, metal);
        glass.complete(knowledge, catalog, ScanMode.THAUMOMETER, 100);
        assertEquals(COMPLETED, observe(20, metal));
        assertTrue(metal.complete(knowledge, catalog, ScanMode.THAUMOMETER, 100).succeeded());
        assertTrue(other.active(), "one player's completion must not clear another's timer");
    }
}
