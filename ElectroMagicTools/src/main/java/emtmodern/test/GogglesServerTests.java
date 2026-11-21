package emtmodern.test;

import emtmodern.ElectroMagicTools;
import emtmodern.item.ElectricGoggles;
import ic2.api.item.ElectricItem;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class GogglesServerTests {
    public static net.minecraftforge.common.util.FakePlayer player(GameTestHelper test){return new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"EMTArmor"));}
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void revealingDiscountAndIndustrialCraftDamage(GameTestHelper test){
        var player=player(test);int[] discount={5,6,8,7};String[] kinds={"electric","nano","quantum","solar"};
        for(int i=0;i<kinds.length;i++){
            var stack=new ItemStack(ElectroMagicTools.CONTENT.get(kinds[i]+"_goggles").get());var item=(ElectricGoggles)stack.getItem();player.setItemSlot(EquipmentSlot.HEAD,stack);
            test.assertTrue(tc4revival.content.RevealingGear.visible(player,true)&&tc4revival.content.RevealingGear.visible(player,false),"Both goggles API channels reveal with empty charge");
            test.assertTrue(item.getVisDiscount(stack,player,thaumcraft.api.aspects.Aspect.AIR)==discount[i],"Original casting discount");
            ElectricItem.manager.charge(stack,item.getMaxCharge(stack),Integer.MAX_VALUE,true,false);double before=ElectricItem.manager.getCharge(stack);
            float remaining=ic2.core.item.armor.ItemArmorElectric.damageArmor(player,player.damageSources().generic(),20);
            test.assertTrue(Math.abs(remaining-16.7)<.001,"IC2 damage pipeline applies 16.5 percent protection");
            test.assertTrue(Math.abs(ElectricItem.manager.getCharge(stack)-(before-3.3*item.getEnergyPerDamage()))<.01,"Absorbed damage debits exact IC2 EU");
        }test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void quantumLifeSupportAndSolarEquipmentPriority(GameTestHelper test){
        var player=player(test);var level=test.getLevel();var quantum=new ItemStack(ElectroMagicTools.CONTENT.get("quantum_goggles").get());var item=(ElectricGoggles)quantum.getItem();
        ElectricItem.manager.charge(quantum,20000,Integer.MAX_VALUE,true,false);player.setAirSupply(90);player.addEffect(new MobEffectInstance(MobEffects.POISON,100,0));item.onArmorTick(quantum,level,player);
        test.assertTrue(player.getAirSupply()==290&&!player.hasEffect(MobEffects.POISON),"Paid quantum air refill and milk cure");test.assertTrue(ElectricItem.manager.getCharge(quantum)==9000,"1000 EU air plus 10000 EU poison");test.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION),"Original night vision remains available");
        var solar=new ItemStack(ElectroMagicTools.CONTENT.get("solar_goggles").get());var solarItem=(ElectricGoggles)solar.getItem();player.setItemSlot(EquipmentSlot.HEAD,solar);player.setPos(test.absolutePos(new net.minecraft.core.BlockPos(3,30,3)).getCenter());
        level.setDayTime(6000);level.setWeatherParameters(10000,0,false,false);var tool=new ItemStack(ElectroMagicTools.CONTENT.get("thaumium_drill").get());player.getInventory().items.set(0,tool);
        // Level.isDay uses skyDarken, refreshed on the following world tick after setDayTime.
        test.runAfterDelay(2,()->{
            solarItem.generate(solar,level,player);test.assertTrue(ElectricItem.manager.getCharge(solar)==256&&ElectricItem.manager.getCharge(tool)==0,"Solar 256 EU fills equipped armor first");
            ElectricItem.manager.charge(solar,Double.MAX_VALUE,Integer.MAX_VALUE,true,false);solarItem.generate(solar,level,player);test.assertTrue(ElectricItem.manager.getCharge(tool)==100,"Inventory charging honors tool transfer limit");test.succeed();
        });
    }
}
