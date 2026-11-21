package tc4revival.research;

import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import static org.junit.jupiter.api.Assertions.*;

class ResearchMirrorTest {
    private final AspectCatalog aspects=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private ResearchDefinition entry(String key,String category,int x,String target,List<String> parents,Map<String,String> links) {
        return new ResearchDefinition(key,category,Map.of(),parents,List.of(),List.of(),Set.of(),1,0,
                new ResearchDefinition.Display(x,0,Optional.of("thaumcraft:test"),Optional.empty(),0),
                new ResearchDefinition.Triggers(Optional.empty(),Optional.empty(),Optional.empty()),List.of(),
                ResearchDefinition.NoteSeeds.EMPTY,new ResearchDefinition.Reference(Optional.ofNullable(target),links));
    }
    private ResearchCatalog catalog() {
        return new ResearchCatalog(List.of(
                new ResearchCatalog.Category("BASICS","test","test",List.of(entry("ROOT","BASICS",0,null,List.of(),Map.of()))),
                new ResearchCatalog.Category("ADDON","test","test",List.of(
                        entry("FIRST","ADDON",0,"ROOT",List.of(),Map.of()),entry("SECOND","ADDON",3,"ROOT",List.of(),Map.of()),
                        entry("CHILD","ADDON",6,null,List.of("ROOT"),Map.of("ROOT","SECOND"))))),aspects);
    }
    @Test void multipleMirrorsKeepIndependentLayoutAndExplicitLinkSelection() {
        var catalog=catalog();var knowledge=new PlayerKnowledge(aspects);
        var view=new ResearchBrowser(catalog,e->true).view(knowledge,"ADDON",0).orElseThrow();
        var mirror=view.nodes().get(0);
        assertEquals("FIRST",mirror.research().key());assertEquals("ROOT",mirror.original().key());
        assertEquals("thaumcraft.research.mirror_jump",ResearchBookHint.describe(mirror,0,knowledge,aspects,false,false,false).message());
        assertEquals(1,view.links().size());assertEquals("SECOND",view.links().get(0).target());
        assertEquals(3*24+11,view.links().get(0).targetX());
        knowledge.rememberResearch("ROOT");
        view=new ResearchBrowser(catalog,e->true).view(knowledge,"ADDON",0).orElseThrow();
        assertEquals(ResearchBrowser.State.COMPLETE,view.nodes().get(0).state());
        assertEquals(ResearchBrowser.State.COMPLETE,view.nodes().get(1).state());
        assertTrue(new ResearchRules(catalog).prerequisites(knowledge,"CHILD"));
    }
    @Test void mirrorCannotBePurchasedAndDoesNotCreateAdditionalCompletionOrWarp() {
        var catalog=catalog();var knowledge=new PlayerKnowledge(aspects);
        assertFalse(new ResearchBookActions(aspects,catalog,e->true).purchase(knowledge,"FIRST",-1,false).accepted());
        assertTrue(new ResearchProgression(catalog,e->true).completeTrusted(knowledge,"FIRST",false).isEmpty());
        assertTrue(new ResearchNotes(aspects).generate(catalog.require("FIRST"),bound->{fail("Mirror must not consume RNG");return 0;}).isEmpty());
        assertFalse(knowledge.researchMarkers().contains("FIRST"));
    }
    @Test void mirrorNeedsNoDuplicatePagesButRealResearchStillRequiresPages() {
        var source=com.google.gson.JsonParser.parseString("[{\"key\":\"ROOT\",\"pages\":[]},{\"key\":\"CHILD\",\"pages\":[]}]").getAsJsonArray();
        assertDoesNotThrow(()->BookTestData.pages(source).validate(catalog()));
        assertThrows(IllegalArgumentException.class,()->BookTestData.pages(new com.google.gson.JsonArray()).validate(catalog()));
        source.add(com.google.gson.JsonParser.parseString("{\"key\":\"FIRST\",\"pages\":[{\"type\":\"TEXT\",\"text\":\"duplicate\"}]}"));
        assertThrows(IllegalArgumentException.class,()->BookTestData.pages(source).validate(catalog()));
    }
    @Test void forgedMirrorCompletionCannotUnlockItsSiblings() {
        var original=entry("ROOT","BASICS",0,null,List.of(),Map.of());
        var mirror=entry("MIRROR","BASICS",1,"ROOT",List.of(),Map.of());
        mirror=new ResearchDefinition(mirror.key(),mirror.category(),mirror.aspects(),mirror.parents(),mirror.hiddenParents(),List.of("REWARD"),mirror.flags(),mirror.complexity(),mirror.warp(),mirror.display(),mirror.triggers(),mirror.parentsAny(),mirror.noteSeeds(),mirror.reference());
        var catalog=new ResearchCatalog(List.of(new ResearchCatalog.Category("BASICS","t","t",List.of(original,mirror,entry("REWARD","BASICS",2,null,List.of(),Map.of())))),aspects);
        var knowledge=new PlayerKnowledge(aspects);
        assertTrue(new ResearchProgression(catalog,e->true).completeEligible(knowledge,"MIRROR",false).isEmpty());
        assertTrue(knowledge.researchMarkers().isEmpty());
    }
    @Test void cyclesDanglingReferencesAndWrongParentMappingsAreRejected() {
        for(var entries:List.of(List.of(entry("A","ADDON",0,"B",List.of(),Map.of())),
                List.of(entry("A","ADDON",0,"B",List.of(),Map.of()),entry("B","ADDON",1,"A",List.of(),Map.of())),
                List.of(entry("A","ADDON",0,null,List.of(),Map.of()),entry("B","ADDON",1,null,List.of("A"),Map.of("A","B")))))
            assertThrows(IllegalArgumentException.class,()->new ResearchCatalog(List.of(new ResearchCatalog.Category("ADDON","test","test",entries)),aspects));
    }
}

