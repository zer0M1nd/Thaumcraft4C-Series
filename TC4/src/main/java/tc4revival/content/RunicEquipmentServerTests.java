package tc4revival.content;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Uses actual registered accessories and generated recipes at the eventual server checkpoint. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class RunicEquipmentServerTests {
    private RunicEquipmentServerTests() {}
    @GameTest(template="wand_test_empty")
    public static void intrinsicChargesHardeningAndUpgradeCountsCompose(GameTestHelper test) {
        var amulet=new ItemStack(TCItems.runicBauble(RunicBaubleType.EMERGENCY_AMULET));
        var charged=new ItemStack(TCItems.runicBauble(RunicBaubleType.CHARGED_RING)); charged.getOrCreateTag().putByte("RS.HARDEN",(byte)2);
        var healing=new ItemStack(TCItems.runicBauble(RunicBaubleType.HEALING_RING)); var belt=new ItemStack(TCItems.runicBauble(RunicBaubleType.KINETIC_GIRDLE));
        var goggles=new ItemStack(TCItems.GOGGLES.get()); goggles.getOrCreateTag().putByte("RS.HARDEN",(byte)3);
        var equipment=RunicShieldEvents.equipment(List.of(goggles),List.of(amulet,charged,healing,belt));
        test.assertTrue(equipment.maximum()==29 && equipment.charged()==1 && equipment.healing()==1 && equipment.kinetic()==1 && equipment.emergency()==1,"Intrinsic charges plus hardening determine capacity; upgrade counts are per accessory");
        var pair=RunicShieldEvents.equipment(List.of(),List.of(charged,charged.copy()));
        test.assertTrue(pair.maximum()==12 && pair.charged()==2,"Two charged rings contribute two recharge bonuses, independent of hardening");
        var plan=RunicAugmentation.plan(new ItemStack(TCItems.runicBauble(RunicBaubleType.RING)));
        test.assertTrue(plan.components().size()==7 && plan.aspects().amount(tc4revival.aspect.TCAspects.catalog().require("potentia"))==1024,"Dynamic augmentation uses intrinsic charge five even with no hardening tag"); tc4revival.TC4Revival.LOGGER.info("PASS intrinsicChargesHardeningAndUpgradeCountsCompose"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void chargedRingRecipeRequiresStrongSpeedAndProducesLegacyFreshOutput(GameTestHelper test) {
        var id=net.minecraft.resources.ResourceLocation.parse("thaumcraft:infusion/runic_ring_charged");
        var found=test.getLevel().getRecipeManager().byKey(id).orElseThrow(()->new AssertionError("Generate the registered runic recipe before this checkpoint"));
        test.assertTrue(found instanceof InfusionRecipe,"Runic upgrade is a real infusion recipe"); var recipe=(InfusionRecipe)found;
        var center=new ItemStack(TCItems.runicBauble(RunicBaubleType.RING)); center.getOrCreateTag().putByte("RS.HARDEN",(byte)2);
        var shard=new ItemStack(TCItems.shard(ShardType.FIRE));
        var parts=new java.util.ArrayList<>(List.of(new ItemStack(TCItems.shard(ShardType.BALANCED)),shard.copy(),shard.copy(),PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.STRONG_SWIFTNESS),shard.copy(),shard.copy()));
        var input=new InfusionRecipe.Input(center,parts,key->true);
        test.assertTrue(recipe.matches(input,test.getLevel()),"The original strong swiftness potion must match the modern NBT ingredient");
        var result=recipe.assemble(input,test.getLevel().registryAccess());
        test.assertTrue(result.is(TCItems.runicBauble(RunicBaubleType.CHARGED_RING)) && RunicGear.charge(result)==4 && !result.hasTag(),"Static accessory upgrades produce a fresh item and do not preserve prior hardening");
        parts.set(3,PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.SWIFTNESS));
        test.assertTrue(!recipe.matches(new InfusionRecipe.Input(center,parts,key->true),test.getLevel()),"Ordinary speed cannot substitute for the strong potion"); tc4revival.TC4Revival.LOGGER.info("PASS chargedRingRecipeRequiresStrongSpeedAndProducesLegacyFreshOutput"); test.succeed();
    }
}
