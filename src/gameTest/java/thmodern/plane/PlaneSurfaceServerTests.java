package thmodern.plane;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class PlaneSurfaceServerTests {
 @GameTest(template="empty")
 public static void populationQuotaAndRetrySlotsSurviveSave(GameTestHelper test){
  var ledger=new PlanePopulation();var slot=UUID.randomUUID();ledger.record(7,slot);ledger.record(7,slot);ledger.record(8,slot);
  var restored=PlanePopulation.load(ledger.save(new net.minecraft.nbt.CompoundTag()));test.assertTrue(restored.count(7)==1&&restored.count(8)==1&&restored.contains(7,slot),"Generation slots are unique within each plane and survive save");
  for(int i=1;i<100;i++)restored.record(7,UUID.randomUUID());test.assertTrue(restored.count(7)==100,"Quota remembers generated mobs independently of living entities");test.succeed();
 }
 @GameTest(template="empty")
 public static void surfaceLifeAndColdUsePlannedEnvironment(GameTestHelper test){
  var level=test.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,PocketPlanes.DIMENSION));
  var origin=new PocketPlanes.Origin(test.getLevel().dimension().location(),BlockPos.ZERO);
  var mild=new PocketPlanes.Plane(110000,UUID.randomUUID(),"mild",Map.of("victus",1,"motus",149),0,47,origin,"layers",0,false);var at=mild.center();
  level.setBlockAndUpdate(at,Blocks.DIRT.defaultBlockState());level.setBlockAndUpdate(at.above(),Blocks.AIR.defaultBlockState());
  new PlaneSurfaceWriter(mild).decorate(level,new PlaneStrataWriter.Surface(at,new PlaneLayers.Layer("minecraft:dirt",128,0,1,true),0),0);
  test.assertTrue(level.getBlockState(at).is(Blocks.GRASS_BLOCK),"Low positive life greens soil without requiring high-life vegetation");
  var cold=new PocketPlanes.Plane(110000,mild.allocation(),"cold",Map.of("aqua",100),0,47,origin,"layers",0,false);
  level.setBlockAndUpdate(at,Blocks.WATER.defaultBlockState());new PlaneSurfaceWriter(cold).decorate(level,new PlaneStrataWriter.Surface(at,new PlaneLayers.Layer("minecraft:water",128,0,0,true),0),0);
  test.assertTrue(level.getBlockState(at).is(Blocks.ICE),"Cold planned biome freezes surface water before biome writer runs");
  level.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());test.succeed();
 }
}
