package ttmodern;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class PlacementMirrorServerTests {
    @GameTest(template="wand_test_empty") public static void tinkererPlacementMirrorProtectedBuildAndSupply(GameTestHelper test){
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(2,1,2));world.setBlock(pos,Blocks.STONE.defaultBlockState(),3);
        var player=new net.minecraftforge.common.util.FakePlayer(world,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTMirror"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(pos.getX()+5,pos.getY()+2,pos.getZ());player.setXRot(90);var mirror=new ItemStack(Tinkerer.PLACEMENT_MIRROR.get());PlacementMirrorItem.bind(mirror,world,pos);player.setItemInHand(InteractionHand.MAIN_HAND,mirror);
        for(var target:PlacementArea.positions(pos.above(),Direction.UP,Direction.NORTH,90,3))world.removeBlock(target,false);
        var context=new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),Direction.UP,pos,false));player.getInventory().setItem(10,new ItemStack(Items.STONE,8));test.assertTrue(PlacementMirrorItem.place(context)==0&&player.getInventory().getItem(10).getCount()==8,"Insufficient materials cancel the whole requested square");
        var reserve=new ItemStack(Tinkerer.BLOCK_TALISMAN.get());BlockTalismanItem.bind(reserve,Blocks.STONE.defaultBlockState());BlockTalismanItem.add(reserve,2);player.getInventory().setItem(11,reserve);
        Object protector=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent public void PlacementMirrorServerTests_place_1(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event){if(event.getEntity()==player)event.setCanceled(true);}};net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(protector);try{test.assertTrue(PlacementMirrorItem.place(context)==0&&PlacementMirrorItem.available(player,new ItemStack(Items.STONE))==10,"Canceled placement preserves inventory and reserve");}finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(protector);}
        test.assertTrue(PlacementMirrorItem.place(context)==9&&player.getInventory().getItem(10).isEmpty()&&BlockTalismanItem.count(reserve)==1,"Successful square consumes ordinary stacks before reserve, one per placed block");test.assertTrue(player.getMainHandItem()==mirror,"Virtual block use never replaces the held mirror");
        for(var target:PlacementArea.positions(pos.above(),Direction.UP,Direction.NORTH,90,3))world.removeBlock(target,false);world.removeBlock(pos,false);test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererPlacementMirrorUpgradeAndMetadataSafety(GameTestHelper test){
        var reserve=new ItemStack(Tinkerer.BLOCK_TALISMAN.get());var ingredient=new EmptyBlockTalismanIngredient();test.assertTrue(ingredient.test(reserve),"Empty talisman can upgrade");BlockTalismanItem.add(reserve,4);test.assertTrue(!ingredient.test(reserve),"Filled reserve cannot be destroyed by infusion");
        var pos=test.absolutePos(new BlockPos(2,1,2));var mirror=new ItemStack(Tinkerer.PLACEMENT_MIRROR.get());test.getLevel().setBlock(pos,Blocks.CHEST.defaultBlockState(),3);test.assertTrue(!PlacementMirrorItem.bind(mirror,test.getLevel(),pos),"Binding cannot copy block entity inventories");test.getLevel().setBlock(pos,Blocks.OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,Direction.Axis.X),3);test.assertTrue(PlacementMirrorItem.bind(mirror,test.getLevel(),pos)&&PlacementMirrorItem.template(mirror).getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)==Direction.Axis.X,"Bound state preserves log orientation");test.getLevel().removeBlock(pos,false);test.succeed();
    }
}
