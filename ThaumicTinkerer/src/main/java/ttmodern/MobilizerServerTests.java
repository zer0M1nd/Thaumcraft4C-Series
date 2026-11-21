package ttmodern;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class MobilizerServerTests {
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper test) { return new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TTMove")); }
    private static ChestBlockEntity cargo(GameTestHelper test,BlockPos pos) {
        var world=test.getLevel();world.setBlock(pos,Tinkerer.MOBILIZER.get().defaultBlockState(),3);world.setBlock(pos.above(),Blocks.CHEST.defaultBlockState(),3);var chest=(ChestBlockEntity)world.getBlockEntity(pos.above());var stack=new ItemStack(Items.DIAMOND,17);stack.setHoverName(net.minecraft.network.chat.Component.literal("Cargo"));chest.setItem(0,stack);return chest;
    }
    @GameTest(template="wand_test_empty") public static void tinkererMobilizerConservesPassengerAndRollsBack(GameTestHelper test) {
        var world=test.getLevel();var from=test.absolutePos(new BlockPos(1,2,2));var to=from.east();var chest=cargo(test,from);var original=chest.getItem(0).copy();var actor=player(test);
        int[] calls={0};test.assertTrue(!MobilizerTransfer.move(world,from,to,actor,(pos,state)->{if(++calls[0]==2)return false;return world.setBlock(pos,state,2|16);}),"A rejected passenger write rolls back all four cells");
        test.assertTrue(world.getBlockState(from).is(Tinkerer.MOBILIZER.get())&&world.getBlockEntity(from.above()) instanceof ChestBlockEntity saved&&ItemStack.matches(saved.getItem(0),original)&&world.isEmptyBlock(to)&&world.isEmptyBlock(to.above()),"Failed move preserves exact named inventory and both empty targets");
        test.assertTrue(MobilizerTransfer.move(world,from,to,actor),"Movable chest succeeds");test.assertTrue(world.isEmptyBlock(from)&&world.isEmptyBlock(from.above())&&world.getBlockEntity(to.above()) instanceof ChestBlockEntity moved&&ItemStack.matches(moved.getItem(0),original)&&moved.getBlockPos().equals(to.above()),"Moved chest preserves inventory at new coordinates");
        test.assertTrue(world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(from,to.above().east())).isEmpty(),"Moving or rollback does not spill inventory drops");world.removeBlockEntity(to.above());world.removeBlock(to.above(),false);world.removeBlock(to,false);test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererMobilizerDestinationAndProtectionCannotLoseCargo(GameTestHelper test) {
        var world=test.getLevel();var from=test.absolutePos(new BlockPos(1,2,2));var to=from.east();cargo(test,from);var actor=player(test);world.setBlock(to.above(),Blocks.GOLD_BLOCK.defaultBlockState(),3);
        test.assertTrue(!MobilizerTransfer.move(world,from,to,actor)&&world.getBlockState(to.above()).is(Blocks.GOLD_BLOCK),"Occupied overhead destination cannot be overwritten");world.removeBlock(to.above(),false);
        Object protector=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent public void MobilizerServerTests_place_1(net.minecraftforge.event.level.BlockEvent.EntityMultiPlaceEvent event){if(event.getEntity()==actor)event.setCanceled(true);}};
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(protector);try {test.assertTrue(!MobilizerTransfer.move(world,from,to,actor),"Protection cancellation aborts the whole move");test.assertTrue(world.getBlockEntity(from.above()) instanceof ChestBlockEntity chest&&chest.getItem(0).getCount()==17&&world.isEmptyBlock(to),"Protected move restores cargo exactly");}finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(protector);world.removeBlockEntity(from.above());world.removeBlock(from.above(),false);world.removeBlock(from,false);}test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererRelayPairingAndRedstoneStop(GameTestHelper test) {
        var world=test.getLevel();var left=test.absolutePos(new BlockPos(0,2,2));var right=left.east(4);var origin=left.east();
        for(var pos:java.util.List.of(left,right))world.setBlock(pos,Tinkerer.MOBILIZER_RELAY.get().defaultBlockState(),3);world.setBlock(origin,Tinkerer.MOBILIZER.get().defaultBlockState(),3);
        var first=(MobilizerRelayEntity)world.getBlockEntity(left);var second=(MobilizerRelayEntity)world.getBlockEntity(right);test.assertTrue(first.pair(second)&&first.verify()&&second.verify(),"Relays pair reciprocally");var mover=(MobilizerEntity)world.getBlockEntity(origin);mover.link(left,right);
        world.setBlock(origin.below(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);test.assertTrue(!mover.move(),"Redstone pauses a linked mover");world.removeBlock(origin.below(),false);test.assertTrue(mover.move(),"Linked mover advances once when unpowered");world.removeBlock(right,false);var moved=(MobilizerEntity)world.getBlockEntity(origin.east());test.assertTrue(!moved.move(),"Breaking endpoint stops movement immediately");
        var malformed=new net.minecraft.nbt.CompoundTag();malformed.putString("Direction","up");moved.load(malformed);test.assertTrue(moved.direction().getAxis().isHorizontal(),"Malformed direction cannot move vertically");world.removeBlock(origin.east(),false);world.removeBlock(left,false);test.succeed();
    }
}
