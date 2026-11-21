package emtmodern.test;

import emtmodern.ElectroMagicTools;
import emtmodern.item.*;
import ic2.api.item.ElectricItem;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class ElectricToolServerTests {
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void scribingChargeAndInkShareOneReservoir(GameTestHelper test){
        ItemStack stack=new ItemStack(ElectroMagicTools.SCRIBING.get());var pen=(ElectricScribingTools)stack.getItem();
        test.assertTrue(ElectricItem.manager.charge(stack,200,0,false,false)==0,"Reject lower tier");
        test.assertTrue(ElectricItem.manager.charge(stack,200,1,false,false)==5,"Respect five EU per operation");
        pen.setInk(stack,400);test.assertTrue(pen.getInk(stack)==400,"Full charge is exactly 400 ink");
        pen.setInk(stack,399);test.assertTrue(ElectricItem.manager.getCharge(stack)==399,"Research consumes actual EU");
        pen.setInk(stack,-100);test.assertTrue(pen.getInk(stack)==0,"No negative ink");
        pen.setInk(stack,1000);test.assertTrue(pen.getInk(stack)==400,"No overcharge");
        ItemStack restored=ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag()));test.assertTrue(pen.getInk(restored)==400,"Charge persists");test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void electricToolsPayPerBlock(GameTestHelper test){
        var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"EMTTest"));
        for(ToolProfile profile:ToolProfile.ALL){
            ItemStack stack=new ItemStack(ElectroMagicTools.CONTENT.get(profile.id()).get());Item item=stack.getItem();
            var state=profile.pick()?Blocks.STONE.defaultBlockState():Blocks.OAK_LOG.defaultBlockState();
            test.assertTrue(item.getDestroySpeed(stack,state)==1,"Empty tool is slow: "+profile.id());
            ElectricItem.manager.charge(stack,profile.capacity(),Integer.MAX_VALUE,true,false);
            test.assertTrue(item.getDestroySpeed(stack,state)==profile.speed(),"Charged tool uses original speed");
            item.mineBlock(stack,test.getLevel(),state,test.absolutePos(new BlockPos(2,2,2)),player);
            test.assertTrue(ElectricItem.manager.getCharge(stack)==profile.capacity()-profile.breakCost(),"One block costs exact original EU");
        }test.succeed();
    }
}
