package tc4revival.research;

import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectDefinitions;
import tc4revival.research.sync.KnowledgeSnapshot;
import static org.junit.jupiter.api.Assertions.*;

class ResearchGuidanceTest {
    private final tc4revival.aspect.AspectCatalog aspects=AspectDefinitions.bundled();
    private ResearchDefinition target(boolean secondary) {
        return new ResearchDefinition("TARGET","BASICS",Map.of("aer",6,"ignis",5,"aqua",4,"terra",3),
                List.of("PARENT"),List.of(),List.of(),secondary?Set.of(ResearchDefinition.Flag.SECONDARY):Set.of(),1,0,
                new ResearchDefinition.Display(0,0,Optional.empty(),Optional.empty(),0),
                new ResearchDefinition.Triggers(Optional.empty(),Optional.empty(),Optional.of(List.of("sensus"))),
                List.of(),new ResearchDefinition.NoteSeeds(List.of("aer","ordo"),List.of("perditio","ordo")));
    }
    private ResearchCatalog catalog(ResearchDefinition target) {
        return new ResearchCatalog(List.of(new ResearchCatalog.Category("BASICS","icon","background",List.of(target))),aspects);
    }
    private PlayerKnowledge player() {
        var p=new PlayerKnowledge(aspects);p.rememberResearch(ResearchGuidance.RESEARCH);
        for(var a:aspects.all())p.setPool(a,10);return p;
    }
    @Test void ordinaryCostIncludesAllInitialKindsAndSecondaryIsLimitedToThree() {
        assertEquals(Set.of("aer","ignis","aqua","terra","ordo","perditio"),new HashSet<>(ResearchGuidance.cost(target(false))));
        assertEquals(List.of("aer","ignis","aqua"),ResearchGuidance.cost(target(true)));
    }
    @Test void repeatedListeningIsRejectedUntilChosenPrerequisiteIsComplete() {
        var t=target(true);var catalog=catalog(t);var p=player();var rows=ResearchRequirements.collect(t);
        var bought=ResearchGuidance.listen(t,p,catalog,aspects,rows,r->false,n->0).change().orElseThrow().knowledge();
        assertEquals(9,bought.poolAmount(aspects.require("aer")));assertEquals(10,p.poolAmount(aspects.require("aer")));
        assertFalse(ResearchGuidance.listen(t,bought,catalog,aspects,rows,r->false).accepted());
        // A completed current marker can be replaced by another condition, including a scan.
        var again=ResearchGuidance.listen(t,bought,catalog,aspects,rows,r->r.kind()==ResearchRequirements.Kind.RESEARCH,n->0).change().orElseThrow().knowledge();
        assertEquals(8,again.poolAmount(aspects.require("aer")));
        assertEquals(ResearchRequirements.id(rows.get(1)),again.guidance().get(t.key()));
    }
    @Test void unaffordableRequestDoesNotSpendAnythingAndUndiscoveredMechanismIsRejected() {
        var t=target(false);var p=player();p.setPool(aspects.require("ordo"),0);
        var before=KnowledgeNbt.write(p);var rows=ResearchRequirements.collect(t);
        assertFalse(ResearchGuidance.listen(t,p,catalog(t),aspects,rows,r->false).accepted());
        assertEquals(before,KnowledgeNbt.write(p));
        p.forgetResearch(ResearchGuidance.RESEARCH);assertFalse(ResearchGuidance.eligible(t,p,catalog(t)));
    }
    @Test void guidanceSurvivesSaveCopyAndSyncAndHidesOtherIncompleteConditions() {
        var t=target(true);var p=player();var rows=ResearchRequirements.collect(t);
        p.rememberGuidance(t.key(),ResearchRequirements.id(rows.get(1)));
        assertEquals(p.guidance(),p.copy().guidance());
        assertEquals(p.guidance(),KnowledgeNbt.read(KnowledgeNbt.write(p),aspects).knowledge().guidance());
        assertEquals(p.guidance(),KnowledgeSnapshot.capture(p).toKnowledge(aspects).guidance());
        assertFalse(ResearchGuidance.revealed(t.key(),rows.get(0),false,p));
        assertTrue(ResearchGuidance.revealed(t.key(),rows.get(0),true,p));
        assertTrue(ResearchGuidance.revealed(t.key(),rows.get(1),false,p));
        p.wipe();assertTrue(p.guidance().isEmpty());
    }
}
