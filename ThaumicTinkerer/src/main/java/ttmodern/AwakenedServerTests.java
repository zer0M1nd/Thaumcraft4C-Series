package ttmodern;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class AwakenedServerTests {
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper test){var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTAwakened"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);return player;}
    @GameTest(template="wand_test_empty") public static void tinkererAwakenedMiningHonorsProtection(GameTestHelper test){
        var world=test.getLevel();var origin=test.absolutePos(new BlockPos(2,2,2));var allowed=origin.east();var denied=origin.west();world.setBlock(allowed,Blocks.STONE.defaultBlockState(),3);world.setBlock(denied,Blocks.STONE.defaultBlockState(),3);
        var player=player(test);player.moveTo(origin.getCenter());var tool=new ItemStack(Tinkerer.AWAKENED_PICK.get());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,tool);
        Object protector=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent public void AwakenedServerTests_broken_1(net.minecraftforge.event.level.BlockEvent.BreakEvent event){if(event.getPlayer()==player&&event.getPos().equals(denied))event.setCanceled(true);}};
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(protector);
        try{AwakenedMining.mine(player,origin,Direction.UP,tool,1,Blocks.STONE.defaultBlockState());test.assertTrue(world.isEmptyBlock(allowed)&&world.getBlockState(denied).is(Blocks.STONE),"Protected neighbor remains intact while legal neighbor is harvested");}
        finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(protector);world.removeBlock(denied,false);}
        test.assertTrue(!tool.isDamageableItem(),"Range mining never damages the tool");test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererSoulHeartsPreserveFractionalDamageAndCap(GameTestHelper test){
        var player=player(test);SoulHearts.add(player,25);test.assertTrue(SoulHearts.get(player)==20,"Soul hearts cap at twenty health");
        var hit=new net.minecraftforge.event.entity.living.LivingHurtEvent(player,test.getLevel().damageSources().magic(),.25f);SoulHearts.damage(hit);test.assertTrue(hit.getAmount()==0&&SoulHearts.get(player)==19.75f,"Fractional damage consumes the same fractional shield");
        SoulHearts.set(player,.25f);hit=new net.minecraftforge.event.entity.living.LivingHurtEvent(player,test.getLevel().damageSources().magic(),.75f);SoulHearts.damage(hit);test.assertTrue(hit.getAmount()==.5f&&SoulHearts.get(player)==0,"Fractional overflow is not discarded");
        hit=new net.minecraftforge.event.entity.living.LivingHurtEvent(player,test.getLevel().damageSources().magic(),.5f);SoulHearts.damage(hit);test.assertTrue(hit.getAmount()==.5f,"Empty shield cannot turn sub-one damage into immunity");
        SoulHearts.set(player,5);hit=new net.minecraftforge.event.entity.living.LivingHurtEvent(player,test.getLevel().damageSources().magic(),3);hit.setCanceled(true);SoulHearts.damage(hit);test.assertTrue(SoulHearts.get(player)==5,"Canceled damage does not spend stored hearts");
        var stack=new ItemStack(Tinkerer.AWAKENED_SWORD.get());stack.getOrCreateTag().putInt("ToolMode",999);test.assertTrue(AwakenedModes.mode(stack)==0,"Malformed mode safely defaults to normal");test.succeed();
    }
}
