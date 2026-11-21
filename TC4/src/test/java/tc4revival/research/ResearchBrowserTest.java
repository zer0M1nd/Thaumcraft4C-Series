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

class ResearchBrowserTest {
    @Test void completedResearchStaysVisibleAndBrightAfterPrerequisitesAreAdded() {
        var player = new PlayerKnowledge(aspects);
        player.rememberResearch("C");
        var parent = entry("P", List.of(), List.of());
        var hiddenParent = entry("HP", List.of(), List.of());
        for (var flags : List.of(new Flag[0], new Flag[]{Flag.HIDDEN}, new Flag[]{Flag.LOST}, new Flag[]{Flag.CONCEALED})) {
            var child = entry("C", "BASICS", 1, 0, List.of("P"), List.of("HP"), List.of(), flags);
            var model = browser(parent, hiddenParent, child);
            var completed = view(model, player).nodes().stream().filter(n -> n.research().key().equals("C")).findFirst().orElseThrow();
            assertEquals(ResearchBrowser.State.COMPLETE, completed.state());
            assertTrue(completed.prerequisites(), "Completed research no longer rechecks added prerequisites");
            assertFalse(completed.iconLocked(), "Completed icons must not be darkened");
            assertFalse(completed.forcedHidden());
            assertTrue(view(model, player).links().stream().filter(l -> l.source().equals("C"))
                    .allMatch(l -> l.state() == ResearchBrowser.LinkState.COMPLETE));
            assertTrue(new ResearchRules(catalog(parent, hiddenParent, child)).prerequisites(player, "C"));
            assertEquals(Set.of("C"), player.researchMarkers(), "Rendering must not grant missing prerequisites");
        }
    }
    @Test void incompleteResearchWithMissingPrerequisitesKeepsItsLockedIcon() {
        var parent = entry("P", List.of(), List.of());
        var child = entry("C", List.of("P"), List.of());
        var player = new PlayerKnowledge(aspects);
        var model = browser(parent, child);
        var locked = view(model, player).nodes().get(1);
        assertEquals(ResearchBrowser.State.LOCKED, locked.state());
        assertTrue(locked.iconLocked());
        player.rememberResearch("P");
        var available = view(model, player).nodes().get(1);
        assertEquals(ResearchBrowser.State.AVAILABLE, available.state());
        assertFalse(available.iconLocked());
    }
    @Test void creativeRevealsHiddenButNeverDisabledWithoutGrantingKnowledge() {
        var hidden = entry("H", List.of(), List.of(), Flag.HIDDEN);
        var disabled = entry("D", List.of(), List.of());
        var catalog = catalog(hidden, disabled);
        var player = new PlayerKnowledge(aspects);
        var survival = new ResearchBrowser(catalog, e -> !e.key().equals("D"));
        assertTrue(view(survival, player).nodes().isEmpty());
        var creative = new ResearchBrowser(catalog, e -> !e.key().equals("D"), () -> true);
        assertEquals(List.of("H"), view(creative, player).nodes().stream().map(n -> n.research().key()).toList());
        assertFalse(creative.canLocate(player,"D"));
        for (var node : view(creative, player).nodes()) {
            assertTrue(node.forcedHidden()); assertEquals(ResearchBrowser.State.LOCKED, node.state());
        }
        assertTrue(player.researchMarkers().isEmpty());
        var change = new ResearchBookActions(aspects, catalog, e -> false).completeCreative(player, "H");
        var completed = view(creative, change.knowledge()).nodes().stream().filter(n -> n.research().key().equals("H")).findFirst().orElseThrow();
        assertEquals(ResearchBrowser.State.COMPLETE, completed.state()); assertFalse(completed.forcedHidden());
    }
    @Test void baseCategoriesAlwaysPrecedeAddonsIncludingCreativeEldritch() {
        var catalog = new ResearchCatalog(List.of(new ResearchCatalog.Category("addon:one", "i", "b", List.of()),
                new ResearchCatalog.Category("ELDRITCH", "i", "b", List.of()),
                new ResearchCatalog.Category("BASICS", "i", "b", List.of()),
                new ResearchCatalog.Category("addon:two", "i", "b", List.of())), aspects);
        assertEquals(List.of("BASICS", "ELDRITCH", "addon:one", "addon:two"),
                new ResearchBrowser(catalog, e -> true, () -> true).categories(new PlayerKnowledge(aspects)).stream().map(ResearchCatalog.Category::key).toList());
    }
    private final AspectCatalog aspects = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private ResearchDefinition entry(String key, String category, int x, int y, List<String> parents,
                                     List<String> hidden, List<String> siblings, Flag... flags) {
        return new ResearchDefinition(key, category, Map.of(), parents, hidden, siblings, Set.of(flags), 0, 0,
                new ResearchDefinition.Display(x, y, Optional.empty(), Optional.empty(), 1),
                new ResearchDefinition.Triggers(Optional.empty(), Optional.empty(), Optional.empty()));
    }
    private ResearchDefinition entry(String key, List<String> parents, List<String> siblings, Flag... flags) {
        return entry(key, "BASICS", 0, 0, parents, List.of(), siblings, flags);
    }
    private ResearchCatalog catalog(ResearchDefinition... entries) {
        return new ResearchCatalog(List.of(new ResearchCatalog.Category("BASICS", "icon", "background", List.of(entries))), aspects);
    }
    private ResearchBrowser browser(ResearchDefinition... entries) { return new ResearchBrowser(catalog(entries), e -> true); }
    private ResearchBrowser.View view(ResearchBrowser browser, PlayerKnowledge player) { return browser.view(player, "BASICS", 0).orElseThrow(); }
    @Test void cluesRevealNodesWithoutGrantingPrerequisitesAndLostLinksWaitForCompletion() {
        var parent = entry("P", List.of(), List.of());
        var lost = entry("L", List.of("P"), List.of(), Flag.LOST);
        var hidden = entry("H", List.of("P"), List.of(), Flag.HIDDEN);
        var concealed = entry("C", List.of("P"), List.of(), Flag.CONCEALED);
        var model = browser(parent, lost, hidden, concealed); var player = new PlayerKnowledge(aspects);
        assertEquals(List.of("P"), view(model, player).nodes().stream().map(n -> n.research().key()).toList());
        for (String key : List.of("L", "H", "C")) player.rememberResearch("@" + key);
        var clues = view(model, player);
        assertEquals(4, clues.nodes().size());
        assertTrue(clues.nodes().stream().skip(1).allMatch(n -> n.state() == ResearchBrowser.State.LOCKED));
        assertEquals(List.of("H"), clues.links().stream().map(ResearchBrowser.Link::source).toList());
        player.rememberResearch("P");
        assertTrue(view(model, player).nodes().stream().allMatch(ResearchBrowser.Node::prerequisites));
        assertFalse(view(model, player).links().stream().anyMatch(l -> l.source().equals("L")));
        player.rememberResearch("L");
        assertEquals(ResearchBrowser.LinkState.COMPLETE, view(model, player).links().stream()
                .filter(l -> l.source().equals("L")).findFirst().orElseThrow().state());
    }
    @Test void hiddenParentsLockNodesWithoutDrawingLinesAndAbsentParentsAreDisplayOnly() {
        var parent = entry("P", List.of(), List.of());
        var child = entry("C", "BASICS", 2, -3, List.of("ABSENT"), List.of("P"), List.of());
        var model = browser(parent, child); var player = new PlayerKnowledge(aspects);
        var node = view(model, player).nodes().get(1);
        assertEquals(ResearchBrowser.State.LOCKED, node.state()); assertEquals(48, node.x()); assertEquals(-72, node.y());
        assertTrue(view(model, player).links().isEmpty());
        player.rememberResearch("P");
        assertEquals(ResearchBrowser.State.AVAILABLE, view(model, player).nodes().get(1).state());
        assertFalse(new ResearchRules(catalog(parent, child)).prerequisites(player, "C"), "Display must not replace server checks");
    }
    @Test void framesPrioritizeRoundAndUseDifficultyEvenWithoutCosts() {
        var round = entry("R", List.of(), List.of(), Flag.ROUND, Flag.HIDDEN);
        var hidden = entry("H", List.of(), List.of(), Flag.HIDDEN, Flag.SECONDARY);
        var empty = entry("E", List.of(), List.of());
        var model = browser(round, hidden, empty); var player = new PlayerKnowledge(aspects);
        player.rememberResearch("@R"); player.rememberResearch("@H");
        assertEquals(List.of(ResearchBrowser.Frame.ROUND, ResearchBrowser.Frame.HIDDEN_PURCHASE, ResearchBrowser.Frame.NORMAL),
                view(model, player).nodes().stream().map(ResearchBrowser.Node::frame).toList());
        assertEquals(ResearchBrowser.Frame.HIDDEN, model.view(player, "BASICS", 1).orElseThrow().nodes().get(1).frame());
        assertEquals(ResearchBrowser.Frame.PURCHASE, model.view(player, "BASICS", -1).orElseThrow().nodes().get(2).frame());
        assertEquals(ResearchBookActions.Route.NOTE, ResearchBookActions.route(empty, -1));
        assertThrows(IllegalArgumentException.class, () -> model.view(player, "BASICS", 2));
    }
    @Test void siblingParentDuplicatesAreSuppressedAndVirtualTargetsAreNeverLinked() {
        var a = entry("A", List.of("V"), List.of("B", "C", "V"));
        var b = entry("B", List.of("A"), List.of());
        var c = entry("C", List.of(), List.of());
        var virtual = entry("V", List.of(), List.of(), Flag.VIRTUAL);
        var model = browser(a, b, c, virtual); var player = new PlayerKnowledge(aspects);
        var before = view(model, player);
        assertEquals(3, before.nodes().size()); assertEquals(2, before.links().size());
        assertTrue(before.links().stream().anyMatch(l -> l.source().equals("A") && l.target().equals("C") && l.kind() == ResearchBrowser.LinkKind.SIBLING));
        player.rememberResearch("A");
        assertEquals(ResearchBrowser.LinkState.COMPLETE, view(model, player).links().get(0).state());
        assertEquals(ResearchBrowser.LinkState.TARGET_COMPLETE, view(model, player).links().get(1).state());
    }
    @Test void categoriesGateEldritchAndFilterDisabledEntriesAndCrossCategoryLinks() {
        var a = entry("A", List.of("E"), List.of()); var gate = entry("ELDRITCHMINOR", List.of(), List.of());
        var eldritch = entry("E", "ELDRITCH", 1, 2, List.of(), List.of(), List.of());
        var catalog = new ResearchCatalog(List.of(new ResearchCatalog.Category("BASICS", "i", "b", List.of(a, gate)),
                new ResearchCatalog.Category("ELDRITCH", "i", "b", List.of(eldritch))), aspects);
        var model = new ResearchBrowser(catalog, e -> !e.key().equals("A")); var player = new PlayerKnowledge(aspects);
        assertEquals(1, model.categories(player).size()); assertTrue(model.view(player, "ELDRITCH", 0).isEmpty());
        assertEquals(1, view(model, player).nodes().size());
        player.rememberResearch("ELDRITCHMINOR"); assertEquals(2, model.categories(player).size());
        assertEquals(1, model.view(player, "ELDRITCH", 0).orElseThrow().nodes().size());
        assertTrue(view(new ResearchBrowser(catalog, e -> true), player).links().isEmpty());
    }
    @Test void boundsIncludeUnrevealedPositionsAndHitTestingRespectsViewportAndPan() {
        var extreme = entry("H", "BASICS", -10, 10, List.of(), List.of(), List.of(), Flag.HIDDEN);
        var other = entry("O", "BASICS", 10, -10, List.of(), List.of(), List.of());
        var model = browser(entry("A", List.of(), List.of()), extreme, other);
        var view = view(model, new PlayerKnowledge(aspects));
        assertEquals(new ResearchBrowser.Bounds(-325, -352, 128, 179), view.bounds());
        assertEquals("A", view.hit(22, 22, 0, 0).orElseThrow().research().key());
        assertTrue(view.hit(23, 22, 0, 0).isEmpty()); assertTrue(view.hit(-1, 0, 0, 0).isEmpty());
        assertEquals("A", view.hit(0, 0, 10, 10).orElseThrow().research().key());
        assertTrue(view.hit(224, 0, -224, 0).isEmpty());
        assertEquals(127, view.bounds().clampX(999)); assertEquals(-352, view.bounds().clampY(-999));
    }
}
