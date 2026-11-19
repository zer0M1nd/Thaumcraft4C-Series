package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightBendTest extends FlightTestConfig {
    @Test void legacyNodeRadiiRemainExplicitForSaveCompatibility(){
        var n=new FlightGraph.Node(UUID.randomUUID(),DIM,BlockPos.ZERO,BlockPos.ZERO.above(2),Direction.EAST,true,"Default");
        assertEquals(3,n.passRadius());assertEquals(3,n.departureRadius());
        var tag=FlightData.node(n);tag.remove("PassRadius");tag.remove("DepartureRadius");
        assertEquals(3,FlightData.node(tag).passRadius());
        tag.putDouble("PassRadius",10);assertEquals(10,FlightData.node(tag).passRadius());
    }
    private static void assertGeometry(FlightGraph g,FlightBend b){
        var in=g.route(b.incoming()).leaving(b.node());var out=g.route(b.outgoing()).leaving(b.node());var c=b.curve();
        assertEquals(0,c.start.distanceTo(b.passing()?in.position(b.entry()):g.node(b.node()).point()),1e-8);
        assertEquals(0,c.end.distanceTo(out.position(b.exit())),1e-8);
        assertEquals(1,c.derivative(0).normalize().dot(in.tangent(b.entry()).scale(-1)),1e-9);
        assertEquals(1,c.derivative(1).normalize().dot(out.tangent(b.exit())),1e-9);
        // Independent dense analytic curvature check, not the production certificate.
        for(int i=0;i<=8192;i++){double t=i/8192d;var v=c.derivative(t);assertTrue(v.length()>1e-7);
            assertTrue(v.cross(c.secondDerivative(t)).length()/Math.pow(v.length(),3)<=1/b.radius()+1e-9);}
    }
    @Test void editedBendsAndCurvedMainRoutesKeepTangencyAndRadius(){
        var g=new FlightGraph();var a=node(0,80,0,3,3);var b=node(80,90,0,3,3);var c=node(0,95,80,3,3);
        for(var n:List.of(a,b,c))g.add(n);var ab=g.connect(a.id(),b.id());var ac=g.connect(a.id(),c.id());
        var bend=g.bend(a.id(),ab.id(),ac.id(),true);var controls=new ArrayList<>(bend.curve().controls);
        controls.set(0,controls.get(0).add(.3,.4,-.2));controls.set(controls.size()-1,controls.get(controls.size()-1).add(-.3,.2,.4));
        g.editBend(bend.id(),"Corrected",14,controls,20);
        assertTrue(g.bend(bend.id()).curve().length()<=20);
        assertGeometry(g,g.bend(bend.id()));
        g.edit(ab.id(),"Curved",0,List.of(a.point().add(20,12,4),b.point().add(-20,3,8)));
        for(var connector:g.bends())assertGeometry(g,connector);
        var old=g.bend(bend.id());long revision=g.revision();
        assertThrows(IllegalArgumentException.class,()->g.editBend(old.id(),"Too short",14,old.curve().controls,.1));
        assertSame(old,g.bend(old.id()));assertEquals(revision,g.revision());
    }
    @Test void ordinaryDepartureConnectorsDoNotExpandIntoHundredsOfBlocks(){
        var g=new FlightGraph();var a=node(0,80,0,3,3);var b=node(64,80,0,3,3);var c=node(32,80,48,3,3);
        for(var n:List.of(a,b,c))g.add(n);g.connect(a.id(),b.id());g.connect(a.id(),c.id());
        for(var bend:g.bends()){
            System.out.println("compact bend passing="+bend.passing()+" self="+bend.incoming().equals(bend.outgoing())+" length="+bend.curve().length());
            assertTrue(bend.curve().length()<=40,"Default connector takes an excessive detour: "+bend.curve().length());
            assertTrue(FlightManeuver.gentle(bend.curve(),bend.radius()));
        }
    }
    static final ResourceLocation DIM=ResourceLocation.parse("minecraft:overworld");
    static FlightGraph.Node node(int x,int y,int z,double pass,double departure){return new FlightGraph.Node(UUID.randomUUID(),DIM,new BlockPos(x,y-2,z),new BlockPos(x,y,z),Direction.EAST,true,"Node",pass,departure);}
    @Test void selfPairsAndDirectedPairsAreCreatedOnceAndPersistExactly(){
        var d=new FlightData();var a=node(0,80,0,3,3);var b=node(80,80,0,3,3);var c=node(0,80,80,3,3);for(var n:List.of(a,b,c))d.graph.add(n);
        var one=d.graph.connect(a.id(),b.id());assertEquals(4,d.graph.bends().size());
        var two=d.graph.connect(a.id(),c.id());assertEquals(11,d.graph.bends().size());
        assertNotNull(d.graph.bend(a.id(),one.id(),one.id(),true));assertNotNull(d.graph.bend(a.id(),one.id(),one.id(),false));
        var pass=d.graph.bend(a.id(),one.id(),two.id(),true);var reverse=d.graph.bend(a.id(),two.id(),one.id(),true);
        assertEquals(pass.id(),reverse.id());assertEquals(pass.curve().start,reverse.curve().end);assertEquals(14,pass.color());
        assertEquals(11,d.graph.bend(a.id(),one.id(),two.id(),false).color());
        var saved=d.save(new CompoundTag());var loaded=FlightData.load(saved);loaded.graph.ensureBends();assertEquals(11,loaded.graph.bends().size());
        assertEquals(saved.getList("Bends",10),loaded.save(new CompoundTag()).getList("Bends",10));
        d.graph.radii(a.id(),0,0);assertSame(pass,d.graph.bend(pass.id()),"Radius changes must not recompute saved bends");
        d.graph.deleteRoute(two.id());assertEquals(4,d.graph.bends().size());
    }
    @Test void shortSelfRoutesAndThreeDimensionalPairsHaveRegularSingleCurves(){
        for(double length:new double[]{1,6,80})for(Vec3 direction:List.of(new Vec3(1,0,0),new Vec3(0,0,1),new Vec3(-1,0,0),new Vec3(0,1,0),new Vec3(1,1,1).normalize()))for(boolean passing:List.of(false,true)){
            var n=node(0,80,0,3,3);var a=new FlightGraph.Route(UUID.randomUUID(),n.id(),UUID.randomUUID(),1,"",0,FlightCurve.straight(n.point(),n.point().add(length,0,0)));
            var b=new FlightGraph.Route(UUID.randomUUID(),n.id(),UUID.randomUUID(),2,"",0,FlightCurve.straight(n.point(),n.point().add(direction.scale(length))));
            FlightBend bend;try{bend=FlightBend.generate(n,a,b,passing);}catch(IllegalArgumentException ex){assertTrue(length<=6,"Long route connector failed: "+direction+" passing="+passing);assertEquals("airship.error.manoeuvre",ex.getMessage());continue;}var curve=bend.curve();double radius=bend.radius();assertTrue(radius>0&&radius<=2);
            assertTrue(curve.controls.size()<=16);assertFalse(bend.pivot());assertTrue(bend.exit()>0);assertTrue(curve.length()<=40,"Length cap applies to generated bends");
            assertEquals(1,curve.tangent(0).dot(a.leaving(n.id()).tangent(bend.entry()).scale(-1)),1e-7);
            assertEquals(1,curve.tangent(curve.length()).dot(b.curve().tangent(bend.exit())),1e-7);
            double step=curve.length()/2048;var previous=curve.tangent(0);
            for(int i=1;i<=2048;i++){var current=curve.tangent(i*step);assertTrue(FlightSteering.angle(previous,current)<=step/radius*1.01+1e-6,"Radius/cusp violation at "+i+" length="+length+" direction="+direction+" passing="+passing);previous=current;}
        }
    }
    @Test void legacyZeroNodeRadiusDoesNotOverrideGenerationAndDegreeElevationRetainsGeometry(){
        var g=new FlightGraph();var a=node(0,80,0,0,0);var b=node(20,80,0,0,0);g.add(a);g.add(b);var r=g.connect(a.id(),b.id());
        for(var bend:g.bends()){assertFalse(bend.pivot());assertTrue(bend.radius()>0&&bend.radius()<=2);assertTrue(bend.curve().length()>0);}
        var c=new FlightCurve(Vec3.ZERO,List.of(new Vec3(2,4,1),new Vec3(5,2,7),new Vec3(-2,3,5),new Vec3(0,1,9)),new Vec3(5,5,5));
        var elevated=c.elevated();assertEquals(c.controls.size()+1,elevated.controls.size());for(int i=0;i<=100;i++)assertEquals(0,c.at(i/100d).distanceTo(elevated.at(i/100d)),1e-8);
        assertEquals(elevated.controls,FlightData.curve(FlightData.curve(elevated)).controls);
    }
    @Test void editsHaveLengthAndOccupancyChecksWithoutPartialMutation(){
        var g=new FlightGraph();var a=node(0,80,0,3,3);var b=node(80,80,0,3,3);g.add(a);g.add(b);var r=g.connect(a.id(),b.id());var bend=g.bend(a.id(),r.id(),r.id(),false);
        assertThrows(IllegalArgumentException.class,()->g.editBend(bend.id(),"Long",11,List.of(a.point().add(0,400,0)),20));assertSame(bend,g.bend(bend.id()));
        g.reserveRoute(r.id(),UUID.randomUUID());assertThrows(IllegalStateException.class,()->g.editBend(bend.id(),"Occupied",11,bend.curve().controls,4096));assertSame(bend,g.bend(bend.id()));
    }
}
