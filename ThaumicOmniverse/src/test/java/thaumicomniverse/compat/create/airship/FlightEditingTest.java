package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightEditingTest extends FlightTestConfig {
    private static FlightGraph.Node node(int x,int z,Direction direction){return new FlightGraph.Node(UUID.randomUUID(),FlightBendTest.DIM,new BlockPos(x,78,z),new BlockPos(x,80,z),direction,true,"",3,3);}
    @Test void routeLockUsesAnchorDirectionsAndUnlockKeepsFreeHandles(){
        var g=new FlightGraph();var a=node(0,0,Direction.SOUTH);var b=node(80,0,Direction.WEST);g.add(a);g.add(b);var r=g.connect(a.id(),b.id());
        var free=List.of(a.point().add(10,20,10),b.point().add(10,20,10));
        g.edit(r.id(),"locked",1,free,true);var c=g.route(r.id()).curve();
        assertEquals(1,c.derivative(0).normalize().dot(new Vec3(0,0,1)),1e-10);
        assertEquals(1,c.derivative(1).normalize().dot(new Vec3(-1,0,0)),1e-10);
        g.edit(r.id(),"free",1,free,false);assertEquals(free,g.route(r.id()).curve().controls);
        assertFalse(g.route(r.id()).lockEndDirections());
    }
    @Test void lineAndQuadraticConstraintsKeepIndependentNonzeroEndpointDerivatives(){
        for(var c:List.of(FlightCurve.straight(Vec3.ZERO,new Vec3(20,0,0)),new FlightCurve(Vec3.ZERO,List.of(new Vec3(5,5,5)),new Vec3(20,0,0)))){
            var aligned=FlightEditing.align(c,new Vec3(0,1,0),new Vec3(0,0,1));assertTrue(aligned.controls.size()>=2);
            assertEquals(1,aligned.derivative(0).normalize().y,1e-10);assertEquals(1,aligned.derivative(1).normalize().z,1e-10);
        }
    }
    @Test void passingEndsMoveWithinLengthBudgetAndFreeBendsStayFreeAfterRouteEdits(){
        var g=new FlightGraph();var a=node(0,0,Direction.EAST);var b=node(80,0,Direction.EAST);var c=node(0,80,Direction.SOUTH);for(var n:List.of(a,b,c))g.add(n);
        var ab=g.connect(a.id(),b.id());var ac=g.connect(a.id(),c.id());var bend=g.bend(a.id(),ab.id(),ac.id(),true);
        var free=List.of(new Vec3(8,83,7),new Vec3(5,83,11));
        g.editBend(bend.id(),"free",4,free,4096,false,12,15);var moved=g.bend(bend.id());
        assertEquals(ab.leaving(a.id()).position(12),moved.curve().start);assertEquals(ac.leaving(a.id()).position(15),moved.curve().end);assertEquals(free,moved.curve().controls);
        g.edit(ab.id(),"changed",0,List.of(a.point().add(20,10,0),b.point().add(-20,10,0)),false);
        assertEquals(12,g.bend(bend.id()).entry(),1e-8);
        var relocated=g.bend(bend.id());assertFalse(relocated.lockEndDirections());
        assertEquals("free",relocated.name());assertEquals(4,relocated.color());
        assertEquals(g.route(ab.id()).leaving(a.id()).position(12),relocated.curve().start);
        assertEquals(ac.leaving(a.id()).position(15),relocated.curve().end);
        assertTrue(relocated.curve().length()<=40);
        assertTrue(Math.abs(relocated.curve().tangent(0).dot(g.route(ab.id()).leaving(a.id()).tangent(12)))<.999,
                "Unlocked handles must not acquire an endpoint direction lock");
        var restored=FlightData.bend(FlightData.bend(g.bend(bend.id())));assertFalse(restored.lockEndDirections());assertEquals(12,restored.entry());
    }
    @Test void lockedMovedBendIsTangentAndDepartureCannotMoveItsStationEnd(){
        var g=new FlightGraph();var a=node(0,0,Direction.EAST);var b=node(80,0,Direction.EAST);var c=node(0,80,Direction.SOUTH);for(var n:List.of(a,b,c))g.add(n);
        var ab=g.connect(a.id(),b.id());var ac=g.connect(a.id(),c.id());var bend=g.bend(a.id(),ab.id(),ac.id(),true);
        g.editBend(bend.id(),"tangent",4,bend.curve().controls,4096,true,12,15);var moved=g.bend(bend.id());
        assertEquals(1,moved.curve().derivative(0).normalize().dot(ab.leaving(a.id()).tangent(12).scale(-1)),1e-8);
        assertEquals(1,moved.curve().derivative(1).normalize().dot(ac.leaving(a.id()).tangent(15)),1e-8);
        var departure=g.bend(a.id(),ab.id(),ac.id(),false);long revision=g.revision();
        assertThrows(IllegalArgumentException.class,()->g.editBend(departure.id(),"bad",4,departure.curve().controls,4096,false,1,15));
        assertSame(departure,g.bend(departure.id()));assertEquals(revision,g.revision());
        for(double invalid:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY,81})assertThrows(IllegalArgumentException.class,()->g.editBend(bend.id(),"bad",4,bend.curve().controls,4096,false,invalid,15));
        assertSame(moved,g.bend(bend.id()));
    }
    @Test void oldSavesDefaultToLockWithoutRewritingGeometry(){
        var g=new FlightGraph();var a=node(0,0,Direction.SOUTH);var b=node(80,0,Direction.WEST);g.add(a);g.add(b);var r=g.connect(a.id(),b.id());
        var tag=FlightData.route(r);tag.remove("LockEndDirections");var loaded=FlightData.route(tag);assertTrue(loaded.lockEndDirections());assertEquals(r.curve().controls,loaded.curve().controls);
        var bend=g.bends().iterator().next();var nbt=FlightData.bend(bend);nbt.remove("LockEndDirections");assertTrue(FlightData.bend(nbt).lockEndDirections());
        g.edit(r.id(),"free",0,r.curve().controls,false);assertFalse(FlightData.route(FlightData.route(g.route(r.id()))).lockEndDirections());
    }
}
