package emtmodern.test;

import emtmodern.ElectroMagicTools;
import emtmodern.item.*;
import ic2.api.item.ElectricItem;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class AdvancedToolServerTests {
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void rockbreakerPlanesEnergyProtectionAndFollowing(GameTestHelper test){
        var level=test.getLevel();var origin=test.absolutePos(new BlockPos(3,4,3));var player=GogglesServerTests.player(test);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var stack=new ItemStack(ElectroMagicTools.CONTENT.get("rockbreaker_drill").get());var drill=(RockbreakerDrill)stack.getItem();player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        for(Direction direction:java.util.List.of(Direction.UP,Direction.NORTH,Direction.EAST)){
            for(BlockPos pos:RockbreakerDrill.plane(origin,direction))level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
            Vec3 viewpoint=Vec3.atCenterOf(origin).add(Vec3.atLowerCornerOf(direction.getNormal()).scale(4));player.setPos(viewpoint.x,viewpoint.y-player.getEyeHeight(),viewpoint.z);player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,Vec3.atCenterOf(origin));
            ElectricItem.manager.charge(stack,900000,Integer.MAX_VALUE,true,false);drill.onBlockStartBreak(stack,origin,player);level.removeBlock(origin,false);drill.mineBlock(stack,level,Blocks.STONE.defaultBlockState(),origin,player);
            for(BlockPos pos:RockbreakerDrill.plane(origin,direction))test.assertTrue(level.isEmptyBlock(pos),"Mining plane follows selected face axis: "+direction);
            test.assertTrue(ElectricItem.manager.getCharge(stack)==897200,"Eight neighbors cost 8 x 350 EU; no duplicate center debit");
            var drops=level.getEntitiesOfClass(tc4revival.content.FollowingItemEntity.class,new AABB(origin).inflate(5));test.assertTrue(drops.stream().mapToInt(drop->drop.getItem().getCount()).sum()==8&&drops.stream().allMatch(tc4revival.content.FollowingItemEntity::hasFollowingTarget),"Eight exact following drops");drops.forEach(net.minecraft.world.entity.Entity::discard);
        }
        var protectedPos=origin.north();level.setBlockAndUpdate(protectedPos,Blocks.STONE.defaultBlockState());
        java.util.function.Consumer<net.minecraftforge.event.level.BlockEvent.BreakEvent> reject=event->{if(event.getLevel()==level&&event.getPos().equals(protectedPos))event.setCanceled(true);};net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(reject);
        try{test.assertTrue(!drill.harvestNeighbour(player,protectedPos,stack)&&level.getBlockState(protectedPos).is(Blocks.STONE),"Protection cancellation prevents adjacent mining");}finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(reject);}
        level.setBlockAndUpdate(protectedPos,Blocks.BEDROCK.defaultBlockState());test.assertTrue(!drill.harvestNeighbour(player,protectedPos,stack),"Unbreakable block remains intact");
        player.setShiftKeyDown(true);double before=ElectricItem.manager.getCharge(stack);drill.mineBlock(stack,level,Blocks.STONE.defaultBlockState(),origin,player);test.assertTrue(ElectricItem.manager.getCharge(stack)==before-350,"Sneaking only charges one block");test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void streamChainsawGreedyChopChargeBoundaryAndShears(GameTestHelper test){
        var level=test.getLevel();var origin=test.absolutePos(new BlockPos(3,3,3));var player=GogglesServerTests.player(test);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(Vec3.atCenterOf(origin.west(3)));
        var stack=new ItemStack(ElectroMagicTools.CONTENT.get("stream_chainsaw").get());var saw=(StreamChainsaw)stack.getItem();player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        for(int y=0;y<4;y++)level.setBlockAndUpdate(origin.above(y),Blocks.OAK_LOG.defaultBlockState());test.assertTrue(StreamChainsaw.furthest(level,origin).equals(origin.above(3)),"Legacy greedy traversal reaches top log");
        ElectricItem.manager.charge(stack,400,Integer.MAX_VALUE,true,false);
        var context=new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(origin),Direction.WEST,origin,false));saw.useOn(context);
        test.assertTrue(level.getBlockState(origin.above(3)).is(Blocks.OAK_LOG),"Right click requires strictly more than 400 EU");ElectricItem.manager.charge(stack,1,Integer.MAX_VALUE,true,false);saw.useOn(context);
        test.assertTrue(level.isEmptyBlock(origin.above(3))&&ElectricItem.manager.getCharge(stack)==1,"Right click chops one distant log for 400 EU");
        ElectricItem.manager.discharge(stack,Double.MAX_VALUE,Integer.MAX_VALUE,true,false,false);test.assertTrue(saw.onBlockStartBreak(stack,origin,player)&&level.isEmptyBlock(origin.above(2)),"Author's empty-charge left-click chop retained");
        test.assertTrue(level.getEntitiesOfClass(tc4revival.content.FollowingItemEntity.class,new AABB(origin).inflate(5)).isEmpty(),"Chainsaw log drops do not gain axe following behavior");
        var sheep=net.minecraft.world.entity.EntityType.SHEEP.create(level);sheep.setPos(Vec3.atCenterOf(origin));sheep.setSheared(false);saw.interactLivingEntity(stack,player,sheep,InteractionHand.MAIN_HAND);test.assertTrue(sheep.isSheared(),"Original entity shearing works without charge");
        stack.getOrCreateTag().putBoolean("shearsMode",false);sheep.setSheared(false);saw.interactLivingEntity(stack,player,sheep,InteractionHand.MAIN_HAND);test.assertTrue(!sheep.isSheared(),"Per-stack no-shear mode suppresses entity shearing");test.succeed();
    }
}
