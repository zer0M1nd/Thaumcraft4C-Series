package thmodern.plane;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class GatewayLinksServerTests {
 @GameTest(template="empty")
 public static void fourExclusiveSidesPersistAndStaleReleaseCannotRemoveReplacement(GameTestHelper test){
  var dimension=test.getLevel().dimension().location();var plane=new PocketPlanes.Plane(0,UUID.randomUUID(),"Gateway test",Map.of(),0,1,new PocketPlanes.Origin(dimension,BlockPos.ZERO),"ready",0,true);var data=new GatewayLinks();var ids=new ArrayList<UUID>();
  for(int side=0;side<4;side++){var request=UUID.randomUUID();ids.add(request);var link=data.reserve(request,plane,dimension,new BlockPos(side,10,0),true).orElseThrow();test.assertTrue(link.side()==side,"First available sides allocate in legacy order");test.assertTrue(data.reserve(request,plane,dimension,link.slot(),true).orElseThrow().id().equals(request),"Same request retries idempotently");}
  test.assertTrue(data.reserve(UUID.randomUUID(),plane,dimension,new BlockPos(8,10,0),true).isEmpty(),"Fifth gateway is rejected");data=GatewayLinks.load(data.save(new net.minecraft.nbt.CompoundTag()));test.assertTrue(data.release(ids.get(1)),"Existing binding can release");var replacement=data.reserve(UUID.randomUUID(),plane,dimension,new BlockPos(9,10,0),false).orElseThrow();test.assertTrue(replacement.side()==1&&!data.release(ids.get(1))&&data.get(replacement.id()).isPresent(),"Reused side survives old holder's later close");test.succeed();
 }
 @GameTest(template="empty")
 public static void layoutHasLegacyCornersFrameAndNineBlockMouth(GameTestHelper test){
  var cells=GatewayLayout.cells();test.assertTrue(cells.size()==24&&cells.stream().filter(c->c.material().equals("minecraft:air")).count()==9&&cells.stream().filter(c->c.material().contains("advanced_alchemical_construct")).count()==4,"5x5 ring excludes slot and preserves 4 corners/9 opening cells");
  var dimension=test.getLevel().dimension().location();var plane=new PocketPlanes.Plane(2,UUID.randomUUID(),"geometry",Map.of(),0,1,new PocketPlanes.Origin(dimension,BlockPos.ZERO),"ready",0,true);
  for(int side=0;side<4;side++){var mouth=GatewayLayout.mouth(plane,side);test.assertTrue(mouth.size()==9&&new HashSet<>(mouth).size()==9&&mouth.stream().allMatch(p->p.getY()>=126&&p.getY()<=128),"All four mouths preserve width/height and Y126..128");}test.succeed();
 }
}
