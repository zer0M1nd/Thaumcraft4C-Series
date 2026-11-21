package tc4revival.research;

import com.google.gson.JsonParser;
import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.research.ResearchDefinition.Flag;
import static org.junit.jupiter.api.Assertions.*;

class TutorialResearchTest {
    private final AspectCatalog aspects = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private ResearchDefinition entry(String key, List<String> parents, Set<Flag> flags) {
        return new ResearchDefinition(key,"TUTORIAL",Map.of("aer",5),parents,List.of(),List.of(),flags,1,9,
                new ResearchDefinition.Display(key.equals("FIRST")?0:3,0,Optional.empty(),Optional.empty(),1),
                new ResearchDefinition.Triggers(Optional.empty(),Optional.empty(),Optional.empty()));
    }
    private ResearchCatalog catalog(ResearchDefinition... rows) {
        return new ResearchCatalog(List.of(new ResearchCatalog.Category("TUTORIAL","thaumcraft:icon","background",List.of(rows))),aspects);
    }
    @Test void lockedTutorialStaysVisibleAndDarkButReadingCannotCommitIt() {
        var first=entry("FIRST",List.of(),Set.of());
        var tutorial=entry("LESSON",List.of("FIRST"),Set.of(Flag.TUTORIAL,Flag.CONCEALED,Flag.HIDDEN));
        var catalog=catalog(first,tutorial);
        var player=new PlayerKnowledge(aspects);
        var node=new ResearchBrowser(catalog,e->true).view(player,"TUTORIAL",0).orElseThrow().nodes().stream()
                .filter(n->n.research().key().equals("LESSON")).findFirst().orElseThrow();
        assertEquals(ResearchBrowser.State.LOCKED,node.state());assertTrue(node.iconLocked());assertFalse(node.forcedHidden());
        var actions=new ResearchBookActions(aspects,catalog,e->true);
        assertTrue(actions.visible(player,tutorial));
        assertEquals(ResearchBookActions.Rejection.PREREQUISITES,actions.readTutorial(player,"LESSON").rejection().orElseThrow());
        assertTrue(player.researchMarkers().isEmpty());
    }
    @Test void eligibleReadIsFreeAndWarpFreeOnEveryDifficultyAndCannotRepeat() {
        var tutorial=entry("LESSON",List.of("FIRST"),Set.of(Flag.TUTORIAL));
        var catalog=catalog(entry("FIRST",List.of(),Set.of()),tutorial);
        var player=new PlayerKnowledge(aspects);player.rememberResearch("FIRST");
        var actions=new ResearchBookActions(aspects,catalog,e->true);
        for(int difficulty=-1;difficulty<=1;difficulty++)assertEquals(ResearchBookActions.Route.TUTORIAL,ResearchBookActions.route(tutorial,difficulty));
        var change=actions.readTutorial(player,"LESSON").change().orElseThrow();
        assertEquals(Set.of("FIRST"),player.researchMarkers());
        assertEquals(Set.of("FIRST","LESSON"),change.knowledge().researchMarkers());
        assertEquals(player.knownAspects(),change.knowledge().knownAspects());assertEquals(0,change.knowledge().totalWarp());
        assertEquals(ResearchBookActions.Rejection.COMPLETED,actions.readTutorial(change.knowledge(),"LESSON").rejection().orElseThrow());
        assertTrue(new ResearchNotes(aspects).generate(tutorial,bound->0).isEmpty());
        assertFalse(tutorial.countsForProgress());
    }
    @Test void disabledDefinitionsAndOrdinaryResearchCannotUseTutorialRoute() {
        var tutorial=entry("LESSON",List.of(),Set.of(Flag.TUTORIAL));
        var normal=entry("NORMAL",List.of(),Set.of());var catalog=catalog(tutorial,normal);
        var player=new PlayerKnowledge(aspects);
        assertFalse(new ResearchBookActions(aspects,catalog,e->false).readTutorial(player,"LESSON").accepted());
        assertFalse(new ResearchBookActions(aspects,catalog,e->true).readTutorial(player,"NORMAL").accepted());
        assertTrue(normal.countsForProgress());
    }
    @Test void missingDisabledPrerequisiteDoesNotProduceFalseFlashingState() {
        var catalog=catalog(entry("FIRST",List.of(),Set.of()),entry("LESSON",List.of("FIRST"),Set.of(Flag.TUTORIAL)));
        var node=new ResearchBrowser(catalog,e->!e.key().equals("FIRST")).view(new PlayerKnowledge(aspects),"TUTORIAL",0)
                .orElseThrow().nodes().get(0);
        assertEquals(ResearchBrowser.State.LOCKED,node.state());assertTrue(node.iconLocked());
    }
    @Test void readerShowsRealLockedTutorialPagesAndRetainsThemAcrossKnowledgeSync() {
        var catalog=catalog(entry("FIRST",List.of(),Set.of()),entry("LESSON",List.of("FIRST"),Set.of(Flag.TUTORIAL)));
        var pages=ResearchPages.decode(JsonParser.parseString("""
                [{"key":"LESSON","pages":[{"id":"test:lesson","type":"TEXT","text":{"text":"Actual tutorial"}}]}]
                """).getAsJsonArray());
        var navigation=new ResearchBookNavigation(new ResearchBrowser(catalog,e->true),pages);
        var player=new PlayerKnowledge(aspects);navigation.refresh(player,Map.of(),0);
        assertTrue(navigation.open("LESSON",false));
        assertEquals("test:lesson",navigation.visiblePages().get(0).id());
        navigation.refresh(player,Map.of(),0);assertEquals("LESSON",navigation.reading().orElseThrow());
        assertTrue(player.researchMarkers().isEmpty());assertFalse(navigation.canRead("FIRST"));
    }
    @Test void tutorialAlwaysPrecedesEvenAnAddonWithMinimumRank() {
        var tutorial=new ResearchCatalog.Category("TUTORIAL","thaumcraft:icon","background",List.of());
        var addon=new ResearchCatalog.Category("TEST_EARLY_ADDON","test:icon","background",List.of());
        thaumcraft.api.research.ResearchPresentation.registerCategoryOrder("test:tutorial_first",0,"TEST_EARLY_ADDON",Integer.MIN_VALUE);
        assertEquals(tutorial,thaumcraft.api.research.ResearchPresentation.order(List.of(addon,tutorial)).get(0));
    }
    @Test void absentOptionalHostRemovesOnlyItsTutorialAndMirrorAndPages() {
        var source=JsonParser.parseString("""
                {"categories":[{"research":[
                  {"key":"CORE","pageCount":0},
                  {"key":"LESSON","isTutorial":true,"conditions":[],"suggestedResearch":["CORE","ABSENT"]},
                  {"key":"MIRROR","tutorialMirror":true,"conditions":[]}
                ]}],"pages":[{"key":"CORE","pages":[]},{"key":"LESSON","pages":[]} ]}
                """).getAsJsonObject();
        var removed=TutorialResearch.materialize(source,row->false);
        assertEquals(1,removed.getAsJsonArray("categories").get(0).getAsJsonObject().getAsJsonArray("research").size());
        assertEquals(1,removed.getAsJsonArray("pages").size());
        var included=TutorialResearch.materialize(source,row->true);
        assertEquals("[\"CORE\"]",included.getAsJsonArray("categories").get(0).getAsJsonObject().getAsJsonArray("research")
                .get(1).getAsJsonObject().getAsJsonArray("suggestedResearch").toString());
        assertEquals(3,source.getAsJsonArray("categories").get(0).getAsJsonObject().getAsJsonArray("research").size());
    }

    @Test void disabledTutorialsAreAbsentFromTheSynchronizedBookEvenWithSavedProgress() {
        var source = JsonParser.parseString("""
                {"categories":[
                  {"key":"TUTORIAL","research":[{"key":"WELCOME"},{"key":"UNFLAGGED"}]},
                  {"key":"TUTORIAL","research":[{"key":"ADDON"}]},
                  {"key":"BASICS","icon":"thaumcraft:icon","background":"bg","research":[
                    {"key":"CHAIN","mirrorOf":"MIRROR"},{"key":"MIRROR","mirrorOf":"EXTRA"},
                    {"key":"EXTRA","isTutorial":true,"conditions":[]},{"key":"DISPLAY","tutorialMirror":true},
                    {"key":"CORE","category":"BASICS","displayColumn":0,"displayRow":0,"parents":["EXTRA"],
                     "parentsHidden":["ADDON"],"parentsAny":["WELCOME"],"siblings":["UNFLAGGED"],
                     "parentMirrors":{"EXTRA":"MIRROR"},"suggestedResearch":["CORE","EXTRA"]}
                  ]}],"pages":[{"key":"WELCOME","pages":[]},{"key":"ADDON","pages":[]},
                    {"key":"EXTRA","pages":[{"type":"invalid"}]},
                    {"key":"CORE","pages":[{"id":"example:normal","type":"TEXT","text":{"text":"Normal"}},
                                           {"id":"example:hidden","type":"TEXT_CONCEALED","research":"EXTRA"}]}],
                  "recipes":[]}
                """).getAsJsonObject();
        var json = TutorialResearch.materialize(source, false, row -> fail("Disabled lessons must not query host conditions"));
        var snapshot = ResearchDefinitions.decode(json, aspects);
        assertEquals(List.of("BASICS"), snapshot.catalog().categories().stream().map(ResearchCatalog.Category::key).toList());
        assertEquals(List.of("CORE"), snapshot.catalog().all().stream().map(ResearchDefinition::key).toList());
        var normal = snapshot.catalog().require("CORE");
        assertEquals(List.of("CORE"), normal.suggestedResearch());
        assertTrue(normal.parents().isEmpty() && normal.parentsAny().isEmpty() && normal.hiddenParents().isEmpty());
        assertTrue(normal.siblings().isEmpty() && normal.reference().parentMirrors().isEmpty());
        assertEquals(Set.of("CORE"), snapshot.pages().entries().keySet());
        assertEquals(List.of("example:normal"), snapshot.pages().require("CORE").stream().map(ResearchPage::id).toList());
        var player = new PlayerKnowledge(aspects);
        player.rememberResearch("EXTRA");
        var navigation = new ResearchBookNavigation(new ResearchBrowser(snapshot.catalog(), ignored -> true), snapshot.pages());
        navigation.refresh(player, Map.of(), 0);
        assertEquals("BASICS", navigation.category());
        assertFalse(navigation.openSearch("EXTRA"));
        assertFalse(navigation.open("EXTRA", false));
        assertTrue(player.researchMarkers().contains("EXTRA"));
        assertEquals(3, source.getAsJsonArray("categories").size());
    }
}
