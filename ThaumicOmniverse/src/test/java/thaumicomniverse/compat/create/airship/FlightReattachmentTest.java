package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightReattachmentTest extends FlightTestConfig {
    private static final ResourceLocation DIM=ResourceLocation.parse("minecraft:overworld");
    private FlightGraph.Node node(int x, int z) {return new FlightGraph.Node(UUID.randomUUID(),DIM,new BlockPos(x,78,z),new BlockPos(x,80,z),Direction.EAST,true,"N"+x+","+z);}
    private FlightData data(){var d=new FlightData();var a=node(0,0);var b=node(100,0);d.graph.add(a);d.graph.add(b);d.graph.connect(a.id(),b.id());return d;}
    private FlightState ship(FlightData d,Vec3 p,float yaw){var s=new FlightState(UUID.randomUUID(),DIM,p);s.yaw=yaw;d.ships.put(s.id,s);return s;}
    @Test void nearestPointAndFacingChooseBothDirectionsOnTheSameRoute(){
        var d=data();var route=d.graph.routes().iterator().next();
        for(float yaw:new float[]{-90,90}){
            var s=ship(d,new Vec3(45.5,86,12.5),yaw);var entry=FlightReattachment.nearest(d.graph,s);
            assertEquals(45.5,entry.point().x,.001);assertEquals(80,entry.point().y,.001);assertEquals(.5,entry.point().z,.001);
            assertEquals(yaw==-90?route.from():route.to(),entry.from());
            assertTrue(FlightNavigation.forward(yaw).dot(route.leaving(entry.from()).tangent(entry.distance()))>.999);
        }
    }
    @Test void wStartsOneContinuousReturnAndReleasesItsLatchOnJoining(){
        var d=data();var s=ship(d,new Vec3(45.5,85.5,12.5),-90);s.forward=true;
        FlightNavigation.tick(d.graph,s,.4);assertTrue(s.rejoining);assertTrue(s.rejoinAutomatic);
        assertEquals(1,s.transition.curves.size());assertTrue(s.transition.length()<=40);assertEquals(1,s.transition.tangent(0).dot(FlightNavigation.forward(s.yaw)),1e-8);
        s.forward=false;var path=s.transition;int ticks=0;
        while(s.rejoining&&ticks++<1000){var before=s.position;FlightNavigation.tick(d.graph,s,.4);assertTrue(before.distanceTo(s.position)<=.401,"continuous bounded travel");}
        assertTrue(ticks<1000);assertNotNull(s.route);assertFalse(s.rejoinAutomatic);
        assertTrue(path.position(path.length()).distanceTo(s.position)<=.4,"Remainder crosses the tangent seam in the same tick");assertTrue(FlightNavigation.forward(s.yaw).dot(d.graph.route(s.route).leaving(s.fromNode).tangent(s.distance))>.999);
    }
    @Test void spaceAloneReturnsButReleasingItStopsBeforeTheRoute(){
        var d=data();var s=ship(d,new Vec3(45.5,85.5,30.5),-90);s.dockHeld=true;
        for(int i=0;i<15;i++)FlightNavigation.tick(d.graph,s,.4);assertTrue(s.rejoining);assertFalse(s.rejoinAutomatic);
        s.dockHeld=false;for(int i=0;i<100;i++)FlightNavigation.tick(d.graph,s,.4);assertEquals(0,s.speed);assertTrue(s.rejoining);
        s.dockHeld=true;for(int i=0;i<1000&&s.rejoining;i++)FlightNavigation.tick(d.graph,s,.4);assertNotNull(s.route);assertFalse(s.rejoining);
    }
    @Test void deleteDuringTravelOrReturnRetainsHullInventoryPoseAndClearsReservations(){
        for(boolean joining:List.of(false,true)){
            var d=data();var s=ship(d,new Vec3(45.5,85.5,12.5),-90);var r=d.graph.routes().iterator().next();
            s.contraption.putString("Witness","retained");
            if(joining){s.forward=true;FlightNavigation.tick(d.graph,s,.4);s.forward=false;}
            else{s.route=r.id();s.fromNode=r.from();s.distance=45;s.position=r.curve().position(45);d.graph.reserveRoute(r.id(),s.id);}
            var before=s.position;float yaw=s.yaw;d.graph.deleteRoute(r.id());FlightNavigation.tick(d.graph,s,.4);
            assertTrue(FlightReattachment.detached(s));assertEquals(before,s.position);assertEquals(yaw,s.yaw);assertEquals("retained",s.contraption.getString("Witness"));assertEquals(0,s.speed);
            assertNull(d.graph.stationOwner(r.to()));assertNull(d.graph.routeOwner(r.id()));
            var copy=FlightData.load(d.save(new CompoundTag())).ships.get(s.id);assertEquals(before,copy.position);assertTrue(FlightReattachment.detached(copy));
        }
    }
    @Test void returnRespectsOccupancyAndDimensionAndSavesMidReturn(){
        var d=data();var s=ship(d,new Vec3(45.5,85.5,12.5),-90);var r=d.graph.routes().iterator().next();var other=UUID.randomUUID();
        d.graph.reserveRoute(r.id(),other);assertFalse(FlightReattachment.begin(d.graph,s,true));assertTrue(FlightReattachment.detached(s));
        d.graph.release(other);d.graph.reserveStation(r.to(),other);assertFalse(FlightReattachment.begin(d.graph,s,true));assertNull(d.graph.routeOwner(r.id()));
        d.graph.release(other);s.dimension=ResourceLocation.parse("minecraft:the_nether");assertNull(FlightReattachment.nearest(d.graph,s));s.dimension=DIM;
        assertTrue(FlightReattachment.begin(d.graph,s,true));for(int i=0;i<12;i++)FlightNavigation.tick(d.graph,s,.4);
        var loaded=FlightData.load(d.save(new CompoundTag()));var copy=loaded.ships.get(s.id);assertTrue(copy.rejoining);assertTrue(copy.rejoinAutomatic);assertEquals(s.position,copy.position);
        assertEquals(s.id,loaded.graph.routeOwner(r.id()));assertEquals(s.id,loaded.graph.stationOwner(r.to()));
        for(int i=0;i<1000&&copy.rejoining;i++)FlightNavigation.tick(loaded.graph,copy,.4);assertEquals(r.id(),copy.route);
    }
    @Test void relocationAfterDockingClearsThrottleReleaseGate(){
        var d=data();var s=ship(d,new Vec3(45.5,85.5,12.5),-90);s.awaitingThrottleRelease=true;s.paused=true;
        FlightReattachment.detach(d.graph,s,"airship.rejoin.detached");FlightNavigation.manualInput(s,true,false,false);FlightNavigation.tick(d.graph,s,.4);assertTrue(s.rejoining);
    }
    @Test void spaceAloneFinishesApproachWithoutW(){
        var d=data();var r=d.graph.routes().iterator().next();var s=ship(d,r.curve().position(95),-90);
        s.route=r.id();s.fromNode=r.from();s.distance=95;s.dockHeld=true;d.graph.reserveRoute(r.id(),s.id);d.graph.reserveStation(r.to(),s.id);
        for(int i=0;i<150&&s.atNode==null;i++)FlightNavigation.tick(d.graph,s,.4);
        assertEquals(r.to(),s.atNode);assertEquals(d.graph.node(r.to()).point(),s.position);
    }
}
