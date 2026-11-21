package thmodern.plane;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class PlaneUndergroundServerTests {
 @GameTest(template="empty")
 public static void allDepositMaterialsExistAndWrongWorldIsRejected(GameTestHelper test){
  var random=new Random(457);var materials=new HashSet<String>();for(int i=0;i<500;i++){for(var a:PlaneDeposits.POCKETS)materials.add(PlaneDeposits.pocket(a,random));for(var a:PlaneDeposits.CAVES)materials.add(PlaneDeposits.cave(a,random));}
  for(var id:materials)test.assertTrue(ForgeRegistries.BLOCKS.containsKey(ResourceLocation.parse(id)),"Registered environment material: "+id);
  var plane=new PocketPlanes.Plane(0,UUID.randomUUID(),"test",Map.of("terra",40),0,123,new PocketPlanes.Origin(test.getLevel().dimension().location(),BlockPos.ZERO),"caves",0,false);var writer=new PlaneUndergroundWriter(plane);
  boolean rejected=false;try{writer.writePocket(test.getLevel(),0);}catch(IllegalArgumentException expected){rejected=true;}
  test.assertTrue(rejected,"Generator must not replace blocks in the test overworld");test.assertTrue(writer.pocketCount()==163,"Minimum-size realm retains legacy deposit density");test.succeed();
 }
}
