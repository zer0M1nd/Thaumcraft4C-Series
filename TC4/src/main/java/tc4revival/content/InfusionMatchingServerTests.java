package tc4revival.content;

import com.google.gson.JsonObject;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Exercises generated category tags at the eventual dedicated-server checkpoint. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class InfusionMatchingServerTests {
    private InfusionMatchingServerTests() {}
    @GameTest(template="wand_test_empty")
    public static void explicitCenterIdentityDoesNotBroadenToEveryLog(GameTestHelper test) {
        var json=com.google.gson.JsonParser.parseString("{\"ingredient\":{\"item\":\"minecraft:oak_log\"},\"exact\":true}").getAsJsonObject();
        var exact=new InfusionIngredient(json);
        test.assertTrue(exact.matches(new ItemStack(Items.OAK_LOG)),"Named item remains accepted");
        test.assertTrue(!exact.matches(new ItemStack(Items.BIRCH_LOG)),"Distinct log cannot replace an explicitly exact center");
        test.assertTrue(ingredient("minecraft:oak_log",null,null).matches(new ItemStack(Items.BIRCH_LOG)),"Existing category recipes retain their declared behavior");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void modernCategoriesAreSymmetricAndIncludeNewVariants(GameTestHelper test) {
        test.assertTrue(LegacyCraftingCategories.matches(new ItemStack(Items.RED_STAINED_GLASS),new ItemStack(Items.GLASS)),"Stained glass selects the general glass category");
        test.assertTrue(LegacyCraftingCategories.matches(new ItemStack(Items.GLASS),new ItemStack(Items.RED_STAINED_GLASS)),"Modern Forge glass equivalence is symmetric");
        test.assertTrue(!LegacyCraftingCategories.matches(new ItemStack(Items.LAPIS_LAZULI),new ItemStack(Items.BONE_MEAL)),"Modern gems and bone meal are not dyes");
        test.assertTrue(!LegacyCraftingCategories.matches(new ItemStack(Items.BLUE_DYE),new ItemStack(Items.LAPIS_LAZULI)),"New blue dye is not silently treated as an old dye metadata variant");
        test.assertTrue(LegacyCraftingCategories.matches(new ItemStack(Items.OAK_LOG),new ItemStack(Items.BIRCH_WOOD)),"Old log variants share their category");
        test.assertTrue(LegacyCraftingCategories.matches(new ItemStack(Items.MANGROVE_LOG),new ItemStack(Items.OAK_LOG)),"New wood follows the standard Minecraft logs tag");
        test.assertTrue(!LegacyCraftingCategories.matches(new ItemStack(Items.IRON_INGOT),new ItemStack(Items.GOLD_INGOT)),"The broad modern ingots tag never makes different metals interchangeable"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void recipeAndOngoingInfusionUseDifferentTagOrdering(GameTestHelper test) {
        var coin=new ItemStack(TCItems.material(MaterialType.GOLD_COIN));
        var gold=new ItemStack(Items.GOLD_NUGGET);
        test.assertTrue(ingredient("minecraft:gold_nugget",null,null).matches(coin),"Old gold coins can substitute for gold nuggets");
        var expected=gold.copy(); expected.getOrCreateTag().putString("owner","original");
        test.assertTrue(!ingredient("minecraft:gold_nugget","{owner:\"original\"}",null).matches(coin),"Recipe lookup checks required NBT before ore equivalence");
        test.assertTrue(!InfusionIngredient.centerMatches(coin,expected),"Running infusion cannot swap out the selected center through an equivalent material tag");
        var drop=new ItemStack(TCItems.nugget(NuggetType.QUICKSILVER));
        test.assertTrue(!LegacyCraftingCategories.matches(drop,new ItemStack(TCItems.material(MaterialType.QUICKSILVER))),"Quicksilver drops were not registered as whole quicksilver nuggets"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void toolWearIsExactAtStartAndIgnoredOnlyDuringInfusion(GameTestHelper test) {
        var fresh=new ItemStack(Items.IRON_PICKAXE); var worn=fresh.copy(); worn.setDamageValue(7);
        test.assertTrue(!ingredient("minecraft:iron_pickaxe",null,null).matches(worn),"Default recipe metadata is zero, not arbitrary durability");
        test.assertTrue(ingredient("minecraft:iron_pickaxe",null,32767).matches(worn),"An explicit legacy wildcard accepts tool wear");
        test.assertTrue(InfusionIngredient.centerMatches(worn,fresh),"Ongoing center matching ignores modern Damage NBT");
        fresh.getOrCreateTag().putString("owner","original");
        test.assertTrue(!InfusionIngredient.centerMatches(worn,fresh),"Ignoring wear does not discard other required tags");
        worn.getOrCreateTag().putString("owner","original"); worn.getOrCreateTag().putInt("extra",1);
        test.assertTrue(InfusionIngredient.centerMatches(worn,fresh),"Extra top-level NBT is allowed on an otherwise matching center"); test.succeed();
    }
    private static InfusionIngredient ingredient(String id,String nbt,Integer damage) {
        var item=new JsonObject(); item.addProperty("item",id);
        var json=new JsonObject(); json.add("ingredient",item);
        if (nbt!=null) json.addProperty("nbt",nbt);
        if (damage!=null) json.addProperty("damage",damage);
        return new InfusionIngredient(json);
    }
}
