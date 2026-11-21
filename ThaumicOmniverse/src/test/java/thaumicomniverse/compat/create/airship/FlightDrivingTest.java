package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightDrivingTest extends FlightTestConfig {
    @Test void angularBudgetIncludesVerticalTurnsAndDoesNotCancelOpposingBends(){
        var curves=List.of(new FlightCurve(Vec3.ZERO,new Vec3(1,20,0),new Vec3(2,-20,0),new Vec3(30,0,0)),new FlightCurve(Vec3.ZERO,new Vec3(0,0,20),new Vec3(0,20,20),new Vec3(0,20,0)));
        double budget=Math.toRadians(10)/20;
        for(var curve:curves)for(double maximum:new double[]{.2,2}){
            double distance=0;int ticks=0;boolean limited=false;
            while(distance<curve.length()-1e-8&&ticks++<20000){double wanted=Math.min(maximum,curve.length()-distance);double moved=FlightSteering.advance(curve::tangent,distance,wanted,budget);assertTrue(moved>0);assertTrue(FlightSteering.angle(curve.tangent(distance),curve.tangent(distance+moved))<=budget+1e-7);limited|=moved<wanted*.9;distance+=moved;}
            assertTrue(ticks<20000);assertTrue(limited);
        }
    }
    @Test void fortyTickGestureLatchesAndLeavingFreeModeRequiresSpace(){
        var d=new FlightData();var a=FlightBendTest.node(0,80,0,0,0);var b=FlightBendTest.node(80,80,0,0,0);d.graph.add(a);d.graph.add(b);d.graph.connect(a.id(),b.id());
        var s=new FlightState(UUID.randomUUID(),a.dimension(),a.point().add(10,10,10));d.ships.put(s.id,s);s.leftHeld=s.rightHeld=true;
        for(int i=0;i<39;i++)FlightNavigation.tick(d.graph,s,.5);assertFalse(s.freeControl);FlightNavigation.tick(d.graph,s,.5);assertTrue(s.freeControl);
        for(int i=0;i<100;i++)FlightNavigation.tick(d.graph,s,.5);assertTrue(s.freeControl);
        s.leftHeld=s.rightHeld=false;s.downHeld=true;double y=s.position.y;for(int i=0;i<30;i++)FlightNavigation.tick(d.graph,s,.5);assertTrue(s.position.y<y);s.downHeld=false;
        s.leftHeld=s.rightHeld=true;for(int i=0;i<40;i++)FlightNavigation.tick(d.graph,s,.5);assertFalse(s.freeControl);assertTrue(s.requireSpaceReturn);
        s.leftHeld=s.rightHeld=false;s.forward=true;for(int i=0;i<20;i++)FlightNavigation.tick(d.graph,s,.5);assertTrue(FlightReattachment.detached(s));
        s.dockHeld=true;FlightNavigation.tick(d.graph,s,.5);assertTrue(s.rejoining);assertFalse(s.rejoinAutomatic);
        var copy=FlightData.load(d.save(new CompoundTag())).ships.get(s.id);assertFalse(copy.leftHeld);assertFalse(copy.downHeld);assertTrue(copy.requireSpaceReturn);
    }
    @Test void unmatchedAssemblyHeadingUsesContinuousTangentDeparture(){
        var d=new FlightData();var a=FlightBendTest.node(0,80,0,3,3);var b=FlightBendTest.node(80,80,0,3,3);d.graph.add(a);d.graph.add(b);var route=d.graph.connect(a.id(),b.id());
        var s=new FlightState(UUID.randomUUID(),a.dimension(),a.point());s.atNode=a.id();s.yaw=0;s.forward=true;d.graph.reserveStation(a.id(),s.id);
        FlightNavigation.tick(d.graph,s,.5);assertNotNull(s.transition);assertEquals(a.point(),s.position);
        assertEquals(1,s.transition.tangent(0).dot(FlightNavigation.forward(0)),1e-8);
        assertEquals(1,s.transition.tangent(s.transition.length()).dot(route.curve().tangent(s.transitionEndDistance)),1e-8);
        assertTrue(s.transition.length()<=40);boolean moved=false;
        for(int i=0;i<6000&&!b.id().equals(s.atNode);i++){
            var before=s.position;var direction=s.motionDirection;FlightNavigation.tick(d.graph,s,.5);
            assertTrue(before.distanceTo(s.position)<=.5001);moved|=before.distanceTo(s.position)>1e-6;
            assertTrue(FlightSteering.angle(direction,s.motionDirection)<=Math.toRadians(.5)+1e-7);
        }
        assertTrue(moved);assertEquals(b.id(),s.atNode);assertEquals(b.point(),s.position);
    }
    @Test void chosenRoutePassesStationWhileDefaultDockStops(){
        for(boolean dock:List.of(false,true)){
            var d=new FlightData();var a=FlightBendTest.node(0,80,0,3,3);var b=FlightBendTest.node(50,80,0,3,3);var c=FlightBendTest.node(50,80,60,3,3);for(var n:List.of(a,b,c))d.graph.add(n);d.graph.connect(a.id(),b.id());var onward=d.graph.connect(b.id(),c.id());d.graph.edit(onward.id(),"corner",0,List.of(),false);
            var s=new FlightState(UUID.randomUUID(),a.dimension(),a.point());s.atNode=a.id();s.yaw=-90;d.graph.reserveStation(a.id(),s.id);boolean passed=false;
            for(int i=0;i<5000&&!s.awaitingThrottleRelease;i++){FlightNavigation.manualInput(s,true,false,dock);FlightNavigation.prepareChoice(d.graph,s);if(!dock&&b.id().equals(FlightNavigation.approaching(d.graph,s))){s.dockSelected=false;s.selectedExit=onward.id();}var before=s.position;FlightNavigation.tick(d.graph,s,.5);assertTrue(before.distanceTo(s.position)<=.5001);passed|=s.transitionFromRoute!=null;}
            assertEquals(dock?b.id():c.id(),s.atNode);assertEquals(!dock,passed);
        }
    }
    @Test void choosingAfterTheMergePointNeverJumpsBackwards(){
        var d=new FlightData();var a=FlightBendTest.node(0,80,0,3,3);var b=FlightBendTest.node(50,80,0,3,3);var c=FlightBendTest.node(50,80,60,3,3);for(var n:List.of(a,b,c))d.graph.add(n);var r=d.graph.connect(a.id(),b.id());var onward=d.graph.connect(b.id(),c.id());
        var s=new FlightState(UUID.randomUUID(),a.dimension(),r.curve().position(49));s.route=r.id();s.fromNode=a.id();s.distance=49;s.yaw=-90;s.motionDirection=new Vec3(1,0,0);s.forward=true;d.graph.reserveRoute(r.id(),s.id);d.graph.reserveStation(b.id(),s.id);
        FlightNavigation.prepareChoice(d.graph,s);s.dockSelected=false;s.selectedExit=onward.id();for(int i=0;i<5000&&!c.id().equals(s.atNode);i++){var before=s.position;FlightNavigation.tick(d.graph,s,.5);assertTrue(before.distanceTo(s.position)<=.5001);}
        assertEquals(c.id(),s.atNode);
    }
}
