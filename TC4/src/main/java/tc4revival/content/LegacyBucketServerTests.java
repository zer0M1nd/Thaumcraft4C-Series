package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class LegacyBucketServerTests {
    @GameTest(template="wand_test_empty")
    public static void legacyBucketContainerRoundTrips(GameTestHelper test) {
        for(var item:java.util.List.of(TCItems.PURE_WATER_BUCKET.get(),TCItems.LIQUID_DEATH_BUCKET.get())) {
            var fluid=item==TCItems.PURE_WATER_BUCKET.get()?TCFluids.PURE.get():TCFluids.DEATH.get();
            var handler=FluidUtil.getFluidHandler(new ItemStack(item)).orElseThrow(()->new IllegalStateException("Missing bucket capability"));
            test.assertTrue(handler.getFluidInTank(0).getAmount()==1000&&handler.getFluidInTank(0).getFluid()==fluid,"Full registered fluid bucket");
            test.assertTrue(handler.drain(999,FluidAction.EXECUTE).isEmpty(),"No fractional bucket drain");
            test.assertTrue(handler.drain(1000,FluidAction.SIMULATE).getAmount()==1000&&handler.getContainer().is(item),"Simulation preserves bucket");
            test.assertTrue(handler.drain(new FluidStack(net.minecraft.world.level.material.Fluids.LAVA,1000),FluidAction.EXECUTE).isEmpty(),"Wrong fluid rejected");
            test.assertTrue(handler.drain(1000,FluidAction.EXECUTE).getFluid()==fluid&&handler.getContainer().is(Items.BUCKET),"Drain returns empty vanilla bucket");
            test.assertTrue(handler.drain(1000,FluidAction.EXECUTE).isEmpty(),"Cannot drain consumed bucket twice");
            var empty=FluidUtil.getFluidHandler(new ItemStack(Items.BUCKET)).orElseThrow(()->new IllegalStateException("Missing empty bucket capability"));
            test.assertTrue(empty.fill(new FluidStack(fluid,1000),FluidAction.EXECUTE)==1000&&empty.getContainer().is(item),"Vanilla empty bucket accepts modern fluid mapping");
        }
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));var bucket=TCItems.PURE_WATER_BUCKET.get();
        level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());test.assertTrue(!bucket.placeLiquid(level,pos),"Solid blocks cannot be replaced");
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());test.assertTrue(bucket.placeLiquid(level,pos),"Place source");
        var state=level.getBlockState(pos);
        test.assertTrue(state.is(TCCleansing.PURE_WATER.get())&&state.getValue(LiquidBlock.LEVEL)==0,"Placed exact pure source");
        test.assertTrue(TCCleansing.PURE_WATER.get().pickupBlock(level,pos,state).is(bucket)&&level.isEmptyBlock(pos),"Source collection preserves material");
        var flowing=TCCleansing.PURE_WATER.get().defaultBlockState().setValue(LiquidBlock.LEVEL,1);level.setBlockAndUpdate(pos,flowing);
        test.assertTrue(TCCleansing.PURE_WATER.get().pickupBlock(level,pos,flowing).isEmpty()&&level.getBlockState(pos).equals(flowing),"Flowing liquid cannot be collected or erased");
        test.assertTrue(!bucket.placeLiquid(level,new BlockPos(pos.getX(),level.getMaxBuildHeight(),pos.getZ())),"Reject out of build height");
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"BucketUseTest"));
        level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
        player.moveTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,90);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(bucket));
        var used=bucket.use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND);
        test.assertTrue(used.getObject().is(Items.BUCKET)&&level.getBlockState(pos.above()).is(TCCleansing.PURE_WATER.get()),"Real ray-based survival use returns empty bucket");
        level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);var creative=new ItemStack(bucket);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,creative);
        test.assertTrue(bucket.use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND).getObject()==creative&&level.getBlockState(pos.above()).is(TCCleansing.PURE_WATER.get()),"Creative use keeps filled bucket");
        tc4revival.TC4Revival.LOGGER.info("Legacy bucket test passed: both container round trips, atomic drain, pure source placement and pickup");test.succeed();
    }
}
