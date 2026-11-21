package tc4revival.research;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.research.ResearchDefinition.Flag;
import static org.junit.jupiter.api.Assertions.*;

class ResearchBookActionsTest {
    private final AspectCatalog aspects = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private ResearchDefinition entry(String key, List<String> parents, List<String> siblings, Set<Flag> flags, int cost, int warp) {
        return new ResearchDefinition(key, "BASICS", Map.of("aer", cost), parents, List.of(), siblings, flags, 1, warp,
                new ResearchDefinition.Display(0, 0, Optional.empty(), Optional.empty(), 1),
                new ResearchDefinition.Triggers(Optional.empty(), Optional.empty(), Optional.empty()));
    }
    private ResearchCatalog catalog(ResearchDefinition... entries) {
        return new ResearchCatalog(List.of(new ResearchCatalog.Category("BASICS", "icon", "background", List.of(entries))), aspects);
    }
    private ResearchBookActions actions(ResearchDefinition... entries) { return new ResearchBookActions(aspects, catalog(entries), e -> true); }
    @Test void threeDifficultiesChooseTheOldRoutes() {
        var primary = entry("P", List.of(), List.of(), Set.of(), 3, 0);
        var secondary = entry("S", List.of(), List.of(), Set.of(Flag.SECONDARY), 3, 0);
        assertEquals(ResearchBookActions.Route.PURCHASE, ResearchBookActions.route(primary, -1));
        assertEquals(ResearchBookActions.Route.NOTE, ResearchBookActions.route(primary, 0));
        assertEquals(ResearchBookActions.Route.PURCHASE, ResearchBookActions.route(secondary, 0));
        assertEquals(ResearchBookActions.Route.NOTE, ResearchBookActions.route(secondary, 1));
        assertThrows(IllegalArgumentException.class, () -> ResearchBookActions.route(primary, 2));
    }
    @Test void insufficientPurchaseIsAtomicAndDoesNotCompleteOrAddWarp() {
        var book = actions(entry("P", List.of(), List.of(), Set.of(Flag.SECONDARY), 3, 5));
        var player = PlayerKnowledge.firstLogin(aspects, bound -> 0); player.setPool(aspects.require("aer"), (short) 2);
        var result = book.purchase(player, "P", 0, false);
        assertEquals(ResearchBookActions.Rejection.POINTS, result.rejection().orElseThrow());
        assertEquals(2, player.pool(aspects.require("aer"))); assertTrue(player.researchMarkers().isEmpty()); assertEquals(0, player.totalWarp());
    }
    @Test void purchaseUsesCopyChargesOnceAndAppliesOnlyEligibleSiblings() {
        var book = actions(entry("P", List.of(), List.of("S", "LOCKED"), Set.of(Flag.SECONDARY), 3, 5),
                entry("S", List.of("P"), List.of(), Set.of(), 1, 1), entry("LOCKED", List.of("OTHER"), List.of(), Set.of(), 1, 0));
        var player = PlayerKnowledge.firstLogin(aspects, bound -> 0);
        var change = book.purchase(player, "P", 0, false).change().orElseThrow();
        assertEquals(15, player.pool(aspects.require("aer"))); assertTrue(player.researchMarkers().isEmpty());
        assertEquals(12, change.knowledge().pool(aspects.require("aer")));
        assertEquals(List.of("P", "S"), change.completions().stream().map(ResearchProgression.Completion::key).toList());
        assertEquals(4, change.knowledge().warp(PlayerKnowledge.WarpKind.PERMANENT));
        assertEquals(2, change.knowledge().warp(PlayerKnowledge.WarpKind.STICKY));
        assertEquals(ResearchBookActions.Rejection.COMPLETED, book.purchase(change.knowledge(), "P", 0, false).rejection().orElseThrow());
    }
    @Test void primaryCannotBeBoughtInNormalModeAndHardModeBlocksSecondaryPurchase() {
        var book = actions(entry("P", List.of(), List.of(), Set.of(), 1, 0), entry("S", List.of(), List.of(), Set.of(Flag.SECONDARY), 1, 0));
        var player = PlayerKnowledge.firstLogin(aspects, bound -> 0);
        assertEquals(ResearchBookActions.Rejection.NEEDS_NOTE, book.purchase(player, "P", 0, false).rejection().orElseThrow());
        assertEquals(ResearchBookActions.Rejection.NEEDS_NOTE, book.purchase(player, "S", 1, false).rejection().orElseThrow());
        assertTrue(book.purchase(player, "P", -1, true).accepted());
    }
    @Test void hiddenAndConcealedVisibilityStillRequiresPurchasePrerequisites() {
        var hidden = entry("H", List.of("P"), List.of(), Set.of(Flag.HIDDEN), 1, 0);
        var concealed = entry("C", List.of("P"), List.of(), Set.of(Flag.CONCEALED), 1, 0);
        var book = actions(hidden, concealed);
        var player = PlayerKnowledge.firstLogin(aspects, bound -> 0);
        assertFalse(book.visible(player, hidden)); assertFalse(book.visible(player, concealed));
        player.rememberResearch("@H"); player.rememberResearch("@C");
        assertTrue(book.visible(player, hidden)); assertTrue(book.visible(player, concealed));
        assertEquals(ResearchBookActions.Rejection.PREREQUISITES, book.purchase(player, "H", -1, false).rejection().orElseThrow());
        player.rememberResearch("P"); assertTrue(book.purchase(player, "H", -1, false).accepted());
    }
    @Test void normalOpeningCompletesSiblingsWithoutPrerequisitesButOnlyOneOrderedPass() {
        var a = entry("A", List.of(), List.of("B"), Set.of(), 1, 0);
        var b = entry("B", List.of("MISSING"), List.of("C"), Set.of(), 1, 1);
        var c = entry("C", List.of("MISSING"), List.of(), Set.of(), 1, 0);
        var player = new PlayerKnowledge(aspects); player.rememberResearch("A");
        var forward = actions(a, b, c).open(player, false, false, false);
        assertTrue(forward.knowledge().researchMarkers().containsAll(List.of("A", "B", "C")));
        var reverse = actions(b, a, c).open(player, false, false, false);
        assertTrue(reverse.knowledge().researchMarkers().contains("B")); assertFalse(reverse.knowledge().researchMarkers().contains("C"));
        assertEquals(Set.of("A"), player.researchMarkers());
    }
    @Test void legacyRevealRequirementShowsNodeWithoutBypassingFullPrerequisites() {
        var first=entry("REVEAL_FIRST",List.of(),List.of(),Set.of(),1,0);
        var second=entry("REVEAL_SECOND",List.of(),List.of(),Set.of(),1,0);
        var advanced=entry("REVEAL_ADVANCED",List.of(first.key(),second.key()),List.of(),Set.of(Flag.CONCEALED),1,0);
        thaumcraft.api.research.ResearchCategories.registerRevealRequirement(advanced.key(),first.key());
        var catalog=catalog(first,second,advanced);var book=new ResearchBookActions(aspects,catalog,e->true);
        var browser=new ResearchBrowser(catalog,e->true);var player=PlayerKnowledge.firstLogin(aspects,bound->0);
        assertFalse(book.visible(player,advanced));
        assertFalse(browser.view(player,"BASICS",0).orElseThrow().nodes().stream().anyMatch(n->n.research().key().equals(advanced.key())));
        player.rememberResearch(first.key());
        assertTrue(book.visible(player,advanced));
        var node=browser.view(player,"BASICS",0).orElseThrow().nodes().stream().filter(n->n.research().key().equals(advanced.key())).findFirst().orElseThrow();
        assertEquals(ResearchBrowser.State.LOCKED,node.state());assertFalse(node.prerequisites());
        assertEquals(ResearchBookActions.Rejection.PREREQUISITES,book.purchase(player,advanced.key(),-1,false).rejection().orElseThrow());
        player.rememberResearch(second.key());assertTrue(book.purchase(player,advanced.key(),-1,false).accepted());
    }
    @Test void cheatBookRequiresConfigurationAndOnlySeedsUndiscoveredAspects() {
        var book = actions(entry("P", List.of("MISSING"), List.of(), Set.of(), 1, 5));
        var player = PlayerKnowledge.firstLogin(aspects, bound -> 0); player.setPool(aspects.require("lux"), (short) 2);
        assertFalse(book.open(player, true, false, false).changed());
        var granted = book.open(player, true, true, false);
        assertTrue(granted.knowledge().researchMarkers().contains("P")); assertEquals(5, granted.knowledge().totalWarp());
        assertEquals(48, granted.knowledge().knownAspects().size());
        assertEquals(15, granted.knowledge().pool(aspects.require("aer"))); assertEquals(2, granted.knowledge().pool(aspects.require("lux")));
        assertEquals(50, granted.knowledge().pool(aspects.require("vitreus")));
        assertFalse(book.open(granted.knowledge(), true, true, false).changed());
        assertEquals(0, book.open(player, true, true, true).knowledge().totalWarp());
    }
    @Test void disabledAndZeroCostsRejectWhileLargeIntegerCostsRequireSufficientPoints() {
        var bad = entry("BAD", List.of(), List.of(), Set.of(), 32768, 0);
        var zero = entry("ZERO", List.of(), List.of(), Set.of(), 0, 0);
        var book = actions(bad, zero); var player = new PlayerKnowledge(aspects);
        assertEquals(ResearchBookActions.Rejection.POINTS, book.purchase(player, "BAD", -1, false).rejection().orElseThrow());
        bad.aspects().forEach((tag, amount) -> player.setPool(aspects.require(tag), amount + 7));
        var bought = book.purchase(player, "BAD", -1, false);
        assertTrue(bought.accepted());
        bad.aspects().forEach((tag, amount) -> assertEquals(7, bought.change().orElseThrow().knowledge().poolAmount(aspects.require(tag))));
        assertEquals(ResearchBookActions.Rejection.INVALID_COST, book.purchase(player, "ZERO", -1, false).rejection().orElseThrow());
        var disabled = new ResearchBookActions(aspects, catalog(bad), e -> false);
        assertEquals(ResearchBookActions.Rejection.UNKNOWN, disabled.purchase(player, "BAD", -1, false).rejection().orElseThrow());
        assertFalse(disabled.open(player, true, false, false).changed());
    }
    @Test void creativeCompletionRecursesParentsWithoutSpendingPointsOrAddingWarp() {
        var first=entry("FIRST",List.of(),List.of(),Set.of(),10,4);
        var middle=entry("MIDDLE",List.of("FIRST"),List.of(),Set.of(),10,4);
        var last=entry("LAST",List.of("MIDDLE"),List.of(),Set.of(),10,4);
        var player=PlayerKnowledge.firstLogin(aspects,bound->0);
        var result=actions(first,middle,last).completeCreative(player,"LAST");
        assertTrue(result.knowledge().researchMarkers().containsAll(List.of("FIRST","MIDDLE","LAST")));
        assertEquals(player.warp(PlayerKnowledge.WarpKind.PERMANENT),result.knowledge().warp(PlayerKnowledge.WarpKind.PERMANENT));
        assertFalse(player.researchMarkers().contains("LAST"));
        assertFalse(actions(first,middle,last).completeCreative(result.knowledge(),"LAST").changed());
    }
}
