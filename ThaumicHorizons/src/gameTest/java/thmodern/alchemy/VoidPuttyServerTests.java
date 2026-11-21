package thmodern.alchemy;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class VoidPuttyServerTests {
 @GameTest(template="empty")
 public static void repairPreservesEnchantmentsAndRejectsInvalidGrids(GameTestHelper test){
  var player=test.makeMockPlayer();var grid=new TransientCraftingContainer(new CraftingMenu(0,player.getInventory()),3,3);var recipe=new VoidPuttyRepairRecipe(net.minecraft.resources.ResourceLocation.parse("thaumichorizons:void_putty_repair"),CraftingBookCategory.EQUIPMENT);var sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(100);sword.setHoverName(net.minecraft.network.chat.Component.literal("Keep name"));sword.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,3);sword.getOrCreateTag().putInt("RepairCost",12);grid.setItem(0,sword);
  test.assertTrue(!recipe.matches(grid,test.getLevel())&&recipe.assemble(grid,test.getLevel().registryAccess()).isEmpty(),"No free repair without putty");grid.setItem(1,new ItemStack(Horizons.VOID_PUTTY.get()));test.assertTrue(recipe.matches(grid,test.getLevel()),"Damaged repairable tool and putty form valid recipe");var result=recipe.assemble(grid,test.getLevel().registryAccess());test.assertTrue(result.getDamageValue()==0&&result.hasCustomHoverName()&&result.getTag().getInt("RepairCost")==12&&net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,result)==3&&sword.getDamageValue()==100,"Repair preserves all metadata and leaves preview input unchanged");grid.setItem(2,sword.copy());test.assertTrue(!recipe.matches(grid,test.getLevel()),"Cannot repair multiple tools in one operation");grid.setItem(2,ItemStack.EMPTY);grid.setItem(0,result);test.assertTrue(!recipe.matches(grid,test.getLevel()),"Undamaged item is not repaired");grid.setItem(0,ItemStack.EMPTY);test.assertTrue(recipe.assemble(grid,test.getLevel().registryAccess()).isEmpty(),"Putty-only preview is safe");test.succeed();
 }
}
