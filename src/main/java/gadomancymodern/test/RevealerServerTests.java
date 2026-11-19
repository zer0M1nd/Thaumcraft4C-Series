package gadomancymodern.test;
import gadomancymodern.Gadomancy;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("gadomancy") @PrefixGameTestTemplate(false)
public class RevealerServerTests {
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void revealerHelmetApplicabilityIsIndependentOfEnchantingTable(GameTestHelper test){
        var enchantment=Gadomancy.REVEALER.get();
        for(var item:java.util.List.of(Items.LEATHER_HELMET,Items.DIAMOND_HELMET,Items.NETHERITE_HELMET)){
            var helmet=new ItemStack(item);
            test.assertTrue(enchantment.canEnchant(helmet),"Actual helmet category remains eligible for infusion");
            test.assertTrue(!enchantment.canApplyAtEnchantingTable(helmet),"Infusion eligibility does not expose the enchantment at the table");
        }
        test.assertTrue(!enchantment.canEnchant(ItemStack.EMPTY)&&!enchantment.canEnchant(new ItemStack(Items.DIAMOND_CHESTPLATE))&&!enchantment.canEnchant(new ItemStack(Items.DIAMOND_BOOTS)),"Non-helmet categories remain ineligible");
        test.succeed();
    }
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void infusionEnchantmentRequiresResearchAndCannotComeFromTable(GameTestHelper test) {
        // This audit deliberately exercises the legacy XP-based enchanting profile.
        // Keep the pinned core API compatible; older cores only have the legacy profile.
        net.minecraftforge.common.ForgeConfigSpec.BooleanValue option;
        try { option=(net.minecraftforge.common.ForgeConfigSpec.BooleanValue)tc4revival.config.TCConfig.SERVER.getClass().getField("enhancedInfusionEnchanting").get(tc4revival.config.TCConfig.SERVER); }
        catch (NoSuchFieldException absent) { infusionEnchantmentRequiresResearchAndCannotComeFromTableLegacy(test); return; }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot select legacy enchanting test profile",failure); }
        boolean previous=option.get();
        try {option.set(false);infusionEnchantmentRequiresResearchAndCannotComeFromTableLegacy(test);} finally {option.set(previous);}
    }
    private static void infusionEnchantmentRequiresResearchAndCannotComeFromTableLegacy(GameTestHelper test) {var recipe=(tc4revival.content.InfusionRecipe)test.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("gadomancy:revealer")).orElseThrow();var helmet=new ItemStack(Items.DIAMOND_HELMET);var components=java.util.List.of(new ItemStack(tc4revival.content.TCItems.GOGGLES.get()),new ItemStack(tc4revival.content.TCItems.material(tc4revival.content.MaterialType.SALIS_MUNDUS)),new ItemStack(tc4revival.content.TCItems.material(tc4revival.content.MaterialType.SALIS_MUNDUS)));test.assertTrue(!Gadomancy.REVEALER.get().canApplyAtEnchantingTable(helmet)&&Gadomancy.REVEALER.get().getMaxLevel()==1,"Original infusion-only single level enchantment");test.assertTrue(recipe.acceptsCenter(helmet),"Loaded infusion recipe accepts the same eligible helmet");
        var diagnosticPlan=recipe.plan(helmet);
        test.assertTrue(diagnosticPlan.components().size()==components.size(),"Legacy component count: "+diagnosticPlan.components().size()+" xp="+diagnosticPlan.xp());
        for(int i=0;i<components.size();i++)test.assertTrue(diagnosticPlan.components().get(i).matches(components.get(i)),"Legacy component match index="+i+" offered="+components.get(i)+" expected="+diagnosticPlan.components().get(i).displayStacks());
        test.assertTrue(!recipe.matches(new tc4revival.content.InfusionRecipe.Input(helmet,components,key->false),test.getLevel())&&recipe.matches(new tc4revival.content.InfusionRecipe.Input(helmet,components,key->true),test.getLevel()),"Real research gate and helmet recipe");var plan=recipe.plan(helmet);test.assertTrue(plan.xp()==5&&plan.instability()==4&&plan.aspects().amount(tc4revival.aspect.TCAspects.catalog().require("sensus"))==16,"Legacy XP and vis cost");var enchanted=plan.output().apply(helmet);test.assertTrue(enchanted.getEnchantmentLevel(Gadomancy.REVEALER.get())==1&&!recipe.acceptsCenter(enchanted)&&!recipe.acceptsCenter(new ItemStack(Items.DIAMOND_CHESTPLATE)),"Output level, maximum and slot constraints");var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(test.getLevel());var saved=player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);try{player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,enchanted);test.assertTrue(tc4revival.content.RevealingGear.visible(player,true)&&tc4revival.content.RevealingGear.visible(player,false),"Enchanted ordinary helmet must expose both nodes and HUD through core provider");}finally{player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,saved);}test.succeed();}
}
