package tc4revival.content;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Combined equipment regressions for the deferred server checkpoint. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class VoidEquipmentServerTests {
    private VoidEquipmentServerTests() {}
    @GameTest(template="wand_test_empty")
    public static void repairCadenceDoesNotDoubleTickEquippedArmor(GameTestHelper test) {
        var player = test.makeMockPlayer(); player.getAbilities().instabuild=false;
        var level=test.getLevel();
        var sword=new ItemStack(TCItems.voidTool(VoidEquipment.Tool.SWORD)); sword.setDamageValue(8);
        player.tickCount=19; sword.getItem().inventoryTick(sword,level,player,0,false);
        test.assertTrue(sword.getDamageValue()==8,"Healing waits for the holder's twentieth tick");
        player.tickCount=20; sword.getItem().inventoryTick(sword,level,player,0,false);
        test.assertTrue(sword.getDamageValue()==7,"An unselected inventory tool also heals");
        var armor=new ItemStack(TCItems.voidArmor(VoidEquipment.Armor.CHESTPLATE)); armor.setDamageValue(8);
        player.setItemSlot(EquipmentSlot.CHEST,armor);
        armor.getItem().inventoryTick(armor,level,player,0,false);
        armor.getItem().onArmorTick(armor,level,player);
        test.assertTrue(armor.getDamageValue()==7,"Modern inventory and Forge armor callbacks must repair only once together");
        player.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
        armor.getItem().inventoryTick(armor,level,player,1,false);
        test.assertTrue(armor.getDamageValue()==6,"Unequipped armor still repairs in inventory");
        var charm=new ItemStack(TCItems.PRIMAL_CHARM.get()); var ingot=new ItemStack(TCItems.material(MaterialType.VOID));
        test.assertTrue(!sword.getItem().isValidRepairItem(sword,charm) && sword.getItem().isValidRepairItem(sword,ingot),"Void tools repair with void metal");
        test.assertTrue(armor.getItem().isValidRepairItem(armor,ingot) && !armor.getItem().isValidRepairItem(armor,charm),"Armor uses void metal instead");
        tc4revival.TC4Revival.LOGGER.info("PASS repairCadenceDoesNotDoubleTickEquippedArmor");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void toolsWeakenOnClickButSwordRequiresItsHitCallback(GameTestHelper test) {
        var player=test.makeMockPlayer(); player.getAbilities().instabuild=false;
        var target=EntityType.ZOMBIE.create(test.getLevel());
        if (target==null) throw new AssertionError("Unable to create fixture");
        for (var type:VoidEquipment.Tool.values()) {
            target.removeEffect(MobEffects.WEAKNESS);
            var stack=new ItemStack(TCItems.voidTool(type));
            test.assertTrue(!stack.getItem().onLeftClickEntity(stack,player,target),"The weakness hook must not consume the ordinary attack");
            if (type==VoidEquipment.Tool.SWORD) {
                test.assertTrue(!target.hasEffect(MobEffects.WEAKNESS),"Sword left click alone must not apply weakness");
                stack.getItem().hurtEnemy(stack,target,player);
                test.assertTrue(target.getEffect(MobEffects.WEAKNESS).getDuration()==60 && stack.getDamageValue()==1,"Successful sword hits apply sixty ticks and one durability wear");
            } else {
                test.assertTrue(target.getEffect(MobEffects.WEAKNESS).getDuration()==80 && stack.getDamageValue()==0,"Tools apply eighty ticks before normal attack handling without extra wear");
            }
        }
        tc4revival.TC4Revival.LOGGER.info("PASS toolsWeakenOnClickButSwordRequiresItsHitCallback");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void ringDiscountAndRunicHardeningComposeWithoutAddingWarp(GameTestHelper test) {
        var air=TCItems.elementalRing(ElementalRingType.AIR); var earth=TCItems.elementalRing(ElementalRingType.EARTH);
        var airStack=new ItemStack(air); var earthStack=new ItemStack(earth);
        airStack.getOrCreateTag().putByte("RS.HARDEN",(byte)2); earthStack.getOrCreateTag().putByte("RS.HARDEN",(byte)1);
        var armor=new ItemStack(TCItems.voidArmor(VoidEquipment.Armor.HELMET)); armor.getOrCreateTag().putByte("RS.HARDEN",(byte)3);
        var equipment=RunicShieldEvents.equipment(List.of(armor),List.of(airStack,earthStack));
        test.assertTrue(equipment.maximum()==6,"Zero intrinsic charge does not prevent hardening on rings or void armor");
        test.assertTrue(air.visDiscount(airStack,null,ElementalRingType.AIR.aspect())==1 && earth.visDiscount(earthStack,null,ElementalRingType.AIR.aspect())==0,"Only the matching primal receives a percentage point");
        test.assertTrue(WarpingGear.contribution(airStack,null)==0 && WarpingGear.contribution(armor,null)==1,"Hardening and vis discounts do not manufacture equipment warp");
        test.assertTrue(ElementalRingType.EARTH.metadata()==4 && ElementalRingType.FIRE.metadata()==5,"Loot metadata follows primal order, not shard order");
        tc4revival.TC4Revival.LOGGER.info("PASS ringDiscountAndRunicHardeningComposeWithoutAddingWarp");test.succeed();
    }
}
