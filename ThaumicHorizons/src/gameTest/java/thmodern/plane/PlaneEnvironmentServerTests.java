package thmodern.plane;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class PlaneEnvironmentServerTests {
 @GameTest(template="empty")
 public static void planeHasLegacyEnvironmentAndRejectsNaturalSnow(GameTestHelper test){
  var level=test.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,PocketPlanes.DIMENSION));
  test.assertTrue(level!=null,"Pocket dimension is registered");
  var type=level.dimensionType();
  test.assertTrue(!type.hasSkyLight()&&type.hasCeiling()&&!type.natural(),"Plane has no natural sky");
  test.assertTrue(type.effectsLocation().equals(PocketPlanes.DIMENSION)&&type.ambientLight()==.3f,"Own sky effects and old ambient light");
  var cold=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.SNOWY_PLAINS).value();
  var pos=new net.minecraft.core.BlockPos(0,127,29000000);
  level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
  test.assertTrue(!cold.shouldFreeze(level,pos,false),"Cold biomes cannot freeze water inside planes");
  level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
  test.assertTrue(!cold.shouldSnow(level,pos.above()),"Cold biomes cannot add snow on solid ground inside planes");
  level.removeBlock(pos,false);
  test.succeed();
 }
}
