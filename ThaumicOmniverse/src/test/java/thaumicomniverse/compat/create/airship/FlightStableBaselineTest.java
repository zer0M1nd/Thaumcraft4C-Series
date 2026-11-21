package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import thaumicomniverse.RevivalFeatures;
import static org.junit.jupiter.api.Assertions.*;

class FlightStableBaselineTest extends FlightTestConfig {
    static FlightGraph.Node node(int x,int z,boolean station,String dimension) {
        return new FlightGraph.Node(UUID.randomUUID(),ResourceLocation.parse(dimension),new BlockPos(x,78,z),new BlockPos(x,80,z),Direction.EAST,station,"fixture");
    }
    @Test void changingGenerationRadiusDoesNotRewriteExistingBends() {
        var g=new FlightGraph();var a=node(0,0,true,"minecraft:overworld");var b=node(80,0,true,"minecraft:overworld");var c=node(160,0,true,"minecraft:overworld");
        for(var n:List.of(a,b,c))g.add(n);g.connect(a.id(),b.id());
        var saved=new HashMap<UUID,net.minecraft.nbt.CompoundTag>();g.bends().forEach(bend->saved.put(bend.id(),FlightData.bend(bend)));
        assertEquals(2,RevivalFeatures.AIRSHIP_DEFAULT_BEND_RADIUS.get());
        RevivalFeatures.AIRSHIP_DEFAULT_BEND_RADIUS.set(.5);g.connect(b.id(),c.id());g.ensureBends();
        saved.forEach((id,tag)->assertEquals(tag,FlightData.bend(g.bend(id))));
        assertTrue(g.bends().stream().filter(bend->!saved.containsKey(bend.id())).allMatch(bend->bend.radius()<=.5));
        assertTrue(g.bends().size()>saved.size());
    }
    @Test void sameDirectionWaypointSeamNeedsNoBendAndKeepsMoving() {
        var g=new FlightGraph();var a=node(0,0,true,"minecraft:overworld");var b=node(40,0,false,"minecraft:overworld");var c=node(80,0,true,"minecraft:overworld");
        for(var n:List.of(a,b,c))g.add(n);var ab=g.connect(a.id(),b.id());var bc=g.connect(b.id(),c.id());
        assertNull(g.bend(b.id(),ab.id(),bc.id(),true));assertNull(g.bend(b.id(),ab.id(),bc.id(),false));
        var s=new FlightState(UUID.randomUUID(),a.dimension(),a.point());s.atNode=a.id();s.yaw=-90;s.forward=true;g.reserveStation(a.id(),s.id);
        boolean crossed=false;for(int i=0;i<3000&&!c.id().equals(s.atNode);i++) {
            var previous=s.position;var old=s.route;FlightNavigation.tick(g,s,.35);
            assertTrue(previous.distanceTo(s.position)<=.35001);assertNull(s.transition);
            if(ab.id().equals(old)&&bc.id().equals(s.route)){crossed=true;assertTrue(s.speed>0);assertNull(g.stationOwner(b.id()));}
        }
        assertTrue(crossed);assertEquals(c.id(),s.atNode);assertEquals(c.point(),s.position);
    }
    @Test void onlyWaypointsAcrossDimensionsMayPairAndOnlySpecialWaypointsMayEnter() {
        var g=new FlightGraph();var a=node(0,0,true,"minecraft:overworld");var b=node(40,0,false,"minecraft:overworld");var c=node(80,0,false,"minecraft:overworld");var n=node(5,0,false,"minecraft:the_nether");
        for(var node:List.of(a,b,c,n))g.add(node);g.connect(a.id(),b.id());
        assertTrue(FlightNavigation.canEnter(g,b));g.connect(b.id(),c.id());assertFalse(FlightNavigation.canEnter(g,b));
        assertThrows(IllegalArgumentException.class,()->g.linkPortal(a.id(),n.id()));
        assertThrows(IllegalArgumentException.class,()->g.linkPortal(b.id(),c.id()));
        g.linkPortal(b.id(),n.id());assertTrue(FlightNavigation.canEnter(g,b));
        assertEquals(n.id(),g.portalDestination(b.id()));
    }
    @Test void lateTurnCannotBeSelectedAfterItsBendEntry() {
        var g=new FlightGraph();var a=node(0,0,true,"minecraft:overworld");var b=node(80,0,true,"minecraft:overworld");var c=node(80,80,true,"minecraft:overworld");
        for(var n:List.of(a,b,c))g.add(n);var ab=g.connect(a.id(),b.id());var bc=g.connect(b.id(),c.id());g.edit(bc.id(),"corner",0,List.of(),false);
        var bend=g.bend(b.id(),ab.id(),bc.id(),true);assertNotNull(bend);
        var s=new FlightState(UUID.randomUUID(),a.dimension(),ab.curve().end);s.route=ab.id();s.fromNode=a.id();s.distance=ab.curve().length()-bend.entry()/2;
        FlightNavigation.prepareChoice(g,s);assertFalse(FlightNavigation.choiceAvailable(g,s,bc.id()));
        for(int i=0;i<4;i++){FlightNavigation.choose(g,s,1);assertNotEquals(bc.id(),s.selectedExit);}
    }
}
