package thmodern.plane;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class PlaneRecipeServerTests {
 @GameTest(template="empty")
 public static void planeInfusionsLoadThroughActualRecipeManager(GameTestHelper test){
  for(String path:new String[]{"keystone","keystone_alternate","keystone_slot"}){var recipe=test.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumichorizons:infusion/"+path));test.assertTrue(recipe.isPresent(),"Infusion loads with registered ingredients and serializer: "+path);var output=recipe.orElseThrow().getResultItem(test.getLevel().registryAccess());test.assertTrue(path.equals("keystone_slot")?output.is(GatewayBlocks.SLOT.get().asItem()):output.is(Horizons.KEYSTONE.get()),"Infusion produces the actual usable item: "+path);}test.succeed();
 }
}
