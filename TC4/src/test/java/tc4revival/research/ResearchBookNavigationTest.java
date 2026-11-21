package tc4revival.research;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import static org.junit.jupiter.api.Assertions.*;

class ResearchBookNavigationTest {
    @Test void maximumViewportRestoresWithoutAccumulatingPanOnReopen() {
        var first=navigation();first.viewport(544,310);first.refresh(complete(),Map.of(),0);
        first.selectCategory("ARTIFICE");first.drag(60,-40);var saved=first.location();
        for(int i=0;i<4;i++) {
            var reopened=navigation();reopened.viewport(544,310);reopened.refresh(complete(),Map.of(),0);
            assertTrue(reopened.restoreLocation(saved));
            reopened.viewport(544,310); // Screen initialization must be idempotent.
            assertEquals(saved,reopened.location());saved=reopened.location();
        }
    }
    @Test void restoresCategoryAndPanButNeverReopensResearch() {
        var nav=navigation();nav.refresh(complete(),Map.of(),0);nav.selectCategory("ARTIFICE");nav.drag(60,-40);var location=nav.location();
        var reopened=navigation();reopened.refresh(complete(),Map.of(),0);reopened.open("THAUMIUM",false);assertTrue(reopened.restoreLocation(location));assertEquals(location,reopened.location());assertTrue(reopened.reading().isEmpty());
        assertFalse(reopened.restoreLocation(new ResearchBookNavigation.Location("REMOVED_CATEGORY",0,0)));assertFalse(reopened.restoreLocation(new ResearchBookNavigation.Location("BASICS",Double.NaN,0)));
    }
    private final AspectCatalog aspects = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private final ResearchCatalog catalog = ResearchCatalog.decode(read("research-catalog.json"), aspects);
    private final ResearchPages pages = BookTestData.pages(read("research-pages.json"));
    private static com.google.gson.JsonArray read(String name) {
        try (var reader = new java.io.InputStreamReader(java.util.Objects.requireNonNull(
                ResearchBookNavigationTest.class.getResourceAsStream("/" + name)), java.nio.charset.StandardCharsets.UTF_8)) {
            return com.google.gson.JsonParser.parseReader(reader).getAsJsonArray();
        } catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
    }
    private ResearchBookNavigation navigation() { return new ResearchBookNavigation(new ResearchBrowser(catalog, entry -> true), pages); }
    private PlayerKnowledge complete() {
        var knowledge = new PlayerKnowledge(aspects); catalog.all().forEach(entry -> knowledge.rememberResearch(entry.key())); return knowledge;
    }
    @Test void eldritchRecipeAndSearchCannotBypassLockedCategoryEvenInCreative() {
        var knowledge=new PlayerKnowledge(aspects);
        var nav=new ResearchBookNavigation(new ResearchBrowser(catalog,e->true,()->true),pages);
        nav.refresh(knowledge,Map.of(),0);
        var key=catalog.all().stream().filter(e->e.category().equals("ELDRITCH")&&!e.key().equals("ELDRITCHMINOR")).findFirst().orElseThrow().key();
        assertFalse(nav.focusExternal(key));assertFalse(nav.focus(key));assertFalse(nav.openSearch(key));
        knowledge.rememberResearch("ELDRITCHMINOR");nav.refresh(knowledge,Map.of(),0);
        assertTrue(nav.focusExternal(key));assertFalse(nav.open(key,false));
    }
    @Test void completedEldritchResearchStillRequiresCategoryForReaderLinks() {
        var knowledge=new PlayerKnowledge(aspects);var nav=navigation();
        var key=catalog.all().stream().filter(e->e.category().equals("ELDRITCH")&&!e.key().equals("ELDRITCHMINOR")&&pages.entries().containsKey(e.key())).findFirst().orElseThrow().key();
        knowledge.rememberResearch(key);nav.refresh(knowledge,Map.of(),0);
        assertFalse(nav.open(key,true));assertFalse(nav.follow(new ResearchRecipeLinks.Target(key,0)));
        knowledge.rememberResearch("ELDRITCHMINOR");nav.refresh(knowledge,Map.of(),0);assertTrue(nav.open(key,true));
    }
    @Test void externalRecipeLocatesHiddenResearchWithoutUnlockingIt() {
        var knowledge=new PlayerKnowledge(aspects);var navigation=navigation();navigation.refresh(knowledge,Map.of(),0);
        var hidden=catalog.all().stream().filter(e->e.has(ResearchDefinition.Flag.HIDDEN)||e.has(ResearchDefinition.Flag.LOST)).findFirst().orElseThrow();
        assertTrue(navigation.focusExternal(hidden.key()));assertEquals(hidden.category(),navigation.category());
        assertTrue(navigation.view().orElseThrow().nodes().stream().anyMatch(n->n.research().key().equals(hidden.key())));
        assertFalse(navigation.open(hidden.key(),false));assertTrue(knowledge.researchMarkers().isEmpty());
        knowledge.rememberResearch(hidden.key());navigation.refresh(knowledge,Map.of(),0);assertTrue(navigation.open(hidden.key(),false));
    }
    @Test void searchFocusSelectsCategoryAndClampsCenterWithoutGrantingResearch() {
        var navigation=navigation();var knowledge=complete();navigation.refresh(knowledge,Map.of(),0);
        for(var node:navigation.searchable()) {
            assertTrue(navigation.focus(node.research().key()));assertEquals(node.research().category(),navigation.category());
            var view=navigation.view().orElseThrow();assertEquals(view.bounds().clampX(node.x()+11-112),navigation.panX());assertEquals(view.bounds().clampY(node.y()+11-98),navigation.panY());
            assertTrue(navigation.open(node.research().key(),false));
        }
        var fresh=new PlayerKnowledge(aspects);navigation.refresh(fresh,Map.of(),0);assertFalse(navigation.open("THAUMIUM",false));assertTrue(fresh.researchMarkers().isEmpty());
    }
    @Test void everyObservedResearchClampsEvenSpreadsAndKeepsOddLastPagesReadable() {
        var navigation = navigation(); navigation.refresh(complete(), Map.of(), 0);
        for (var entry : catalog.all()) {
            assertTrue(navigation.open(entry.key(), false));
            int count = navigation.visiblePages().size(); int last = Math.max(0, count - 1) / 2 * 2;
            int turns = 0;
            while (navigation.turn(1)) { assertEquals(0, navigation.spread() % 2); assertTrue(++turns <= count); }
            assertEquals(last, navigation.spread(), entry.key());
            while (navigation.turn(-1)) assertTrue(navigation.spread() >= 0);
            assertEquals(0, navigation.spread());
        }
    }
    @Test void searchOpensLockedPreviewWithoutExposingRecipesOrGrantingResearch() {
        var navigation = navigation(); var knowledge = new PlayerKnowledge(aspects);
        navigation.refresh(knowledge, Map.of(), 0);
        var key = navigation.searchable().stream().filter(n -> !knowledge.researchMarkers().contains(n.research().key()))
                .findFirst().orElseThrow().research().key();
        assertTrue(navigation.focus(key)); assertTrue(navigation.openSearch(key));
        navigation.refresh(knowledge, Map.of(), 0);
        assertEquals(key, navigation.reading().orElseThrow()); assertFalse(navigation.canRead(key));
        assertEquals(1, navigation.visiblePages().size());
        assertEquals("thaumcraft.research.locked_preview", navigation.visiblePages().get(0).text().orElseThrow());
        assertFalse(navigation.turn(1)); assertFalse(navigation.openSearch("MISSING"));
        assertTrue(knowledge.researchMarkers().isEmpty());
        knowledge.rememberResearch(key); navigation.refresh(knowledge, Map.of(), 0);
        assertTrue(navigation.canRead(key));
        assertNotEquals("thaumcraft.research.locked_preview", navigation.visiblePages().get(0).text().orElse(""));
    }
    @Test void hiddenCategoryAndReadingPermissionsFollowSnapshotsWithoutMutatingCaller() {
        var knowledge = new PlayerKnowledge(aspects); var navigation = navigation();
        navigation.refresh(knowledge, Map.of(), 0);
        assertFalse(navigation.selectCategory("ELDRITCH")); assertFalse(navigation.open("THAUMIUM", false));
        knowledge.rememberResearch("THAUMIUM"); assertFalse(navigation.open("THAUMIUM", false));
        navigation.refresh(knowledge, Map.of(), 0); assertTrue(navigation.open("THAUMIUM", false));
        knowledge.forgetResearch("THAUMIUM"); navigation.refresh(knowledge, Map.of(), 0);
        assertTrue(navigation.reading().isEmpty()); assertEquals(0, navigation.spread());
        assertTrue(knowledge.researchMarkers().isEmpty());
    }
    @Test void aspectPageChangesClampPositionAndLinksRestorePreviousSpread() {
        var navigation = navigation(); var knowledge = complete(); var discovered = new LinkedHashMap<String, Integer>();
        Arrays.stream(LegacyAspect.values()).forEach(aspect -> discovered.put(aspect.create().tag(), 0));
        navigation.refresh(knowledge, discovered, 0); assertTrue(navigation.open("ASPECTS", false));
        while (navigation.turn(1)) {}
        int previous = navigation.spread(); assertTrue(previous > 0);
        assertTrue(navigation.open("THAUMIUM", true)); assertEquals(0, navigation.spread());
        assertTrue(navigation.backLink()); assertEquals("ASPECTS", navigation.reading().orElseThrow());
        assertEquals(previous, navigation.spread());
        navigation.refresh(knowledge, Map.of(), 0);
        assertTrue(navigation.spread() <= Math.max(0, navigation.visiblePages().size() - 1) / 2 * 2);
        navigation.showBrowser(); assertFalse(navigation.backLink());
    }
    @Test void noteHintsRequireBothSuppliesAndRejectAnExistingNote() {
        var entry = catalog.all().stream().filter(e -> !e.aspects().isEmpty()).findFirst().orElseThrow();
        var node = new ResearchBrowser.Node(entry, 0, 0, ResearchBrowser.State.AVAILABLE, true, ResearchBrowser.Frame.NORMAL);
        var knowledge = new PlayerKnowledge(aspects);
        var ready = ResearchBookHint.describe(node, 1, knowledge, aspects, true, true, false);
        assertTrue(ready.requestable()); assertEquals("tc.research.getprim", ready.message());
        for (boolean paper : new boolean[]{false, true}) for (boolean ink : new boolean[]{false, true}) {
            assertEquals(paper && ink, ResearchBookHint.describe(node, 1, knowledge, aspects, paper, ink, false).requestable());
            var existing = ResearchBookHint.describe(node, 1, knowledge, aspects, paper, ink, true);
            assertFalse(existing.requestable()); assertEquals("tc.research.hasnote", existing.message());
        }
    }
    @Test void purchaseHintsUseDiscoveredPoolsAndDoNotSpendThem() {
        var entry = catalog.all().stream().filter(e -> !e.aspects().isEmpty()).findFirst().orElseThrow();
        var node = new ResearchBrowser.Node(entry, 0, 0, ResearchBrowser.State.AVAILABLE, true, ResearchBrowser.Frame.NORMAL);
        var knowledge = new PlayerKnowledge(aspects);
        assertFalse(ResearchBookHint.describe(node, -1, knowledge, aspects, false, false, true).requestable());
        entry.aspects().forEach((tag, amount) -> knowledge.setPool(aspects.require(tag), amount.shortValue()));
        var ready = ResearchBookHint.describe(node, -1, knowledge, aspects, false, false, true);
        assertTrue(ready.requestable()); assertEquals("tc.research.purchase", ready.message());
        entry.aspects().forEach((tag, amount) -> assertEquals(amount.intValue(), knowledge.pool(aspects.require(tag))));
        var locked = new ResearchBrowser.Node(entry, 0, 0, ResearchBrowser.State.LOCKED, false, ResearchBrowser.Frame.NORMAL);
        assertEquals("tc.researchmissing", ResearchBookHint.describe(locked, -1, knowledge, aspects, true, true, false).message());
        assertFalse(ResearchBookHint.describe(locked, -1, knowledge, aspects, true, true, false).requestable());
    }
    @Test void recipeLinksLocateSourceSpreadAndRestoreThePreviousVisit() {
        var navigation = navigation(); navigation.refresh(complete(), Map.of(), 0);
        assertTrue(navigation.open("ASPECTS", false));
        int before = navigation.spread();
        var entry = pages.entries().entrySet().stream().filter(e -> e.getValue().size() >= 4).findFirst().orElseThrow();
        assertTrue(navigation.follow(new ResearchRecipeLinks.Target(entry.getKey(), 3)));
        assertEquals(entry.getKey(), navigation.reading().orElseThrow()); assertEquals(2, navigation.spread());
        assertFalse(navigation.follow(new ResearchRecipeLinks.Target(entry.getKey(), -1)));
        assertFalse(navigation.follow(new ResearchRecipeLinks.Target(entry.getKey(), entry.getValue().size())));
        assertFalse(navigation.follow(new ResearchRecipeLinks.Target("MISSING", 0)));
        assertTrue(navigation.backLink()); assertEquals("ASPECTS", navigation.reading().orElseThrow());
        assertEquals(before, navigation.spread()); assertFalse(navigation.hasHistory());
    }
    @Test void panningAndCategoryChangesStayWithinCurrentTreeAndRejectInvalidValues() {
        var navigation = navigation(); navigation.refresh(complete(), Map.of(), 0);
        for (var category : navigation.categories()) {
            assertTrue(navigation.selectCategory(category.key())); navigation.drag(Double.MAX_VALUE, -Double.MAX_VALUE);
            var bounds = navigation.view().orElseThrow().bounds();
            assertEquals(bounds.clampX(navigation.panX()), navigation.panX());
            assertEquals(bounds.clampY(navigation.panY()), navigation.panY());
            double x = navigation.panX(), y = navigation.panY(); navigation.drag(Double.NaN, 1);
            assertEquals(x, navigation.panX()); assertEquals(y, navigation.panY());
        }
        var category = navigation.category(); assertFalse(navigation.selectCategory("MISSING")); assertEquals(category, navigation.category());
    }
}
