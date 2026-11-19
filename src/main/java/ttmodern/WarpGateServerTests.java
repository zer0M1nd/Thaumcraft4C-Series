package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class WarpGateServerTests {
    @GameTest(template="wand_test_empty") public static void tinkererWarpGateInventoryAndPearlPersistence(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(2,1,2));test.getLevel().setBlock(pos,Tinkerer.WARP_GATE.get().defaultBlockState(),3);var gate=(WarpGateEntity)test.getLevel().getBlockEntity(pos);
        var pearl=new ItemStack(Tinkerer.SKY_PEARL.get(),2);SkyPearlItem.bind(pearl,test.getLevel(),new BlockPos(14,-40,29));pearl.setHoverName(net.minecraft.network.chat.Component.literal("Lower world"));
        test.assertTrue(gate.items.insertItem(0,pearl,false).getCount()==1&&gate.items.getStackInSlot(0).getCount()==1,"One pearl per slot, remainder retained");test.assertTrue(!gate.items.insertItem(1,new ItemStack(Items.DIAMOND),false).isEmpty(),"Gate rejects ordinary items");gate.locked(true);
        var copy=new WarpGateEntity(pos,gate.getBlockState());copy.load(gate.saveWithoutMetadata());test.assertTrue(copy.locked()&&copy.items.getSlots()==10&&SkyPearlItem.position(copy.items.getStackInSlot(0)).getY()==-40&&copy.items.getStackInSlot(0).getHoverName().getString().equals("Lower world"),"Lock, name, dimension and negative Y survive saving");
        var malformed=gate.saveWithoutMetadata();malformed.getCompound("Inventory").putInt("Size",300);copy.load(malformed);test.assertTrue(copy.items.getSlots()==10,"Malformed saved size cannot resize the fixed gate inventory");
        var capability=gate.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);test.assertTrue(capability.isPresent(),"Automation can access pearls");gate.invalidateCaps();test.assertTrue(!capability.isPresent(),"Removed gate invalidates automation handles");gate.reviveCaps();test.assertTrue(gate.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).isPresent(),"Revived gate supplies a fresh handle");test.getLevel().removeBlock(pos,false);test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererWarpGateValidatesDestinationAndCaller(GameTestHelper test){
        var level=test.getLevel();var source=test.absolutePos(new BlockPos(1,1,1));var destination=source.offset(4,0,0);level.setBlock(source,Tinkerer.WARP_GATE.get().defaultBlockState(),3);level.setBlock(destination,Tinkerer.WARP_GATE.get().defaultBlockState(),3);for(int y=1;y<=3;y++)level.removeBlock(destination.above(y),false);
        var gate=(WarpGateEntity)level.getBlockEntity(source);var target=(WarpGateEntity)level.getBlockEntity(destination);var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTWarp")){
            @Override public void teleportTo(double x,double y,double z){setPos(x,y,z);}
            @Override public void closeContainer(){}
        };player.setPos(source.getX()+.5,source.getY()+1,source.getZ()+.5);var pearl=new ItemStack(Tinkerer.SKY_PEARL.get());SkyPearlItem.bind(pearl,level,destination);gate.items.setStackInSlot(0,pearl);
        test.assertTrue(!gate.teleport(player,-1)&&!gate.teleport(player,10),"Packet indices are bounded");target.locked(true);test.assertTrue(!gate.teleport(player,0),"Incoming lock rejects transfer");target.locked(false);
        pearl.getTag().putString("Dimension","minecraft:the_nether");test.assertTrue(!gate.teleport(player,0),"Pearls from another dimension cannot target matching coordinates");SkyPearlItem.bind(pearl,level,destination);
        level.setBlock(destination.above(2),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);test.assertTrue(!gate.teleport(player,0),"Blocked landing rejects teleport");level.removeBlock(destination.above(2),false);
        player.setPos(source.getX()+3,source.getY()+1,source.getZ()+.5);test.assertTrue(!gate.teleport(player,0),"Nearby player still must be standing on the source gate");player.setPos(source.getX()+.5,source.getY()+1,source.getZ()+.5);
        test.assertTrue(gate.teleport(player,0)&&player.distanceToSqr(destination.getX()+.5,destination.getY()+1.6,destination.getZ()+.5)<.001,"Valid pearl teleports to the gate without consuming it");player.setPos(source.getX()+.5,source.getY()+1,source.getZ()+.5);test.assertTrue(!gate.teleport(player,0)&&gate.items.getStackInSlot(0).getCount()==1,"Same-tick duplicate is rejected and pearl retained");level.removeBlock(source,false);level.removeBlock(destination,false);test.succeed();
    }
}
