package tc4revival.research;

import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import static org.junit.jupiter.api.Assertions.*;

class CompletedResearchPrerequisitesTest {
    private final AspectCatalog aspects=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private ResearchDefinition row(String key,List<String> parents,List<String> hidden,String original) {
        return new ResearchDefinition(key,"BASICS",Map.of(),parents,hidden,List.of(),Set.of(),1,0,
                new ResearchDefinition.Display(0,0,Optional.empty(),Optional.empty(),0),
                new ResearchDefinition.Triggers(Optional.empty(),Optional.empty(),Optional.empty()),List.of(),
                ResearchDefinition.NoteSeeds.EMPTY,new ResearchDefinition.Reference(Optional.ofNullable(original),Map.of()));
    }
    @Test void completionSkipsNewParentsAndChildrenCheckOnlyTheirImmediateParent() {
        var catalog=new ResearchCatalog(List.of(new ResearchCatalog.Category("BASICS","test","test",List.of(
                row("A",List.of(),List.of(),null),row("B",List.of("A"),List.of("EXTRA"),null),
                row("EXTRA",List.of(),List.of(),null),row("C",List.of("B"),List.of(),null),
                row("MIRROR",List.of(),List.of(),"B"),row("D",List.of("MIRROR"),List.of(),null)))),aspects);
        var knowledge=new PlayerKnowledge(aspects);var rules=new ResearchRules(catalog);
        assertFalse(rules.prerequisites(knowledge,"B"));assertFalse(rules.prerequisites(knowledge,"C"));
        knowledge.rememberResearch("B");
        for(String key:List.of("B","C","MIRROR","D"))assertTrue(rules.prerequisites(knowledge,key),key);
        assertFalse(new ResearchRules(catalog,e->!e.key().equals("B")).prerequisites(knowledge,"B"));
        assertEquals(Set.of("B"),knowledge.researchMarkers());
        assertFalse(rules.prerequisites(knowledge,"UNKNOWN"));
        knowledge.rememberResearch("@UNKNOWN");
        assertFalse(rules.prerequisites(knowledge,"@UNKNOWN"));
        assertEquals("C",new ResearchProgression(catalog,e->true).completeEligible(knowledge,"C",true).get(0).key());
        assertFalse(knowledge.researchMarkers().contains("A"));
    }
}
