package emtmodern.test;
import emtmodern.ElectroMagicTools;
import emtmodern.energy.ElectricCloudCreation;
import ic2.api.item.ElectricItem;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.gametest.*;
@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class ElectricCloudServerTests {
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void ic2BatteryElectrolyzesNitorAtOriginalTransferRate(GameTestHelper test){
        var level=test.getLevel();var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"CloudTest"));var pos=test.absolutePos(new BlockPos(3,3,3));var battery=new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("ic2:energy_crystal")));ElectricItem.manager.charge(battery,1024,4,true,false);player.setItemInHand(InteractionHand.MAIN_HAND,battery);var hit=new BlockHitResult(pos.getCenter(),Direction.UP,pos,false);
        level.setBlockAndUpdate(pos,tc4revival.content.TCArcane.NITOR.get().defaultBlockState());ElectricCloudCreation.interact(new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,pos,hit));
        test.assertTrue(level.getBlockState(pos).is(ElectroMagicTools.CLOUD.get()),"Real IC2 battery converts nitor");test.assertTrue(ElectricItem.manager.getCharge(battery)==768,"Conversion spends exactly 256 EU");test.assertTrue(level.getBlockState(pos).getCollisionShape(level,pos).isEmpty()&&level.getBlockState(pos).getLightEmission()==15,"Original intangible full-bright cloud");
        level.setBlockAndUpdate(pos,tc4revival.content.TCArcane.NITOR.get().defaultBlockState());var partial=battery.copy();ElectricItem.manager.discharge(partial,Double.MAX_VALUE,4,true,true,false);ElectricItem.manager.charge(partial,128,4,true,false);player.setItemInHand(InteractionHand.MAIN_HAND,partial);ElectricCloudCreation.interact(new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,pos,hit));
        test.assertTrue(level.getBlockState(pos).is(tc4revival.content.TCArcane.NITOR.get())&&ElectricItem.manager.getCharge(partial)==0,"Original partial discharge consumes energy without successful conversion");test.succeed();
    }
}
