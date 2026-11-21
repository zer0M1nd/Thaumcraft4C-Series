package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightPersistenceTest extends FlightTestConfig {
    private FlightGraph.Node node(String dim,int x){return node(dim,x,true);}
    private FlightGraph.Node node(String dim,int x,boolean station){return new FlightGraph.Node(UUID.randomUUID(),ResourceLocation.parse(dim),new BlockPos(x,70,0),new BlockPos(x,72,0),Direction.EAST,station,dim+x);}
    @Test void portalsOfferCrossDimensionAndLocalPaths(){
        var g=new FlightGraph();var a=node("minecraft:overworld",0);var p=node("minecraft:overworld",40,false);var bypass=node("minecraft:overworld",80);var q=node("minecraft:the_nether",5,false);var b=node("minecraft:the_nether",40);
        for(var n:List.of(a,p,bypass,q,b))g.add(n);
        var r1=g.connect(a.id(),p.id());var local=g.connect(p.id(),bypass.id());var r2=g.connect(q.id(),b.id());g.linkPortal(p.id(),q.id());g.linkPortal(q.id(),p.id());
        var ship=UUID.randomUUID();assertEquals(List.of(r1.id(),p.id(),r2.id()),g.shortestPath(a.id(),b.id(),ship));
        // Portal entry is optional: a normal outgoing route remains in this dimension.
        assertEquals(List.of(r1.id(),local.id()),g.shortestPath(a.id(),bypass.id(),ship));
        assertEquals(List.of(r2.id()),g.shortestPath(q.id(),b.id(),ship,q.id()));
        g.unlinkPortal(p.id());assertTrue(g.shortestPath(a.id(),b.id(),ship).isEmpty());
    }
    @Test void passengerTitleAndPortalExitSurviveRoundTrip(){
        var data=new FlightData();var a=node("minecraft:overworld",0,false);var b=node("minecraft:the_nether",0,false);data.graph.add(a);data.graph.add(b);data.graph.linkPortal(a.id(),b.id());
        var s=new FlightState(UUID.randomUUID(),a.dimension(),a.point());s.atNode=a.id();s.portalExitNode=a.id();s.scheduleTitle="Night flight";
        var passenger=new CompoundTag();passenger.putUUID("Player",UUID.randomUUID());passenger.putInt("Seat",2);s.passengers.put("rider",passenger);data.ships.put(s.id,s);
        var loaded=FlightData.load(data.save(new CompoundTag()));var copy=loaded.ships.get(s.id);
        assertEquals(s.passengers,copy.passengers);assertEquals(s.scheduleTitle,copy.scheduleTitle);assertEquals(a.id(),copy.portalExitNode);assertEquals(b.id(),loaded.graph.portalDestination(a.id()));
    }
    @Test void malformedVehicleDoesNotEraseOtherVehiclesOrItsInventory(){
        var data=new FlightData();var good=new FlightState(UUID.randomUUID(),ResourceLocation.parse("minecraft:overworld"),Vec3.ZERO);good.contraption.putString("Cargo","42 diamonds");data.ships.put(good.id,good);
        var root=data.save(new CompoundTag());var broken=FlightData.ship(good);broken.putUUID("Id",UUID.randomUUID());broken.putDouble("Speed",Double.NaN);root.getList("Ships",Tag.TAG_COMPOUND).add(broken);
        var loaded=FlightData.load(root);assertEquals(1,loaded.ships.size());assertEquals("42 diamonds",loaded.ships.get(good.id).contraption.getString("Cargo"));
        assertEquals(broken,loaded.quarantined.getList("Ships",Tag.TAG_COMPOUND).getCompound(0));assertEquals(loaded.quarantined,loaded.save(new CompoundTag()).getCompound("Quarantined"));
    }
    @Test void conflictingStationRestorationStopsSecondShipWithoutLosingCargo(){
        var data=new FlightData();var at=node("minecraft:overworld",0);data.graph.add(at);
        for(int i=0;i<2;i++){var s=new FlightState(UUID.randomUUID(),at.dimension(),at.point());s.atNode=at.id();s.contraption.putInt("Diamonds",42);data.ships.put(s.id,s);}
        var loaded=FlightData.load(data.save(new CompoundTag()));assertEquals(2,loaded.ships.size());assertEquals(1,loaded.ships.values().stream().filter(s->s.paused).count());assertTrue(loaded.ships.values().stream().allMatch(s->s.contraption.getInt("Diamonds")==42));
    }
}
