package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightControlRevisionTest extends FlightTestConfig {
    private static final ResourceLocation DIM=ResourceLocation.parse("minecraft:overworld");
    private FlightGraph.Node node(int x,int z,boolean station){return new FlightGraph.Node(UUID.randomUUID(),DIM,new BlockPos(x,78,z),new BlockPos(x,80,z),Direction.EAST,station,"N");}
    private FlightState on(FlightGraph g,FlightGraph.Route r,double distance){
        var s=new FlightState(UUID.randomUUID(),DIM,r.curve().position(distance));s.route=r.id();s.fromNode=r.from();s.distance=distance;s.yaw=-90;s.motionDirection=r.curve().tangent(distance);
        assertTrue(FlightNavigation.reserve(g,s,r,r.from()));return s;
    }
    @Test void stationChoiceDefaultsToDockAndCyclesWithEveryRoute(){
        var g=new FlightGraph();var a=node(0,0,true);var b=node(100,0,true);var c=node(200,0,true);for(var n:List.of(a,b,c))g.add(n);
        var ab=g.connect(a.id(),b.id());g.connect(b.id(),c.id());var s=on(g,ab,70);
        FlightNavigation.prepareChoice(g,s);assertTrue(s.dockSelected);assertNull(s.selectedExit);assertTrue(FlightNavigation.canApproach(g,s));
        var seen=new HashSet<UUID>();for(int i=0;i<2;i++){FlightNavigation.choose(g,s,1);assertFalse(s.dockSelected);seen.add(s.selectedExit);}
        assertEquals(2,seen.size());s.dockHeld=true;FlightNavigation.tick(g,s,.35);assertEquals(0,s.speed,1e-9);assertEquals(0,s.dockDistanceTotal);assertFalse(FlightNavigation.canApproach(g,s));
        FlightNavigation.choose(g,s,1);assertTrue(s.dockSelected);FlightNavigation.tick(g,s,.35);assertTrue(s.speed>0);assertTrue(s.dockDistanceTotal>0);
    }
    @Test void waypointHasNoDockOptionAndUsesArrivalTangentRatherThanDistantYaw(){
        var g=new FlightGraph();var a=node(0,0,true);var b=node(100,0,false);var c=node(200,0,true);var d=node(100,100,true);for(var n:List.of(a,b,c,d))g.add(n);
        var ab=g.connect(a.id(),b.id());var bc=g.connect(b.id(),c.id());g.connect(b.id(),d.id());var s=on(g,ab,30);s.yaw=0;
        FlightNavigation.prepareChoice(g,s);assertEquals(bc.id(),s.selectedExit);assertFalse(FlightNavigation.dockingOption(g,s));
        for(int i=0;i<6;i++){FlightNavigation.choose(g,s,1);assertFalse(s.dockSelected);assertNotNull(s.selectedExit);}
    }
    @Test void freeFlightUsesProvidedAccelerationForThrottleAndRelease(){
        for(double acceleration:new double[]{3d/1600,6d/1600}){
            var s=new FlightState(UUID.randomUUID(),DIM,Vec3.ZERO);s.freeControl=true;s.forward=true;s.acceleration=acceleration;
            var g=new FlightGraph();for(int i=0;i<180;i++){double before=s.speed;FlightNavigation.tick(g,s,.5);assertTrue(s.speed-before<=acceleration+1e-10);}
            s.forward=false;for(int i=0;i<180;i++){double before=s.speed;FlightNavigation.tick(g,s,.5);assertTrue(before-s.speed<=acceleration+1e-10);}
            assertEquals(0,s.speed,1e-9);
        }
    }
    @Test void straightStationApproachBrakesWithinTheNormalBudget(){
        var g=new FlightGraph();var a=node(0,0,true);var b=node(100,0,true);g.add(a);g.add(b);var r=g.connect(a.id(),b.id());var s=on(g,r,1);s.forward=true;s.dockHeld=true;
        int ticks=0;double worst=0;while(s.atNode==null&&ticks++<3000){double before=s.speed;FlightNavigation.tick(g,s,.35);worst=Math.max(worst,Math.abs(s.speed-before));}
        assertEquals(b.id(),s.atNode);assertTrue(worst<=s.acceleration+1e-7,"normal braking delta="+worst);
    }
    @Test void modeGestureShowsFortyTicksAndOnlyTogglesOnceUntilReleased(){
        var s=new FlightState(UUID.randomUUID(),DIM,Vec3.ZERO);var g=new FlightGraph();s.leftHeld=s.rightHeld=true;
        for(int i=1;i<40;i++){FlightNavigation.tick(g,s,.35);assertEquals(i,s.modeGestureTicks);assertFalse(s.freeControl);}
        FlightNavigation.tick(g,s,.35);assertTrue(s.freeControl);assertTrue(s.modeGestureLatched);
        for(int i=0;i<60;i++)FlightNavigation.tick(g,s,.35);assertTrue(s.freeControl);
        s.leftHeld=false;FlightNavigation.tick(g,s,.35);assertEquals(0,s.modeGestureTicks);
        s.leftHeld=true;for(int i=0;i<40;i++)FlightNavigation.tick(g,s,.35);assertFalse(s.freeControl);assertTrue(s.requireSpaceReturn);
    }
    @Test void ordinaryCornerAnticipatesBrakingAndDoesNotStopAtTheCurveSeams(){
        var g=new FlightGraph();var a=node(0,0,true);var b=node(100,0,false);var c=node(100,100,true);for(var n:List.of(a,b,c))g.add(n);
        var r=g.connect(a.id(),b.id());var onward=g.connect(b.id(),c.id());g.edit(onward.id(),"corner",0,List.of(),false);var s=on(g,r,1);s.forward=true;s.dockHeld=true;
        int ticks=0;double worst=0;boolean turning=false;while(s.atNode==null&&ticks++<15000){
            double before=s.speed;var position=s.position;FlightNavigation.tick(g,s,.35);
            assertTrue(position.distanceTo(s.position)<=.35001);worst=Math.max(worst,Math.abs(s.speed-before));turning|=s.transition!=null;
        }
        assertTrue(turning);assertEquals(c.id(),s.atNode);assertTrue(worst<=s.acceleration*1.05+1e-7,"corner acceleration delta="+worst);
    }
    @Test void oversizedRadiusShrinksToKeepConnectorsWithinFortyBlocks(){
        var g=new FlightGraph();var a=node(0,0,true);var b=node(80,0,true);g.add(a);g.add(b);thaumicomniverse.RevivalFeatures.AIRSHIP_DEFAULT_BEND_RADIUS.set(128d);
        assertNotNull(g.connect(a.id(),b.id()));
        assertFalse(g.bends().isEmpty());
        for(var bend:g.bends())assertTrue(bend.curve().length()<=40+1e-8);
        assertTrue(g.bends().stream().filter(bend->bend.node().equals(a.id())).allMatch(bend->bend.radius()<128));
    }
    @Test void returnCurveMatchesBothTangentsAndRejectsFarRoutes(){
        var g=new FlightGraph();var a=node(0,0,true);var b=node(100,0,true);g.add(a);g.add(b);g.connect(a.id(),b.id());
        var s=new FlightState(UUID.randomUUID(),DIM,new Vec3(40,85,10));s.yaw=-90;
        assertTrue(FlightReattachment.begin(g,s,false));var c=s.transition.curves.get(0);
        assertEquals(1,c.tangent(0).dot(FlightNavigation.forward(-90)),1e-9);
        assertEquals(1,c.tangent(c.length()).dot(g.route(s.transitionToRoute).leaving(s.transitionToNode).tangent(s.transitionEndDistance)),1e-9);
        assertTrue(c.length()<=40);assertTrue(FlightManeuver.gentle(c,2));
        var far=new FlightState(UUID.randomUUID(),DIM,new Vec3(40,85,100));far.yaw=-90;
        assertFalse(FlightReattachment.begin(g,far,false));assertTrue(FlightReattachment.detached(far));
    }
}
