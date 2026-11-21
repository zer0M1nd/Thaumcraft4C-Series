package thmodern.plane;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class PlaneEnvelopeServerTests {
 @GameTest(template="empty")
 public static void biomeAndBudgetedBoundaryReachCorrectPositions(GameTestHelper test){
  var level=test.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,PocketPlanes.DIMENSION));
  var plane=new PocketPlanes.Plane(109999,UUID.randomUUID(),"envelope",Map.of("ordo",20),0,7,new PocketPlanes.Origin(test.getLevel().dimension().location(),BlockPos.ZERO),"biomes",0,false);var writer=new PlaneEnvelopeWriter(plane);
  writer.biome(level,0);var sample=plane.center().offset(-32,0,-32);var actual=level.getNoiseBiome(Math.floorDiv(sample.getX(),4),32,Math.floorDiv(sample.getZ(),4));
  test.assertTrue(actual.is(ResourceKey.create(Registries.BIOME,ResourceLocation.parse(plane.parameters().biome()))),"First chunk receives planned biome throughout height");
  long next=writer.boundary(level,0,7);test.assertTrue(next==7,"Boundary respects block budget");
  for(var point:PlaneShell.points(32).subList(0,7))test.assertTrue(level.getBlockState(plane.center().offset(point.x(),point.y(),point.z())).is(PlanarBlocks.VOID.get()),"Checkpoint covers written boundary points");
  test.assertTrue(writer.ringCount()==0&&writer.rings(level,0,4)==0,"No optional aspect shells is a valid empty stage");test.succeed();
 }
}
