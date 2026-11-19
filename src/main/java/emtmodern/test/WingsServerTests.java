package emtmodern.test;

import emtmodern.ElectroMagicTools;
import emtmodern.item.WingFlight;
import ic2.api.item.ElectricItem;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;

@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class WingsServerTests {
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void flapGlideAndIndustrialEnergy(GameTestHelper test){
        test.getLevel().setWeatherParameters(10000,0,false,false);test.getLevel().setRainLevel(0);
        var player=GogglesServerTests.player(test);player.getAbilities().instabuild=false;
        for(String kind:java.util.List.of("feather","thaumium","nano","quantum")){
            var stack=new ItemStack(ElectroMagicTools.CONTENT.get(kind+"_wings").get());var profile=WingFlight.PROFILES.get(kind);player.setItemSlot(EquipmentSlot.CHEST,stack);player.setDeltaMovement(.1,0,.1);player.setOnGround(false);
            if(profile.energy()>0)ElectricItem.manager.charge(stack,10000,Integer.MAX_VALUE,true,false);
            for(int i=0;i<10;i++)WingFlight.tick(player,stack,profile,true);
            WingFlight.tick(player,stack,profile,false);int force=stack.getTag().getByte("f");WingFlight.tick(player,stack,profile,false);
            test.assertTrue(Math.abs(player.getDeltaMovement().y-profile.lift()*force)<.0001,"Release produces accumulated upward impulse");
            test.assertTrue(Math.abs(player.getDeltaMovement().x-.1/profile.horizontal())<.0001,"Flap accelerates limited horizontal motion");
            if(profile.energy()>0)test.assertTrue(Math.abs(ElectricItem.manager.getCharge(stack)-(10000-profile.lift()*force*10*profile.energy()))<.001,"Paid exact flap energy through IC2");
            player.setDeltaMovement(0,-1,0);stack.getOrCreateTag().putBoolean("isJumping",true);WingFlight.tick(player,stack,profile,true);
            test.assertTrue(Math.abs(player.getDeltaMovement().y+profile.glide())<.0001,"Held jump slows descent by original multiplier");
            var hurt=new net.minecraftforge.event.entity.living.LivingHurtEvent(player,player.damageSources().fall(),20);WingFlight.fallProtection(hurt);test.assertTrue(Math.abs(hurt.getAmount()-20*profile.fall())<.001,"Original damage multiplier retained");
            player.fallDistance=7;player.setDeltaMovement(0,1.5,0);WingFlight.risingFallDistance(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,player));test.assertTrue(player.fallDistance==5.5,"Rising motion subtracts accumulated falling distance");
            if(profile.energy()>0){
                ElectricItem.manager.discharge(stack,Double.MAX_VALUE,Integer.MAX_VALUE,true,false,false);stack.getTag().putBoolean("isJumping",false);stack.getTag().putBoolean("isHolding",true);stack.getTag().putByte("f",(byte)7);WingFlight.tick(player,stack,profile,false);test.assertTrue(player.getDeltaMovement().y>0,"Author's empty-charge flap behavior retained");
                ElectricItem.manager.charge(stack,10000,Integer.MAX_VALUE,true,false);test.assertTrue(Math.abs(ic2.core.item.armor.ItemArmorElectric.damageArmor(player,player.damageSources().generic(),20)-16.7)<.001,"Powered wings retain original 16.5 percent protection despite chest slot");
            }
        }
        var glue=test.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("emt","glue")).orElseThrow();test.assertTrue(glue.getResultItem(test.getLevel().registryAccess()).getCount()==2,"Rubber ball smelts to two glue");test.succeed();
    }
}
