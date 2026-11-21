package ttmodern;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class BlockTalismanServerTests {
    @GameTest(template="wand_test_empty") public static void tinkererBlockTalismanCompactionPreservesCountsAndTags(GameTestHelper test){
        var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTReserve"));var ring=new ItemStack(Tinkerer.BLOCK_TALISMAN.get());BlockTalismanItem.bind(ring,Blocks.COBBLESTONE.defaultBlockState());
        player.getInventory().setItem(0,new ItemStack(Items.COBBLESTONE,20));player.getInventory().setItem(10,new ItemStack(Items.COBBLESTONE,64));var named=new ItemStack(Items.COBBLESTONE,5);named.setHoverName(net.minecraft.network.chat.Component.literal("Keep metadata"));player.getInventory().setItem(11,named);
        BlockTalismanItem.replenish(player,ring);test.assertTrue(player.getInventory().getItem(0).getCount()==64&&BlockTalismanItem.count(ring)==20&&player.getInventory().getItem(10).isEmpty()&&player.getInventory().getItem(11)==named,"One hotbar stack is filled; reserve absorbs extras without swallowing custom NBT");
        test.assertTrue(!BlockTalismanItem.bind(ring,Blocks.DIRT.defaultBlockState()),"Nonempty reserve cannot be rebound");
        BlockTalismanItem.add(ring,Integer.MAX_VALUE);test.assertTrue(BlockTalismanItem.count(ring)==Integer.MAX_VALUE&&BlockTalismanItem.add(ring,64)==0,"Reserve saturates without overflow or negative counts");test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererBlockTalismanRejectingInventoryDoesNotCharge(GameTestHelper test){
        var ring=new ItemStack(Tinkerer.BLOCK_TALISMAN.get());BlockTalismanItem.bind(ring,Blocks.STONE.defaultBlockState());BlockTalismanItem.add(ring,100);
        var denied=new net.minecraftforge.items.ItemStackHandler(1){@Override public boolean isItemValid(int slot,ItemStack stack){return false;}};
        test.assertTrue(BlockTalismanItem.deposit(ring,denied)==0&&BlockTalismanItem.count(ring)==100,"Rejected insertion preserves reserve");
        var partial=new net.minecraftforge.items.ItemStackHandler(1){@Override public int getSlotLimit(int slot){return 16;}};
        test.assertTrue(BlockTalismanItem.deposit(ring,partial)==16&&partial.getStackInSlot(0).getCount()==16&&BlockTalismanItem.count(ring)==84,"Only accepted items are debited");test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererBlockTalismanPlacementProtectionPreservesReserve(GameTestHelper test){
        var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTPlaceRing"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);var ring=new ItemStack(Tinkerer.BLOCK_TALISMAN.get());BlockTalismanItem.bind(ring,Blocks.STONE.defaultBlockState());BlockTalismanItem.add(ring,5);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ring);
        var pos=test.absolutePos(new net.minecraft.core.BlockPos(2,1,2));test.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),3);var target=pos.above();test.getLevel().removeBlock(target,false);player.moveTo(pos.getCenter().add(2,1,0));
        Object protector=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent public void BlockTalismanServerTests_place_1(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event){if(event.getEntity()==player)event.setCanceled(true);}};
        var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),net.minecraft.core.Direction.UP,pos,false));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(protector);try{Tinkerer.BLOCK_TALISMAN.get().useOn(context);test.assertTrue(test.getLevel().isEmptyBlock(target)&&BlockTalismanItem.count(ring)==5&&player.getMainHandItem()==ring,"Canceled placement neither spends blocks nor replaces the held talisman");}finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(protector);}
        Tinkerer.BLOCK_TALISMAN.get().useOn(context);test.assertTrue(test.getLevel().getBlockState(target).is(Blocks.STONE)&&BlockTalismanItem.count(ring)==4,"Successful placement spends exactly one block");test.getLevel().removeBlock(target,false);test.succeed();
    }
}
