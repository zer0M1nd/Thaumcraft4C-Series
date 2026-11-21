package thmodern.focus;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class IlluminationServerTests {
 @GameTest(template="empty")
 public static void dyePreservesFocusDataAndUsesLastColor(GameTestHelper test){
  var player=test.makeMockPlayer();var menu=new CraftingMenu(0,player.getInventory());var grid=new TransientCraftingContainer(menu,3,3);
  var source=new ItemStack(Horizons.ILLUMINATION_FOCI.get(11).get());source.setHoverName(net.minecraft.network.chat.Component.literal("Sunlight"));source.getOrCreateTag().putString("upgrade-test","solar");grid.setItem(0,source);grid.setItem(1,new ItemStack(Items.RED_DYE));grid.setItem(8,new ItemStack(Items.BLUE_DYE));
  var recipe=new IlluminationDyeRecipe(new ResourceLocation(Horizons.ID,"illumination_dye"),CraftingBookCategory.MISC);
  test.assertTrue(recipe.matches(grid,test.getLevel()),"One focus and multiple dyes are accepted");var result=recipe.assemble(grid,test.getLevel().registryAccess());
  test.assertTrue(result.is(Horizons.ILLUMINATION_FOCI.get(4).get())&&result.hasCustomHoverName()&&"solar".equals(result.getTag().getString("upgrade-test")),"Last dye wins without losing name or upgrade data");
  test.assertTrue(source.is(Horizons.ILLUMINATION_FOCI.get(11).get())&&source.getCount()==1,"Recipe preview never mutates its source");grid.setItem(2,new ItemStack(Items.STONE));test.assertTrue(!recipe.matches(grid,test.getLevel())&&recipe.assemble(grid,test.getLevel().registryAccess()).isEmpty(),"Foreign ingredients reject recipe");grid.setItem(2,source.copy());test.assertTrue(!recipe.matches(grid,test.getLevel()),"Two foci cannot merge");test.succeed();
 }
 @GameTest(template="empty")
 public static void solarLightAffectsOnlyUndead(GameTestHelper test){
  var pos=test.absolutePos(new net.minecraft.core.BlockPos(2,2,2));var level=test.getLevel();var state=Horizons.SOLAR_LIGHT.get().defaultBlockState().setValue(LightBlock.SHADE,4);level.setBlock(pos,state,3);
  var zombie=EntityType.ZOMBIE.create(level);var cow=EntityType.COW.create(level);state.entityInside(level,pos,zombie);state.entityInside(level,pos,cow);
  test.assertTrue(zombie.getRemainingFireTicks()==100&&!cow.isOnFire(),"Solar light burns undead for five seconds and leaves animals alone");zombie.clearFire();Horizons.LIGHT.get().defaultBlockState().entityInside(level,pos,zombie);test.assertTrue(!zombie.isOnFire(),"Normal light has no solar damage");
  test.assertTrue(state.getLightEmission(level,pos)==14&&state.getCollisionShape(level,pos).isEmpty()&&state.getValue(LightBlock.SHADE)==4,"Colored light keeps brightness and has no collision");test.succeed();
 }
}
