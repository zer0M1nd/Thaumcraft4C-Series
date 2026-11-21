package tc4revival.content;

import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class MetalAlchemyRecipeServerTests {
    @GameTest(template="wand_test_empty")
    public static void sixMetalRecipesPreserveCatalystsCostsAndOutputs(GameTestHelper test) {
        String[] metals={"iron","gold","copper"};
        Item[] ores={Items.IRON_ORE,Items.GOLD_ORE,Items.COPPER_ORE};
        Item[] deep={Items.DEEPSLATE_IRON_ORE,Items.DEEPSLATE_GOLD_ORE,Items.DEEPSLATE_COPPER_ORE};
        Item[] raw={Items.RAW_IRON,Items.RAW_GOLD,Items.RAW_COPPER};
        Item[] nuggets={Items.IRON_NUGGET,Items.GOLD_NUGGET,TCItems.nugget(NuggetType.COPPER)};
        ClusterType[] clusters={ClusterType.IRON,ClusterType.GOLD,ClusterType.COPPER};
        for(int i=0;i<3;i++) {
            var cost=new AspectAmounts().add(TCAspects.catalog().require("metallum"),2).add(TCAspects.catalog().require("ordo"),2);
            var recipe=recipe(test,"pure_"+metals[i]);
            check(test,recipe,"PURE"+metals[i].toUpperCase(java.util.Locale.ROOT),new ItemStack(ores[i]),cost,new ItemStack(TCItems.cluster(clusters[i]),2));
            test.assertTrue(recipe.catalystMatches(new ItemStack(deep[i])) && !recipe.catalystMatches(new ItemStack(raw[i])) && !recipe.catalystMatches(new ItemStack(ores[(i+1)%3])),"Ore tag accepts deepslate but excludes raw and other metal");
            cost=new AspectAmounts().add(TCAspects.catalog().require("metallum"),2);
            if(i>0)cost.add(TCAspects.catalog().require(i==1?"lucrum":"permutatio"),1);
            recipe=recipe(test,"transmute_"+metals[i]);
            check(test,recipe,"TRANS"+metals[i].toUpperCase(java.util.Locale.ROOT),new ItemStack(nuggets[i]),cost,new ItemStack(nuggets[i],3));
            test.assertTrue(!recipe.catalystMatches(new ItemStack(nuggets[(i+1)%3])),"Metal transmutation cannot cross material subtypes");
        }
        test.assertTrue(!recipe(test,"transmute_gold").catalystMatches(new ItemStack(TCItems.material(MaterialType.GOLD_COIN))),"Direct gold-nugget catalyst rejects tagged-equivalent coin");
        tc4revival.TC4Revival.LOGGER.info("PASS sixMetalRecipesPreserveCatalystsCostsAndOutputs: six loaded recipes, exact costs/outputs/research and catalyst boundaries");test.succeed();
    }
    private static CrucibleRecipe recipe(GameTestHelper test,String path) {
        return (CrucibleRecipe)test.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:crucible/"+path)).orElseThrow();
    }
    private static void check(GameTestHelper test,CrucibleRecipe recipe,String research,ItemStack catalyst,AspectAmounts cost,ItemStack output) {
        test.assertTrue(recipe.research().equals(research) && recipe.costs().view().equals(cost.view()),"Exact old research/cost: "+recipe.getId());
        var input=new CrucibleRecipe.Input(catalyst,cost,key->key.equals(research));
        test.assertTrue(recipe.matches(input,test.getLevel()) && ItemStack.matches(recipe.assemble(input,test.getLevel().registryAccess()),output),"Exact loaded recipe output");
        test.assertTrue(!recipe.matches(new CrucibleRecipe.Input(catalyst,cost,key->false),test.getLevel()),"Research gate");
        for(var aspect:cost.aspects()) {
            var insufficient=cost.copy();insufficient.remove(aspect,1);
            test.assertTrue(!recipe.matches(new CrucibleRecipe.Input(catalyst,insufficient,key->true),test.getLevel()) && recipe.remainder(insufficient).isEmpty(),"Every aspect charged");
        }
        var surplus=cost.copy().add(TCAspects.catalog().require("aer"),7);
        var remaining=recipe.remainder(surplus).orElseThrow();
        test.assertTrue(remaining.size()==1 && remaining.amount(TCAspects.catalog().require("aer"))==7 && catalyst.getCount()==1,"Exact debit preserves unrelated contents and query inventory");
    }
}
