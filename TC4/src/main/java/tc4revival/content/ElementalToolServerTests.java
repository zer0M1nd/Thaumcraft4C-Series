package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class ElementalToolServerTests {
    private static FakePlayer player(GameTestHelper test,BlockPos pos,String name) {
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),name));
        player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5); return player;
    }
    private static UseOnContext use(FakePlayer player,BlockPos pos,Direction face) {
        return new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),face,pos,false));
    }
    @GameTest(template="wand_test_empty")
    public static void elementalHoeTillingAndTreeCosts(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,3,3)); var player=player(test,pos,"HoeTest");
        var hoe=new ItemStack(TCItems.ELEMENTAL_HOE.get()); player.setItemInHand(InteractionHand.MAIN_HAND,hoe);
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) { level.setBlock(pos.offset(x,0,z),Blocks.DIRT.defaultBlockState(),3); level.setBlock(pos.offset(x,1,z),Blocks.AIR.defaultBlockState(),3); }
        test.assertTrue(hoe.useOn(use(player,pos,Direction.UP))==InteractionResult.SUCCESS,"Nine-cell tilling succeeds");
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) test.assertTrue(level.getBlockState(pos.offset(x,0,z)).is(Blocks.FARMLAND),"All nine cells tilled");
        test.assertTrue(hoe.getDamageValue()==9,"One durability per tilled block");
        level.setBlock(pos,Blocks.COARSE_DIRT.defaultBlockState(),3); player.setShiftKeyDown(true);
        hoe.useOn(use(player,pos,Direction.UP)); test.assertTrue(hoe.getDamageValue()==10 && level.getBlockState(pos).is(Blocks.DIRT),"Coarse dirt first becomes ordinary dirt");
        hoe.useOn(use(player,pos,Direction.UP)); test.assertTrue(hoe.getDamageValue()==11 && level.getBlockState(pos).is(Blocks.FARMLAND),"A second use tills ordinary dirt");
        player.setShiftKeyDown(false);
        var sapling=pos.above(); level.setBlock(pos,Blocks.DIRT.defaultBlockState(),3);
        level.setBlock(sapling,TCArcane.SILVERWOOD_SAPLING.get().defaultBlockState(),2); level.setBlock(sapling.above(),Blocks.BEDROCK.defaultBlockState(),2);
        hoe.setDamageValue(hoe.getMaxDamage()-149); hoe.useOn(use(player,sapling,Direction.UP));
        test.assertTrue(hoe.getDamageValue()==hoe.getMaxDamage()-149,"Silverwood rejects less than 150 durability remaining");
        hoe.setDamageValue(hoe.getMaxDamage()-150); hoe.useOn(use(player,sapling,Direction.UP));
        test.assertTrue(hoe.getDamageValue()==hoe.getMaxDamage()-125 && level.getBlockState(sapling).is(TCArcane.SILVERWOOD_SAPLING.get()),"Failed tree generation restores sapling but consumes 25");
        test.assertTrue(hoe.getDestroySpeed(Blocks.HAY_BLOCK.defaultBlockState())>1,"Hoe uses modern fast harvesting");
        pass(test,"hoe-tilling-tree-costs");
    }
    @GameTest(template="wand_test_empty",timeoutTicks=120)
    public static void elementalAxeHarvestAndFollowing(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,3,3)); var player=player(test,pos.west(2),"AxeTest");
        var axe=new ItemStack(TCItems.ELEMENTAL_AXE.get()); player.setItemInHand(InteractionHand.MAIN_HAND,axe);
        for (int y=0;y<4;y++) level.setBlock(pos.above(y),Blocks.OAK_LOG.defaultBlockState(),3);
        test.assertTrue(ElementalTreeSearch.furthest(level,pos).equals(pos.above(3)),"Greedy walk finds upper log");
        test.assertTrue(axe.getItem().onBlockStartBreak(axe,pos,player),"Special chop cancels original break");
        test.assertTrue(level.isEmptyBlock(pos.above(3)) && level.getBlockState(pos).is(Blocks.OAK_LOG),"Only distant log harvested");
        test.assertTrue(axe.getDamageValue()==1,"One durability per special chop");
        var drops=level.getEntitiesOfClass(FollowingItemEntity.class,new AABB(pos).inflate(5));
        test.assertTrue(drops.size()==1 && drops.get(0).hasFollowingTarget(),"Harvest drop follows player");
        var item=drops.get(0); var saved=new net.minecraft.nbt.CompoundTag(); item.saveWithoutId(saved);
        var restored=TCEntities.FOLLOWING_ITEM.get().create(level); restored.load(saved);
        test.assertTrue(!restored.hasFollowingTarget() && restored.getItem().is(Items.OAK_LOG),"Stored items retain stack but not transient target");
        test.runAfterDelay(65,()->{ test.assertTrue(!item.hasFollowingTarget(),"Actual world ticks finish following"); pass(test,"axe-harvest-following"); });
    }
    @GameTest(template="wand_test_empty")
    public static void elementalShovelPlacementAndMining(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,3,3)); var player=player(test,pos,"ShovelTest");
        var shovel=new ItemStack(TCItems.ELEMENTAL_SHOVEL.get()); player.setItemInHand(InteractionHand.MAIN_HAND,shovel);
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) level.setBlock(pos.offset(x,1,z),Blocks.AIR.defaultBlockState(),2);
        level.setBlock(pos,Blocks.GRASS_BLOCK.defaultBlockState(),3); player.getInventory().setItem(1,new ItemStack(Items.DIRT,9));
        test.assertTrue(shovel.useOn(use(player,pos,Direction.UP)).consumesAction(),"Successful placement commits Forge snapshots and neighbour notifications");
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) test.assertTrue(level.getBlockState(pos.offset(x,1,z)).is(Blocks.DIRT),"Grass template falls back to inventory dirt");
        test.assertTrue(player.getInventory().getItem(1).isEmpty() && shovel.getDamageValue()==9,"Nine placements consume nine items and durability");
        var center=pos.above(); level.setBlock(center,Blocks.AIR.defaultBlockState(),3);
        shovel.getItem().mineBlock(shovel,level,Blocks.DIRT.defaultBlockState(),center,player);
        test.assertTrue(shovel.getDamageValue()==17,"Area mining damages eight surviving neighbors after original block removal");
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) test.assertTrue(level.isEmptyBlock(center.offset(x,0,z)),"Nine-cell mining plane cleared");
        new tc4revival.network.WandAreaPacket(InteractionHand.MAIN_HAND).handle(player);
        test.assertTrue(ElementalShovelLayout.orientation(shovel)==1,"Server-authoritative direction key changes shovel");
        pass(test,"shovel-placement-mining");
    }
    private static void pass(GameTestHelper test,String name) {
        com.mojang.logging.LogUtils.getLogger().info("Elemental tool system test passed: {}",name); test.succeed();
    }
}
