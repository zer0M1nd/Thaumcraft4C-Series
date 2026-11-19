package tc4revival.research;

import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import static org.junit.jupiter.api.Assertions.*;

class AspectSynthesisTest {
    private Aspect a(String name,String... parts) { return new Aspect(name,0xffffff,List.of(parts),"f",1); }
    private final AspectCatalog catalog=new AspectCatalog(List.of(a("air"),a("fire"),a("water"),
            a("x","air","fire"),a("y","x","water"),a("z","x","y"),a("double","x","x")));
    private AspectSynthesis.Plan plan(String target,Map<String,Integer> pool,Set<String> bonuses) {
        return AspectSynthesis.plan(catalog.require(target),catalog,a->pool.getOrDefault(a.tag(),0),bonuses);
    }
    private Map<String,Integer> costs(AspectSynthesis.Plan plan) {
        var result=new HashMap<String,Integer>();plan.costs().forEach((a,n)->result.put(a.tag(),n));return result;
    }
    @Test void repeatedBranchesAreAggregatedBeforeTakingExistingIntermediates() {
        var plan=plan("z",Map.of("x",1,"air",1,"fire",1,"water",1),Set.of());
        assertTrue(plan.affordable());assertEquals(Map.of("x",1,"air",1,"fire",1,"water",1),costs(plan));
        assertEquals(Map.of("air",2,"fire",2,"water",1),costs(plan("z",Map.of(),Set.of())));
    }
    @Test void equalComponentsConsumeTwoUnitsAndOneBonusIsOneUnit() {
        var plan=plan("double",Map.of("x",1),Set.of("x"));
        assertTrue(plan.affordable());assertEquals(Map.of("x",2),costs(plan));
        assertEquals(Map.of("x",1,"air",1,"fire",1),costs(plan("double",Map.of(),Set.of("x"))));
    }
    @Test void onlyMissingPrimalAspectsAreReportedAndTargetStockDoesNotReplaceTheCraft() {
        var plan=plan("z",Map.of("z",100,"air",2,"water",1),Set.of());
        assertFalse(plan.affordable());assertEquals(Set.of(catalog.require("fire")),plan.missing());
        assertFalse(plan("air",Map.of(),Set.of()).valid());
    }
    @Test void abundantIntermediatesStopExpansionAndAvoidConsumingPrimalStock() {
        assertEquals(Map.of("x",1,"y",1),costs(plan("z",Map.of("x",5,"y",5,"air",500),Set.of())));
    }
}
