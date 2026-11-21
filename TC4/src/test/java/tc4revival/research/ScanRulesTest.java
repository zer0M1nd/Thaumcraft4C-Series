package tc4revival.research;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.research.PlayerKnowledge.ScanKind;
import tc4revival.research.PlayerKnowledge.ScanMode;
import static org.junit.jupiter.api.Assertions.*;

class ScanRulesTest {
    private final AspectCatalog catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private Aspect aspect(String tag) { return catalog.require(tag); }
    private ScanRules.Result scan(PlayerKnowledge player, String id, ScanMode mode, AspectAmounts amounts) {
        return ScanRules.apply(player, catalog, ScanKind.OBJECT, id, mode, amounts, 100);
    }

    @Test void discoveringCrystalAwardsBasePlusTwoAndEnablesMetal() {
        var player = new PlayerKnowledge(catalog);
        var blocked = scan(player, "metal", ScanMode.THAUMOMETER, new AspectAmounts().add(aspect("metallum"), 4));
        assertEquals(ScanRules.Status.MISSING_PARENT, blocked.status());
        assertEquals(aspect("vitreus"), blocked.missingParent().orElseThrow());
        assertFalse(player.scanned(ScanKind.OBJECT, "metal"));
        var crystal = scan(player, "crystal", ScanMode.THAUMOMETER, new AspectAmounts().add(aspect("vitreus"), 2));
        assertTrue(crystal.succeeded());
        assertTrue(crystal.rewards().get(0).discovery());
        assertEquals(4, player.pool(aspect("vitreus")));
        assertTrue(scan(player, "metal", ScanMode.THAUMOMETER, new AspectAmounts().add(aspect("metallum"), 4)).succeeded());
        assertEquals(6, player.pool(aspect("metallum")));
    }

    @Test void allParentsAreCheckedBeforeGrantingAnyPartOfAMixedTarget() {
        var player = new PlayerKnowledge(catalog);
        var mixed = new AspectAmounts().add(aspect("vitreus"), 2).add(aspect("metallum"), 4);
        assertEquals(ScanRules.Status.MISSING_PARENT, scan(player, "mixed", ScanMode.THAUMOMETER, mixed).status());
        assertFalse(player.knows(aspect("vitreus")));
        assertFalse(player.scanned(ScanKind.OBJECT, "mixed"));
    }

    @Test void repeatedCompletionCannotMintResearchPoints() {
        var player = new PlayerKnowledge(catalog);
        var target = new AspectAmounts().add(aspect("aer"), 4);
        assertTrue(scan(player, "same", ScanMode.THAUMOMETER, target).succeeded());
        for (int repeat = 0; repeat < 10; repeat++) {
            assertEquals(ScanRules.Status.ALREADY_SCANNED, scan(player, "same", ScanMode.THAUMOMETER, target).status());
        }
        assertEquals(4, player.pool(aspect("aer")));
    }

    @Test void enhancedScanAfterThaumometerOnlyAddsOnePerAspect() {
        var player = new PlayerKnowledge(catalog);
        var target = new AspectAmounts().add(aspect("vitreus"), 4);
        scan(player, "same", ScanMode.THAUMOMETER, target);
        assertEquals(6, player.pool(aspect("vitreus")));
        scan(player, "same", ScanMode.ENHANCED, target);
        assertEquals(7, player.pool(aspect("vitreus")));
        assertEquals(ScanRules.Status.ALREADY_SCANNED, scan(player, "same", ScanMode.ENHANCED, target).status());
        assertEquals(ScanRules.Status.ALREADY_SCANNED, scan(player, "same", ScanMode.THAUMOMETER, target).status());
    }

    @Test void enhancedFirstScanAddsBaseOneAndDiscoveryBonus() {
        var player = new PlayerKnowledge(catalog);
        scan(player, "same", ScanMode.ENHANCED, new AspectAmounts().add(aspect("vitreus"), 4));
        assertEquals(7, player.pool(aspect("vitreus")));
    }

    @Test void softCapChangesGainsAtExactlyOneAndOneQuarterThresholds() {
        var player = new PlayerKnowledge(catalog);
        player.setPool(aspect("aer"), (short) 99);
        assertEquals(16, ScanRules.grant(player, aspect("aer"), 16, 100).gained());
        assertEquals(115, player.pool(aspect("aer")), "100 is not a hard storage cap");
        player.setPool(aspect("aer"), (short) 100);
        assertEquals(4, ScanRules.grant(player, aspect("aer"), 16, 100).gained());
        player.setPool(aspect("aer"), (short) 124);
        assertEquals(4, ScanRules.grant(player, aspect("aer"), 16, 100).gained());
        player.setPool(aspect("aer"), (short) 125);
        assertEquals(1, ScanRules.grant(player, aspect("aer"), 16, 100).gained());
    }

    @Test void fractionalThresholdUsesTheLegacyFloatComparison() {
        var player = new PlayerKnowledge(catalog);
        player.setPool(aspect("aer"), (short) 126);
        assertEquals(4, ScanRules.grant(player, aspect("aer"), 16, 101).gained());
        player.setPool(aspect("aer"), (short) 127);
        assertEquals(1, ScanRules.grant(player, aspect("aer"), 16, 101).gained());
    }

    @Test void emptyScanIsRecordedWithoutInventingAspects() {
        var player = new PlayerKnowledge(catalog);
        assertEquals(ScanRules.Status.NO_ASPECTS, scan(player, "new_vanilla", ScanMode.THAUMOMETER, null).status());
        assertEquals(ScanRules.Status.NO_ASPECTS, scan(player, "empty", ScanMode.THAUMOMETER, new AspectAmounts()).status());
        assertTrue(player.scanned(ScanKind.OBJECT, "new_vanilla"));
        assertEquals(ScanRules.Status.ALREADY_SCANNED,scan(player,"new_vanilla",ScanMode.THAUMOMETER,null).status());
        assertTrue(player.scanned(ScanKind.OBJECT,"empty"));
        assertTrue(scan(player, "zero", ScanMode.THAUMOMETER, new AspectAmounts().add(aspect("vitreus"), 0)).succeeded());
        assertEquals(2, player.pool(aspect("vitreus")));
    }
}
