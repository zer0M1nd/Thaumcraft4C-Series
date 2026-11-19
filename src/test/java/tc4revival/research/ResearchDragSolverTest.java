package tc4revival.research;

import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import static org.junit.jupiter.api.Assertions.*;

class ResearchDragSolverTest {
    @Test void equallyShortBridgesPreferTheCentreButStillTryAnOuterFeasibleBridge() {
        var a=new ResearchHex(-2,0);var b=new ResearchHex(-1,1);
        var inner=new ResearchHex(-1,0);var outer=new ResearchHex(-2,1);
        var grid=new LinkedHashMap<ResearchHex,ResearchNote.Cell>();
        grid.put(a,anchor("terra"));grid.put(b,anchor("ordo"));
        grid.put(outer,ResearchNote.Cell.blank());grid.put(inner,ResearchNote.Cell.blank());
        assertEquals(List.of(a,inner,b),new ResearchDragSolver(note(grid),aspects,a,stock(10),Set.of()).preview(b).orElseThrow().path());
        grid.put(inner,anchor("aer"));
        assertEquals(List.of(a,outer,b),new ResearchDragSolver(note(grid),aspects,a,stock(10),Set.of()).preview(b).orElseThrow().path());
    }
    @Test void centralPreferenceMinimizesTotalCostNotJustTheFirstStep() {
        var grid=new LinkedHashMap<ResearchHex,ResearchNote.Cell>();grid.put(new ResearchHex(0,0),ResearchNote.Cell.blank());
        for(int r=1;r<=3;r++)for(var h:ResearchHex.ring(r))grid.put(h,ResearchNote.Cell.blank());
        var a=new ResearchHex(-3,0);var b=new ResearchHex(0,3);grid.put(a,anchor("terra"));grid.put(b,anchor("terra"));
        var plan=new ResearchDragSolver(note(grid),aspects,a,Map.of("terra",100,"vitreus",100),Set.of()).preview(b).orElseThrow();
        assertEquals(a.distance(b)+1,plan.path().size());
        assertEquals(minimumCenterCost(a,b,a.distance(b),grid.keySet()),plan.path().stream().skip(1).mapToInt(ResearchDragSolverTest::centerCost).sum());
    }
    private static int centerCost(ResearchHex h) { return h.q()*h.q()+h.q()*h.r()+h.r()*h.r(); }
    private static int minimumCenterCost(ResearchHex from,ResearchHex to,int steps,Set<ResearchHex> grid) {
        if(steps==0)return from.equals(to)?0:100000;
        int best=100000;
        for(int d=0;d<6;d++) {
            var next=from.neighbor(d);
            if(grid.contains(next)&&next.distance(to)==steps-1)best=Math.min(best,centerCost(next)+minimumCenterCost(next,to,steps-1,grid));
        }
        return best;
    }
    private final AspectCatalog aspects=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private ResearchNote note(Map<ResearchHex,ResearchNote.Cell> grid) {return new ResearchNote("TEST",0,false,0,grid);}
    private ResearchNote.Cell anchor(String tag) {return new ResearchNote.Cell(1,tag);}
    private Map<String,Integer> stock(int count) {var result=new LinkedHashMap<String,Integer>();aspects.all().forEach(a->result.put(a.tag(),count));return result;}
    @Test void findsCheapestDirectBridgeWithoutChangingFixedEndpoints() {
        var a=new ResearchHex(-1,0);var b=new ResearchHex(1,0);var middle=new ResearchHex(0,0);
        var note=note(Map.of(a,anchor("terra"),middle,ResearchNote.Cell.blank(),b,anchor("ordo")));
        var solver=new ResearchDragSolver(note,aspects,a,stock(10),Set.of());
        var plan=solver.preview(b).orElseThrow();
        assertEquals(Map.of(middle,"vitreus"),plan.placements());
        assertEquals(List.of(a,middle,b),plan.path());
        assertSame(plan,solver.preview(b).orElseThrow());
        assertEquals(0,note.grid().get(middle).type());
    }
    @Test void freeTokenIsUsableAndSnapshotDoesNotFollowPoolChanges() {
        var a=new ResearchHex(-1,0);var b=new ResearchHex(1,0);var middle=new ResearchHex(0,0);
        var pool=stock(0);var free=new HashSet<>(Set.of("vitreus"));
        var solver=new ResearchDragSolver(note(Map.of(a,anchor("terra"),middle,ResearchNote.Cell.blank(),b,anchor("ordo"))),aspects,a,pool,free);
        pool.clear();free.clear();
        assertEquals(Map.of(middle,"vitreus"),solver.preview(b).orElseThrow().placements());
    }
    @Test void missingCellsNegativeCellsAndAlreadyConnectedEndpointsProduceNoSpuriousPlan() {
        var a=new ResearchHex(-1,0);var b=new ResearchHex(1,0);var middle=new ResearchHex(0,0);
        for(var grid:List.of(Map.of(a,anchor("terra"),b,anchor("ordo")),
                Map.of(a,anchor("terra"),middle,new ResearchNote.Cell(3,"vitreus"),b,anchor("ordo")),
                Map.of(a,anchor("terra"),middle,new ResearchNote.Cell(2,"vitreus"),b,anchor("ordo")))) {
            var solver=new ResearchDragSolver(note(grid),aspects,a,stock(10),Set.of());
            assertTrue(solver.preview(b).isEmpty());assertTrue(solver.preview(a).isEmpty());
            assertTrue(solver.preview(new ResearchHex(4,4)).isEmpty());
        }
    }
    @Test void unknownNegativeAspectStillProtectsAdjacentCells() {
        var a=new ResearchHex(-1,0);var b=new ResearchHex(1,0);var middle=new ResearchHex(0,0);
        var pool=stock(10);pool.remove("terra");
        var grid=Map.of(a,anchor("ordo"),middle,ResearchNote.Cell.blank(),b,anchor("ordo"),
                new ResearchHex(0,1),new ResearchNote.Cell(3,"terra"));
        var plan=new ResearchDragSolver(note(grid),aspects,a,pool,Set.of()).preview(b);
        plan.ifPresent(p->assertNotEquals("vitreus",p.placements().get(middle)));
    }
    @Test void reachabilityUsesExactLengthAndBoundedSearchHandlesFullGrid() {
        var grid=new LinkedHashMap<ResearchHex,ResearchNote.Cell>();var center=new ResearchHex(0,0);grid.put(center,ResearchNote.Cell.blank());
        for(int r=1;r<=4;r++)for(var h:ResearchHex.ring(r))grid.put(h,ResearchNote.Cell.blank());
        var solver=new ResearchDragSolver(note(grid),aspects,new ResearchHex(-4,0),stock(0),Set.of());
        assertTrue(solver.reachable("terra","terra",0));assertFalse(solver.reachable("terra","terra",1));
        assertTrue(solver.reachable("terra","terra",2));assertTrue(solver.reachable("terra","vitreus",1));
        assertTimeoutPreemptively(Duration.ofSeconds(2),()->assertTrue(solver.preview(new ResearchHex(4,0)).isEmpty()));
    }
    @Test void quantityThresholdExcludesInsufficientRepeatedAspects() {
        var grid=new LinkedHashMap<ResearchHex,ResearchNote.Cell>();
        for(int q=-3;q<=3;q++)grid.put(new ResearchHex(q,0),ResearchNote.Cell.blank());
        grid.put(new ResearchHex(-3,0),anchor("terra"));grid.put(new ResearchHex(3,0),anchor("terra"));
        assertTrue(new ResearchDragSolver(note(grid),aspects,new ResearchHex(-3,0),stock(1),Set.of()).preview(new ResearchHex(3,0)).isEmpty());
    }
}
