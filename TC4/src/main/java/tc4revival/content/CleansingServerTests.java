package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraftforge.gametest.*;
import tc4revival.research.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class CleansingServerTests {
    private CleansingServerTests() {}
    @GameTest(template="wand_test_empty",timeoutTicks=300)
    public static void bathSaltsExpireIntoOneSourceAndGiveWardOnce(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,2,3));
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
        for (var side:net.minecraft.core.Direction.Plane.HORIZONTAL) level.setBlock(pos.relative(side),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(pos,Blocks.WATER.defaultBlockState(),3);
        var salts=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY()+.1,pos.getZ()+.5,new ItemStack(TCCleansing.BATH_SALTS.get(),5));
        salts.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); level.addFreshEntity(salts);
        test.runAfterDelay(205,()->{
            test.assertTrue(salts.isRemoved() && level.getBlockState(pos).is(TCCleansing.PURE_WATER.get()),"Entire salt stack must expire into one purifying source; removed="+salts.isRemoved()+", itemPos="+salts.position()+", bathPos="+pos+", block="+level.getBlockState(pos));
            test.assertTrue(TCCleansing.PURE_WATER.get().pickupBlock(level,pos,level.getBlockState(pos)).is(TCItems.PURE_WATER_BUCKET.get())
                    && level.isEmptyBlock(pos),"Legacy source pickup returns the purifying bucket");
            test.assertTrue(TCItems.PURE_WATER_BUCKET.get().placeLiquid(level,pos),"Collected bath can be placed again");
            var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BathTest"));
            var data=KnowledgeSavedData.get(level.getServer()); data.update(player.getUUID(),level.random::nextInt,knowledge->{knowledge.setWarp(PlayerKnowledge.WarpKind.PERMANENT,100); return null;});
            TCCleansing.PURE_WATER.get().entityInside(level.getBlockState(pos),level,pos,player);
            test.assertTrue(player.getEffect(TCEffects.WARP_WARD.get()).getDuration()==20000 && level.isEmptyBlock(pos),"Permanent warp scales ward duration and consumes source");
            level.setBlock(pos,TCCleansing.PURE_WATER.get().defaultBlockState(),3);
            TCCleansing.PURE_WATER.get().entityInside(level.getBlockState(pos),level,pos,player);
            test.assertTrue(level.getBlockState(pos).is(TCCleansing.PURE_WATER.get()),"Existing ward must preserve the next source");
            player.removeEffect(TCEffects.WARP_WARD.get());
            var flowing=TCCleansing.PURE_WATER.get().defaultBlockState().setValue(LiquidBlock.LEVEL,1); level.setBlock(pos,flowing,3);
            TCCleansing.PURE_WATER.get().entityInside(flowing,level,pos,player);
            test.assertTrue(!player.hasEffect(TCEffects.WARP_WARD.get()),"Flowing water does not grant ward");
            pass(test,"bath-expiry-and-ward");
        });
    }
    @GameTest(template="wand_test_empty")
    public static void soapCompletionClearsTemporaryWarpWithoutRemovingPermanentWarp(GameTestHelper test) {
        var level=test.getLevel(); var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"SoapTest"));
        var data=KnowledgeSavedData.get(level.getServer());
        data.update(player.getUUID(),level.random::nextInt,knowledge->{ knowledge.setWarp(PlayerKnowledge.WarpKind.PERMANENT,40); knowledge.setWarp(PlayerKnowledge.WarpKind.TEMPORARY,9); knowledge.setWarpCounter(1); return null; });
        var soap=new ItemStack(TCCleansing.SANITY_SOAP.get(),2);
        soap.getItem().releaseUsing(soap,level,player,5);
        test.assertTrue(soap.getCount()==2 && data.snapshot(player.getUUID()).orElseThrow().warp(PlayerKnowledge.WarpKind.TEMPORARY)==9,"195 ticks must not consume soap or clear warp");
        soap.getItem().releaseUsing(soap,level,player,4);
        var result=data.snapshot(player.getUUID()).orElseThrow();
        test.assertTrue(soap.getCount()==1 && result.warp(PlayerKnowledge.WarpKind.TEMPORARY)==0 && result.warp(PlayerKnowledge.WarpKind.PERMANENT)==40 && result.warpCounter()==40,"196 ticks clears temporary warp and resets event counter");
        pass(test,"soap-duration-and-warp");
    }
    private static void pass(GameTestHelper test,String name) { com.mojang.logging.LogUtils.getLogger().info("Cleansing system test passed: {}",name); test.succeed(); }
}
