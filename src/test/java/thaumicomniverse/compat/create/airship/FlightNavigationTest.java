package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightNavigationTest extends FlightTestConfig {
    private FlightGraph.Node node(int x,int z,boolean station){return new FlightGraph.Node(UUID.randomUUID(),ResourceLocation.parse("minecraft:overworld"),new BlockPos(x,78,z),new BlockPos(x,80,z),Direction.EAST,station,"N"+x+","+z);}
    private FlightState ship(FlightData d,FlightGraph.Node n){var s=new FlightState(UUID.randomUUID(),n.dimension(),n.point());s.atNode=n.id();s.yaw=n.facing().toYRot();s.initialFacing=n.facing();d.ships.put(s.id,s);if(n.station())d.graph.reserveStation(n.id(),s.id);return s;}
    @Test void departureTurnHasNoTeleportAndLevelDeckFollowsTheRoute(){
        var d=new FlightData();var a=node(0,0,true);var b=node(-80,0,true);d.graph.add(a);d.graph.add(b);d.graph.connect(a.id(),b.id());var s=ship(d,a);s.forward=true;
        boolean moved=false;
        for(int i=0;i<6000&&!b.id().equals(s.atNode);i++){
            s.dockHeld=true;var before=s.position;FlightNavigation.tick(d.graph,s,.4);
            assertTrue(before.distanceTo(s.position)<=.401,"No position jump at a transition boundary");assertTrue(s.speed<=.4+1e-8);moved|=s.speed>0;
        }
        assertTrue(moved);assertEquals(b.id(),s.atNode);assertEquals(b.point(),s.position);assertEquals(0,s.speed);assertNull(d.graph.stationOwner(a.id()));assertEquals(s.id,d.graph.stationOwner(b.id()));
    }
    @Test void alignedDepartureUsesTheRouteImmediately(){
        var d=new FlightData();var a=node(0,0,true);var b=node(80,0,true);d.graph.add(a);d.graph.add(b);d.graph.connect(a.id(),b.id());var s=ship(d,a);s.forward=true;
        FlightNavigation.tick(d.graph,s,.4);assertNull(s.transition);assertNotNull(s.route);
        assertEquals(b.id(),FlightNavigation.approaching(d.graph,s));
        for(int i=0;i<12;i++){assertEquals(b.id(),FlightNavigation.approaching(d.graph,s));s.forward=true;FlightNavigation.tick(d.graph,s,.4);}
        assertTrue(s.distance>0);
    }
    @Test void oppositeTrafficAndFullDestinationWaitBeforeLeaving(){
        var d=new FlightData();var a=node(0,0,true);var b=node(80,0,true);d.graph.add(a);d.graph.add(b);var r=d.graph.connect(a.id(),b.id());
        var s=ship(d,a);var other=ship(d,b);s.forward=true;FlightNavigation.tick(d.graph,s,.4);assertEquals(a.id(),s.atNode);assertEquals(a.point(),s.position);
        d.graph.release(other.id);d.graph.reserveRoute(r.id(),other.id);FlightNavigation.tick(d.graph,s,.4);assertEquals(a.id(),s.atNode);
        d.graph.release(other.id);FlightNavigation.tick(d.graph,s,.4);assertNotNull(s.route);assertEquals(s.id,d.graph.routeOwner(r.id()));assertEquals(s.id,d.graph.stationOwner(b.id()));
    }
    @Test void waypointPassesWithContinuousPositionAndReservesBothSegmentsDuringTurn(){
        var d=new FlightData();var a=node(0,0,true);var b=node(50,0,false);var c=node(50,70,true);for(var n:List.of(a,b,c))d.graph.add(n);var first=d.graph.connect(a.id(),b.id());var second=d.graph.connect(b.id(),c.id());d.graph.edit(second.id(),"corner",0,List.of(),false);var s=ship(d,a);s.forward=true;boolean turning=false;
        for(int i=0;i<6000&&!c.id().equals(s.atNode);i++){
            s.dockHeld=true;var before=s.position;FlightNavigation.tick(d.graph,s,.5);assertTrue(before.distanceTo(s.position)<=.501);
            if(s.transitionFromRoute!=null){turning=true;assertTrue(s.selectionLocked);assertEquals(s.id,d.graph.routeOwner(first.id()));assertEquals(s.id,d.graph.routeOwner(second.id()));var locked=s.selectedExit;FlightNavigation.choose(d.graph,s,1);assertEquals(locked,s.selectedExit);}
        }
        assertTrue(turning);assertEquals(c.id(),s.atNode);assertNull(d.graph.routeOwner(first.id()));assertNull(d.graph.routeOwner(second.id()));
    }
    @Test void savingDuringTurnRestoresBothReservationsAndInventorySnapshot(){
        var d=new FlightData();var a=node(0,0,true);var b=node(40,0,false);var c=node(40,40,true);for(var n:List.of(a,b,c))d.graph.add(n);var first=d.graph.connect(a.id(),b.id());var second=d.graph.connect(b.id(),c.id());d.graph.edit(second.id(),"corner",0,List.of(),false);var s=ship(d,a);s.forward=true;s.contraption.putString("InventoryWitness","unique stack payload");
        for(int i=0;i<3000&&s.transitionFromRoute==null;i++)FlightNavigation.tick(d.graph,s,.5);
        assertNotNull(s.transitionFromRoute);var loaded=FlightData.load(d.save(new CompoundTag()));var copy=loaded.ships.get(s.id);
        assertFalse(copy.forward);assertEquals(s.position,copy.position);assertEquals("unique stack payload",copy.contraption.getString("InventoryWitness"));assertFalse(copy.paused);
        assertEquals(s.id,loaded.graph.routeOwner(first.id()));assertEquals(s.id,loaded.graph.routeOwner(second.id()));assertEquals(s.id,loaded.graph.stationOwner(b.id()));assertEquals(s.id,loaded.graph.stationOwner(c.id()));
        copy.forward=true;copy.dockHeld=true;for(int i=0;i<3000&&!c.id().equals(copy.atNode);i++)FlightNavigation.tick(loaded.graph,copy,.5);assertEquals(c.id(),copy.atNode);
    }
    @Test void aTerminalWithoutAnotherRouteDocksWithoutSpace(){
        var d=new FlightData();var a=node(0,0,true);var b=node(20,0,true);d.graph.add(a);d.graph.add(b);d.graph.connect(a.id(),b.id());var s=ship(d,a);s.forward=true;
        for(int i=0;i<300;i++)FlightNavigation.tick(d.graph,s,.5);
        assertEquals(b.id(),s.atNode);assertEquals(b.point(),s.position);assertEquals(0,s.speed);
    }
    @Test void uTurnKeepsBothEndpointAndInternalTangents(){
        var p=FlightPath.transition(Vec3.ZERO,new Vec3(1,0,0),new Vec3(-6,0,0),new Vec3(-1,0,0),6);
        assertEquals(1,p.tangent(0).dot(new Vec3(1,0,0)),1e-8);assertEquals(1,p.tangent(p.length()).dot(new Vec3(-1,0,0)),1e-8);
        assertEquals(1,p.curves.size(),"A manoeuvre is a single Bezier, never a chain");
        for(double t=0;t<p.length();t+=.05)assertTrue(p.position(t).distanceTo(p.position(Math.min(p.length(),t+.05)))<=.051);
    }
    @Test void repeatedHeldInputCannotRelaunchUntilTheDriverReleasesThrottle(){
        var d=new FlightData();var a=node(0,0,true);var b=node(20,0,true);d.graph.add(a);d.graph.add(b);d.graph.connect(a.id(),b.id());var s=ship(d,a);
        for(int i=0;i<400;i++){FlightNavigation.manualInput(s,true,false,true);FlightNavigation.tick(d.graph,s,.5);}
        assertEquals(b.id(),s.atNode);assertEquals(b.point(),s.position);assertEquals(0,s.speed);assertTrue(s.awaitingThrottleRelease);
        FlightNavigation.manualInput(s,false,false,true);FlightNavigation.tick(d.graph,s,.5);assertFalse(s.awaitingThrottleRelease);assertEquals(b.id(),s.atNode);
        FlightNavigation.manualInput(s,true,false,false);FlightNavigation.tick(d.graph,s,.5);assertNull(s.atNode);assertNotNull(s.transition);
    }
    @Test void holdingReverseBrakesTurnsOnceAndContinuesToThePreviousStation(){
        var d=new FlightData();var a=node(0,0,true);var b=node(100,0,true);d.graph.add(a);d.graph.add(b);d.graph.connect(a.id(),b.id());var s=ship(d,a);
        for(int i=0;i<150;i++){FlightNavigation.manualInput(s,true,false,true);FlightNavigation.tick(d.graph,s,.4);}assertNull(s.atNode);assertNotNull(s.route);
        int turns=0;boolean wasTurning=false;
        for(int i=0;i<3000;i++){
            FlightNavigation.manualInput(s,false,true,true);var before=s.position;FlightNavigation.tick(d.graph,s,.4);assertTrue(before.distanceTo(s.position)<=.401);
            boolean turning=s.transitionFromRoute!=null;if(turning&&!wasTurning)turns++;wasTurning=turning;
        }
        assertEquals(0,turns,"Reversing stops and turns in place without creating a bend");assertEquals(a.id(),s.atNode);assertEquals(0,s.speed);
    }
}
