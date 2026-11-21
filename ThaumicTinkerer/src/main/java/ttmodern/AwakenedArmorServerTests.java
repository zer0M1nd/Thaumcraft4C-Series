package ttmodern;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class AwakenedArmorServerTests {
    @GameTest(template="wand_test_empty") public static void awakenedLeggingsMaintainAndReleasePersonalLight(GameTestHelper test){
        var p=player(test);var level=test.getLevel();boolean enabled=TinkererConfig.KAMI_LEG_LIGHT.get();
        var pos=p.blockPosition().above();var legs=new ItemStack(Tinkerer.AWAKENED_LEGS.get());
        level.addFreshEntity(p);
        try{
            TinkererConfig.KAMI_LEG_LIGHT.set(true);p.setItemSlot(EquipmentSlot.LEGS,legs);p.tickCount=5;
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            AwakenedArmorEffects.tickPlayer(p);
            test.assertTrue(level.getBlockState(pos).is(Tinkerer.NITOR_GAS.get())&&level.getBlockState(pos).getLightEmission(level,pos)==15,"Awakened leggings create the legacy level-15 centre light");
            Tinkerer.NITOR_GAS.get().tick(level.getBlockState(pos),level,pos,level.random);
            test.assertTrue(level.getBlockState(pos).is(Tinkerer.NITOR_GAS.get()),"A wearer without Bright Nitor sustains temporary light");
            legs.getOrCreateTag().putBoolean("AbilityDisabled",true);
            Tinkerer.NITOR_GAS.get().tick(level.getBlockState(pos),level,pos,level.random);
            test.assertTrue(level.getBlockState(pos).isAir(),"Disabling the ability releases the light");
            p.setItemSlot(EquipmentSlot.LEGS,ItemStack.EMPTY);var nitor=new ItemStack(Tinkerer.BRIGHT_NITOR.get());p.getInventory().add(nitor);
            // Inventory.add consumes the input stack; tick the actual stored copy.
            var carried=p.getInventory().getItem(0);carried.getItem().inventoryTick(carried,level,p,0,false);
            test.assertTrue(level.getBlockState(pos).getLightEmission(level,pos)==12,"Bright Nitor keeps its original level-12 light");
            Tinkerer.NITOR_GAS.get().tick(level.getBlockState(pos),level,pos,level.random);
            test.assertTrue(level.getBlockState(pos).is(Tinkerer.NITOR_GAS.get()),"Carried nitor shares the retention mechanism");
            p.getInventory().clearContent();Tinkerer.NITOR_GAS.get().tick(level.getBlockState(pos),level,pos,level.random);
            test.assertTrue(level.getBlockState(pos).isAir(),"Removing both sources releases light");
        }finally{TinkererConfig.KAMI_LEG_LIGHT.set(enabled);p.discard();level.removeBlock(pos,false);}
        test.succeed();
    }
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper test){var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTGemArmor"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.moveTo(test.absolutePos(new BlockPos(2,3,2)).getCenter());return player;}
    @GameTest(template="wand_test_empty") public static void tinkererAwakenedArmorToggleAndFlightOwnership(GameTestHelper test){
        var player=player(test);var chest=new ItemStack(Tinkerer.AWAKENED_CHEST.get());player.setItemSlot(EquipmentSlot.CHEST,chest);AwakenedArmorEffects.tickPlayer(player);test.assertTrue(player.getAbilities().mayfly,"Active chest grants flight");
        chest.getOrCreateTag().putBoolean("AbilityDisabled",true);AwakenedArmorEffects.tickPlayer(player);test.assertTrue(!player.getAbilities().mayfly&&!player.getAbilities().flying,"Disabling chest revokes its own flight");
        player.getAbilities().mayfly=true;chest.getOrCreateTag().putBoolean("AbilityDisabled",false);AwakenedArmorEffects.tickPlayer(player);player.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);AwakenedArmorEffects.tickPlayer(player);test.assertTrue(player.getAbilities().mayfly,"A flight grant already present before the chest is preserved");
        float originalStep=player.getStepHeight();var boots=new ItemStack(Tinkerer.AWAKENED_BOOTS.get());player.setItemSlot(EquipmentSlot.FEET,boots);AwakenedArmorEffects.tickPlayer(player);test.assertTrue(Math.abs(player.getStepHeight()-originalStep-.4)<.0001,"Boots enable a full-block step");
        player.getPersistentData().putBoolean(AwakenedArmorEffects.DISABLED,true);AwakenedArmorEffects.tickPlayer(player);test.assertTrue(Math.abs(player.getStepHeight()-originalStep)<.0001&&!AwakenedArmorEffects.active(player,EquipmentSlot.FEET),"Global switch removes only addon step modifier");test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererAwakenedArmorProtectionAndHealing(GameTestHelper test){
        var player=player(test);player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Tinkerer.AWAKENED_HELM.get()));player.getFoodData().setFoodLevel(10);player.setHealth(10);player.tickCount=80;AwakenedArmorEffects.tickPlayer(player);test.assertTrue(player.getHealth()==11,"Helmet heals one health every eighty ticks while partially fed");
        player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Tinkerer.AWAKENED_LEGS.get()));player.setSecondsOnFire(10);player.tickCount=85;AwakenedArmorEffects.tickPlayer(player);test.assertTrue(player.getHealth()==11.5f&&player.hasEffect(MobEffects.FIRE_RESISTANCE),"Burning mantle grants resistance and heals half a health every five ticks");
        var boots=new ItemStack(Tinkerer.AWAKENED_BOOTS.get());player.setItemSlot(EquipmentSlot.FEET,boots);var fall=new net.minecraftforge.event.entity.living.LivingFallEvent(player,30,1);AwakenedArmorEffects.fall(fall);test.assertTrue(fall.getDamageMultiplier()==0&&fall.getDistance()==30,"Enabled boots prevent damage while retaining the landing distance");
        boots.getOrCreateTag().putBoolean("AbilityDisabled",true);fall=new net.minecraftforge.event.entity.living.LivingFallEvent(player,30,1);AwakenedArmorEffects.fall(fall);test.assertTrue(fall.getDamageMultiplier()==1&&fall.getDistance()==30,"Disabled boots do not leave fall immunity behind");test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void awakenedBootsRetainShockwaveWithoutFallDamage(GameTestHelper test){
        var level=test.getLevel();var wearer=player(test);var boots=new ItemStack(Tinkerer.AWAKENED_BOOTS.get());
        boots.enchant(TinkererEnchantments.get("shockwave"),5);wearer.setItemSlot(EquipmentSlot.FEET,boots);
        var target=net.minecraft.world.entity.EntityType.HUSK.create(level);target.moveTo(wearer.position().add(2,0,0));target.setNoAi(true);level.addFreshEntity(target);
        try{
            float health=target.getHealth();var event=new net.minecraftforge.event.entity.living.LivingFallEvent(wearer,20,1);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
            test.assertTrue(event.getDamageMultiplier()==0&&event.getDistance()==20,"Landing keeps distance but cannot hurt wearer");
            test.assertTrue(target.getHealth()<health,"The registered Shockwave handler still damages nearby targets");
        }finally{target.discard();wearer.discard();}
        test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererAwakenedBootsRespectGrassProtection(GameTestHelper test){
        var player=player(test);player.setItemSlot(EquipmentSlot.FEET,new ItemStack(Tinkerer.AWAKENED_BOOTS.get()));var pos=player.blockPosition().below();test.getLevel().setBlock(pos,Blocks.DIRT.defaultBlockState(),3);
        Object protector=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent public void AwakenedArmorServerTests_place_1(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event){if(event.getEntity()==player&&event.getPos().equals(pos))event.setCanceled(true);}};
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(protector);try{AwakenedArmorEffects.tickPlayer(player);test.assertTrue(test.getLevel().getBlockState(pos).is(Blocks.DIRT),"Protected dirt is restored");}finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(protector);}
        AwakenedArmorEffects.tickPlayer(player);test.assertTrue(test.getLevel().getBlockState(pos).is(Blocks.GRASS_BLOCK),"Unprotected dirt grows grass");test.getLevel().removeBlock(pos,false);test.succeed();
    }
}
