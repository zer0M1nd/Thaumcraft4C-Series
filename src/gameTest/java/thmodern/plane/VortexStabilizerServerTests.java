package thmodern.plane;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class VortexStabilizerServerTests {
 @GameTest(template="empty",timeoutTicks=50)
 public static void overlappingStabilizersNeverOverwritePermanentType(GameTestHelper test){var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));level.setBlock(pos,TCArcane.AURA_NODE.get().defaultBlockState(),3);var node=(AuraNodeBlockEntity)level.getBlockEntity(pos);node.configure(new tc4revival.aspect.AspectAmounts().add(tc4revival.aspect.TCAspects.catalog().require("aer"),10),AuraNodeType.HUNGRY,null,"stabilizer-test",false);var left=pos.west(2);var right=pos.east(2);level.setBlock(left,Horizons.VORTEX_STABILIZER.get().defaultBlockState().setValue(VortexStabilizerBlock.FACING,Direction.EAST),3);level.setBlock(right,Horizons.VORTEX_STABILIZER.get().defaultBlockState().setValue(VortexStabilizerBlock.FACING,Direction.WEST),3);level.setBlock(left.below(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);level.setBlock(right.below(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
  test.runAfterDelay(7,()->{test.assertTrue(node.nodeType()==AuraNodeType.NORMAL&&node.permanentType()==AuraNodeType.HUNGRY,"Two beams temporarily normalize without rewriting permanent type");level.removeBlock(left,false);test.assertTrue(node.nodeType()==AuraNodeType.NORMAL,"Other beam remains after one device is removed");thaumcraft.api.nodes.NodeApi.edit(node,thaumcraft.api.nodes.NodeApi.getBase(node),AuraNodeType.UNSTABLE,null);level.removeBlock(right,false);test.assertTrue(node.nodeType()==AuraNodeType.UNSTABLE,"Removal reveals latest permanent edit instead of stale cached type");test.succeed();});
 }
}
