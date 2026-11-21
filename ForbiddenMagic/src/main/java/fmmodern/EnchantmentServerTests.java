package fmmodern;



import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;


@GameTestHolder("forbidden_magic") @PrefixGameTestTemplate(false)
public class EnchantmentServerTests {
    @GameTest(template="test_empty")
    public static void tableAndAnvilQueriesTerminateAndPreserveEligibility(GameTestHelper test) {
        var wrath = new DarkEnchantments.DarkEnchantment("wrath", 5, EnchantmentCategory.WEAPON);
        for (var item : new net.minecraft.world.item.Item[]{Items.DIAMOND_SWORD, Items.DIAMOND_AXE}) {
            assertTrue(wrath.canEnchant(new ItemStack(item)));
            assertTrue(wrath.canApplyAtEnchantingTable(new ItemStack(item)));
        }
        for (var item : new net.minecraft.world.item.Item[]{Items.BOOK, Items.DIAMOND_PICKAXE, Items.DIRT}) {
            assertFalse(wrath.canEnchant(new ItemStack(item)));
            assertFalse(wrath.canApplyAtEnchantingTable(new ItemStack(item)));
        }
        for (String id : new String[]{"greedy", "educational"}) {
            var enchantment = new DarkEnchantments.DarkEnchantment(id, 1, EnchantmentCategory.WEAPON);
            assertTrue(enchantment.canEnchant(new ItemStack(Items.DIAMOND_SWORD)));
            assertFalse(enchantment.canEnchant(new ItemStack(Items.DIRT)));
            assertFalse(enchantment.canApplyAtEnchantingTable(new ItemStack(Items.DIAMOND_SWORD)));
        }
        test.succeed();
    }
    private static void assertTrue(boolean value) { if (!value) throw new AssertionError("Expected eligible item"); }
    private static void assertFalse(boolean value) { if (value) throw new AssertionError("Expected ineligible item"); }
}
