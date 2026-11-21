package ttmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class NecromancyServerTests {
    private static net.minecraft.world.entity.player.Player player(GameTestHelper test,BlockPos pos){var player=new net.minecraft.world.entity.player.Player(test.getLevel(),pos,0,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TTBlood")){public boolean isSpectator(){return false;}public boolean isCreative(){return false;}};player.moveTo(pos.getCenter());return player;}
    @GameTest(template="wand_test_empty") public static void tinkererSummoningCatalogNetworkRoundTrip(GameTestHelper test){
        var recipes=MobEssenceCatalog.recipes();test.assertTrue(!recipes.isEmpty(),"Loaded data pack contains summon recipes");var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{SummoningSync.encode(new SummoningSync.Snapshot(recipes),buffer);var decoded=SummoningSync.decode(buffer);test.assertTrue(decoded.recipes().equals(recipes)&&buffer.readableBytes()==0,"All entity IDs, ordered aspects and duplicate input quantities survive synchronization");buffer.clear();buffer.writeVarInt(4097);boolean rejected=false;try{SummoningSync.decode(buffer);}catch(IllegalArgumentException expected){rejected=true;}test.assertTrue(rejected,"Oversized recipe list rejected before allocation");}finally{buffer.release();}test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererBloodSwordSelfCostBlockingAndDrops(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var player=player(test,pos);var sword=new ItemStack(Tinkerer.BLOOD_SWORD.get());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sword);
        var cow=EntityType.COW.create(level);cow.moveTo(pos.getCenter().add(1,0,0));level.addFreshEntity(cow);player.setHealth(20);cow.hurt(level.damageSources().playerAttack(player),1);test.assertTrue(player.getHealth()==18,"Successful blood sword attack costs one heart");
        player.invulnerableTime=0;player.setHealth(20);player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);player.hurt(level.damageSources().mobAttack(cow),100);test.assertTrue(player.getHealth()==17,"Blocking replaces incoming amount with three magic damage without recursion");player.stopUsingItem();
        player.invulnerableTime=0;var victim=EntityType.WOLF.create(level);victim.moveTo(pos.getCenter().add(-1,0,0));level.addFreshEntity(victim);sword.getOrCreateTag().putBoolean("Activated",true);victim.hurt(level.damageSources().playerAttack(player),100);
        var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,victim.getBoundingBox().inflate(2));int souls=drops.stream().filter(e->e.getItem().getItem() instanceof MobEssenceItem).mapToInt(e->e.getItem().getCount()).sum();test.assertTrue(souls==3&&drops.stream().filter(e->e.getItem().getItem() instanceof MobEssenceItem).allMatch(e->((MobEssenceItem)e.getItem().getItem()).aspect().equals("bestia")),"Repeated wolf aspects yield three souls: found="+souls+", health="+victim.getHealth()+", active="+BloodSwordItem.active(player.getMainHandItem())+", drops="+drops.stream().map(e->e.getItem().toString()).toList());drops.forEach(Entity::discard);cow.discard();victim.discard();test.succeed();
    }
    private static List<ArcanePedestalBlockEntity> pedestals(GameTestHelper test,BlockPos pos,String... aspects){var result=new ArrayList<ArcanePedestalBlockEntity>();int[][] offsets={{1,0},{-1,0},{0,1}};for(int i=0;i<3;i++){var at=pos.offset(offsets[i][0],0,offsets[i][1]);test.getLevel().setBlock(at,TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),3);var pedestal=(ArcanePedestalBlockEntity)test.getLevel().getBlockEntity(at);var stack=MobEssenceItem.stack(aspects[i],0);stack.setCount(4);pedestal.setItem(0,stack);result.add(pedestal);}return result;}
    @GameTest(template="wand_test_empty") public static void tinkererSummonRequiresMultiplicityAndConservesStacks(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));level.setBlock(pos,Tinkerer.SUMMONING_TABLET.get().defaultBlockState(),3);var tablet=(SummoningTabletEntity)level.getBlockEntity(pos);var pedestals=pedestals(test,pos,"terra","terra","bestia");
        test.assertTrue(!tablet.attempt(level),"Set equality is not enough: terra terra bestia must not summon a cow");
        pedestals.get(0).setItem(0,MobEssenceItem.stack("bestia",0));var before=pedestals.stream().map(p->p.getItem(0).getCount()).toList();
        test.assertTrue(tablet.attempt(level),"Cow composition matches all three independent pedestals");for(int i=0;i<3;i++)test.assertTrue(pedestals.get(i).getItem(0).getCount()==before.get(i)-1,"Only one essence is consumed per pedestal");
        var mobs=level.getEntitiesOfClass(net.minecraft.world.entity.animal.Cow.class,new net.minecraft.world.phys.AABB(pos).inflate(3));test.assertTrue(mobs.size()==1,"One invocation spawns exactly one matching creature");mobs.forEach(Entity::discard);pedestals.forEach(p->level.removeBlock(p.getBlockPos(),false));level.removeBlock(pos,false);test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererSummonCancellationAndRedstonePreserveInputs(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));level.setBlock(pos,Tinkerer.SUMMONING_TABLET.get().defaultBlockState(),3);var tablet=(SummoningTabletEntity)level.getBlockEntity(pos);var pedestals=pedestals(test,pos,"bestia","terra","iter");var before=pedestals.stream().map(p->p.getItem(0).copy()).toList();
        level.setBlock(pos.above(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);test.assertTrue(!tablet.attempt(level),"Redstone disables summoning");level.removeBlock(pos.above(),false);
        Object cancel=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent public void NecromancyServerTests_spawn_1(net.minecraftforge.event.entity.EntityJoinLevelEvent event){if(event.getLevel()==level&&event.getEntity() instanceof net.minecraft.world.entity.animal.Pig)event.setCanceled(true);}};
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(cancel);try{test.assertTrue(!tablet.attempt(level),"Canceled spawn reports failure");for(int i=0;i<3;i++)test.assertTrue(ItemStack.matches(before.get(i),pedestals.get(i).getItem(0)),"Rejected spawn does not consume or relabel any input");}finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(cancel);pedestals.forEach(p->level.removeBlock(p.getBlockPos(),false));level.removeBlock(pos,false);}test.succeed();
    }
}
