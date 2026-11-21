package thaumicomniverse.compat.create.airship;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FlightGraphTest extends FlightTestConfig {
    private FlightGraph.Node node(int x,int y,int z){return new FlightGraph.Node(UUID.randomUUID(),ResourceLocation.parse("minecraft:overworld"),new BlockPos(x,y-2,z),new BlockPos(x,y,z),Direction.NORTH,true,"");}
    @Test void chebyshevBoundaryIncludesDiagonal256AndRejects257(){
        var a=node(0,80,0);assertTrue(FlightGraph.canConnect(a,node(256,336,-256)));
        assertFalse(FlightGraph.canConnect(a,node(257,80,0)));
        var different=new FlightGraph.Node(UUID.randomUUID(),ResourceLocation.parse("minecraft:the_nether"),BlockPos.ZERO,new BlockPos(12,80,12),Direction.SOUTH,true,"Nether");
        assertFalse(FlightGraph.canConnect(a,different));
    }
    @Test void distanceSamplingDoesNotUseBezierParameterAsSpeed(){
        var curve=new FlightCurve(Vec3.ZERO,new Vec3(1,90,0),new Vec3(2,-90,0),new Vec3(180,0,0));
        assertEquals(curve.start,curve.position(0));assertEquals(curve.end,curve.position(curve.length()));
        for(double d=5;d<curve.length()-1;d+=1){
            double travelled=0;
            for(int i=0;i<50;i++)travelled+=curve.position(d+i/50d).distanceTo(curve.position(d+(i+1)/50d));
            assertEquals(1,travelled,.005); // Arc distance; the chord is shorter around a sharp turn.
        }
        var reverse=curve.reverse();
        for(double d=0;d<curve.length();d+=2)assertEquals(0,curve.position(d).distanceTo(reverse.position(reverse.length()-d)),.001);
        assertThrows(IllegalArgumentException.class,()->curve.position(Double.NaN));
    }
    @Test void parallelIdentitiesOccupancyAndEditProtection(){
        var graph=new FlightGraph();var a=node(0,80,0);var b=node(80,90,20);graph.add(a);graph.add(b);
        var one=graph.connect(a.id(),b.id());var two=graph.connect(a.id(),b.id());assertNotEquals(one.id(),two.id());
        assertNotEquals(one.curve().control1,two.curve().control1);
        UUID first=UUID.randomUUID(),second=UUID.randomUUID();assertTrue(graph.reserveRoute(one.id(),first));assertFalse(graph.reserveRoute(one.id(),second));
        assertTrue(graph.reserveRoute(two.id(),second));assertTrue(graph.reserveStation(b.id(),first));assertFalse(graph.reserveStation(b.id(),second));
        assertThrows(IllegalStateException.class,()->graph.edit(one.id(),"new",2,one.curve().control1,one.curve().control2));
        assertEquals(List.of(two.id()),graph.shortestPath(a.id(),b.id(),second));
        graph.release(first);assertTrue(graph.reserveRoute(one.id(),second));assertTrue(graph.reserveStation(b.id(),second));
        assertEquals(Set.of(one.id(),two.id()),graph.removeNode(a.id()));assertTrue(graph.routes().isEmpty());assertNull(graph.routeOwner(one.id()));
    }
    @Test void deletingAnOccupiedRouteReleasesItWithoutDeletingItsNodes(){
        var graph=new FlightGraph();var a=node(0,80,0);var b=node(80,80,0);graph.add(a);graph.add(b);
        var route=graph.connect(a.id(),b.id());graph.reserveRoute(route.id(),UUID.randomUUID());
        graph.deleteRoute(route.id());assertNull(graph.route(route.id()));assertNull(graph.routeOwner(route.id()));
        assertEquals(2,graph.nodes().size());
    }
    @Test void straightestExcludesOnlyTheActualIncomingRouteAndSmoothJoinKeepsTangents(){
        var graph=new FlightGraph();var a=node(-40,80,0);var b=node(0,80,0);var c=node(40,80,0);var d=node(0,80,40);
        for(var n:List.of(a,b,c,d))graph.add(n);
        var incoming=graph.connect(a.id(),b.id());var parallel=graph.connect(a.id(),b.id());var forward=graph.connect(b.id(),c.id());var turn=graph.connect(b.id(),d.id());
        for(var route:List.of(incoming,parallel,forward,turn))graph.edit(route.id(),"",0,List.of(),false);
        incoming=graph.route(incoming.id());parallel=graph.route(parallel.id());forward=graph.route(forward.id());turn=graph.route(turn.id());
        assertTrue(graph.exits(b.id(),incoming.id()).contains(parallel));
        assertEquals(forward,graph.straightest(b.id(),incoming.id(),new Vec3(1,0,0)).orElseThrow());
        var join=FlightCurve.join(incoming.curve(),turn.curve(),6);
        assertEquals(0,join.start.distanceTo(incoming.curve().position(incoming.curve().length()-6)),1e-9);
        assertEquals(1,join.tangent(0).dot(incoming.curve().tangent(incoming.curve().length()-6)),1e-6);
        assertEquals(1,join.tangent(join.length()).dot(turn.curve().tangent(6)),1e-6);
    }
    @Test void malformedInputCannotMutateAnExistingRoute(){
        var graph=new FlightGraph();var a=node(0,80,0);var b=node(10,80,0);graph.add(a);graph.add(b);var route=graph.connect(a.id(),b.id());
        assertThrows(IllegalArgumentException.class,()->graph.edit(route.id(),"x",0,new Vec3(Double.NaN,0,0),Vec3.ZERO));
        assertThrows(IllegalArgumentException.class,()->graph.edit(route.id(),"x",0,new Vec3(100000,0,0),Vec3.ZERO));
        assertThrows(IllegalArgumentException.class,()->graph.renameNode(a.id(),"x".repeat(65)));
        assertSame(route,graph.route(route.id()));
        graph.deleteRoute(route.id());assertEquals(2,graph.connect(a.id(),b.id()).sequence());
    }
}
