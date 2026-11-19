package emtmodern.test;

import emtmodern.item.LegacyResearchConfig;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraftforge.gametest.*;
import thaumcraft.api.research.ResearchCategories;

@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class ResearchConfigServerTests {
    public static CraftingContainer grid(){return new TransientCraftingContainer(new AbstractContainerMenu(null,0){public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}public boolean stillValid(Player p){return true;}},3,3);}
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void uraniumAndPlateCraftingPreserveOriginalCountsAndHammerWear(GameTestHelper test){
        var level=test.getLevel();var grid=grid();var uranium=(tc4revival.content.ArcaneRecipe)level.getRecipeManager().byKey(new ResourceLocation("emt","tiny_uranium")).orElseThrow();grid.setItem(4,MaterialServerTests.stack("ic2:uranium_238"));
        test.assertTrue(uranium.matches(grid,level)&&uranium.assemble(grid,level.registryAccess()).getCount()==7&&uranium.visCost(grid).size()==6&&uranium.visCost(grid).values().stream().allMatch(n->n==1),"Original uranium exchange uses one U238, seven small U235, and one of each primal");
        grid.clearContent();var hammer=MaterialServerTests.stack("ic2:forge_hammer");hammer.setDamageValue(5);grid.setItem(1,hammer);grid.setItem(4,MaterialServerTests.stack("thaumcraft:thaumium_ingot"));grid.setItem(7,MaterialServerTests.stack("minecraft:obsidian"));
        var plate=(CraftingRecipe)level.getRecipeManager().byKey(new ResourceLocation("emt","thaumium_plate")).orElseThrow();test.assertTrue(plate.matches(grid,level)&&plate.assemble(grid,level.registryAccess()).is(MaterialServerTests.stack("emt:thaumium_plate").getItem()),"Original vertical hammer/ingot/obsidian arrangement crafts plate");test.assertTrue(plate.getRemainingItems(grid).get(1).getDamageValue()==6&&plate.getRemainingItems(grid).get(4).isEmpty()&&plate.getRemainingItems(grid).get(7).isEmpty(),"Hammer survives with one wear; ingot and obsidian consumed");
        hammer.setDamageValue(hammer.getMaxDamage()-1);test.assertTrue(plate.getRemainingItems(grid).get(1).isEmpty(),"Last hammer use breaks the hammer");test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void researchDisableAlsoRejectsActualRecipeAndPropagatesOriginalDependencies(GameTestHelper test){
        var tiny=LegacyResearchConfig.DISABLED.get("tiny_uranium");var wings=LegacyResearchConfig.DISABLED.get("thaumium_wings");var solar=LegacyResearchConfig.DISABLED.get("compressed_solar");var drill=LegacyResearchConfig.DISABLED.get("thaumium_drill");var omni=LegacyResearchConfig.DISABLED.get("thaumium_omnitool");
        boolean oldTiny=tiny.get(),oldWings=wings.get(),oldSolar=solar.get(),oldDrill=drill.get(),oldOmni=omni.get();
        try {
            tiny.set(false);var recipe=(tc4revival.content.ArcaneRecipe)test.getLevel().getRecipeManager().byKey(new ResourceLocation("emt","tiny_uranium")).orElseThrow();var grid=grid();grid.setItem(0,MaterialServerTests.stack("ic2:uranium_238"));test.assertTrue(recipe.matches(grid,test.getLevel()),"Recipe initially craftable");tiny.set(true);test.assertTrue(!ResearchCategories.enabled("emt:tiny_uranium")&&!recipe.matches(grid,test.getLevel()),"Disabling research rejects real recipe even if knowledge already exists");
            wings.set(true);for(String id:new String[]{"thaumium_wings","nano_wings","quantum_wings","infused_quantum_chestplate"})test.assertTrue(!ResearchCategories.enabled("emt:"+id),"Original wing prerequisite switch disables "+id);
            solar.set(true);for(String id:new String[]{"compressed_solar","water_solar","dark_solar","order_solar","fire_solar","air_solar","earth_solar","solar_goggles"})test.assertTrue(!ResearchCategories.enabled("emt:"+id),"Original compressed-solar switch disables "+id);
            drill.set(true);omni.set(false);test.assertTrue(ResearchCategories.enabled("emt:thaumium_drill"),"Original overwritten drill setting retained");omni.set(true);test.assertTrue(!ResearchCategories.enabled("emt:thaumium_drill")&&!ResearchCategories.enabled("emt:rockbreaker_drill"),"Original effective omn itool setting gates drill family");
        } finally {tiny.set(oldTiny);wings.set(oldWings);solar.set(oldSolar);drill.set(oldDrill);omni.set(oldOmni);}test.succeed();
    }
}
