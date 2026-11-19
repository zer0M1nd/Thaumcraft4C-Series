package thmodern.node;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thmodern.*;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class NodeDeviceServerTests {
    @GameTest(template="empty")
    public static void monitorReportsDepletionAndRecovers(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(2,3,2));var level=test.getLevel();var air=TCAspects.catalog().require("aer");
        level.setBlock(pos,TCArcane.AURA_NODE.get().defaultBlockState(),3);
        var node=(AuraNodeBlockEntity)level.getBlockEntity(pos);
        node.configure(new AspectAmounts().add(air,2),AuraNodeType.NORMAL,null,"monitor-test",false);
        var monitorPos=pos.above();level.setBlock(monitorPos,Horizons.NODE_MONITOR.get().defaultBlockState().setValue(NodeMonitorBlock.FACING,Direction.UP),3);
        var monitor=(NodeMonitorEntity)level.getBlockEntity(monitorPos);monitor.tick();
        test.assertTrue(!level.getBlockState(monitorPos).getValue(NodeMonitorBlock.ACTIVE),"Two points does not alarm");
        node.take(air,1);monitor.tick();
        test.assertTrue(level.getSignal(monitorPos,Direction.UP)==15,"One point emits redstone");
        thaumcraft.api.nodes.NodeApi.insert(node,thaumcraft.api.aspects.Aspect.AIR,1,false);monitor.tick();
        test.assertTrue(level.getSignal(monitorPos,Direction.UP)==0,"Replenishment clears alarm");
        level.removeBlock(pos,false);monitor.tick();test.assertTrue(level.isEmptyBlock(monitorPos),"Removing node removes monitor");test.succeed();
    }
    @GameTest(template="empty")
    public static void essentiaDynamoRejectsInvalidFuelAndSharesTickBudget(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var air=TCAspects.catalog().require("aer");
        level.setBlock(pos,Horizons.ESSENTIA_DYNAMO.get().defaultBlockState(),3);var dynamo=(EssentiaDynamoEntity)level.getBlockEntity(pos);
        test.assertTrue(dynamo.addEssentia(air,0,Direction.DOWN)==0,"Zero fuel cannot start a source");
        test.assertTrue(dynamo.addEssentia(air,1,Direction.UP)==0,"Top is not an input");
        test.assertTrue(dynamo.addEssentia(air,1,Direction.DOWN)==1,"One unit starts source");
        test.assertTrue(dynamo.consumeVis(air,8)==8&&dynamo.consumeVis(air,8)==4,"Consumers share 12 cV per world tick");
        var saved=dynamo.saveWithoutMetadata();dynamo.load(saved);
        test.assertTrue(dynamo.consumeVis(air,1)==0,"Reload cannot duplicate spent budget");
        test.assertTrue(dynamo.addEssentia(air,1,Direction.DOWN)==0,"Cannot overfill burn interval");
        test.runAfterDelay(1,()->{test.assertTrue(dynamo.consumeVis(air,20)==12,"New world tick replenishes output");test.succeed();});
    }
    @GameTest(template="empty")
    public static void syntheticEssenceCapacitySurvivesSave(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(2,2,2));var level=test.getLevel();var air=TCAspects.catalog().require("aer");
        level.setBlock(pos,Horizons.SYNTHETIC_NODE.get().defaultBlockState(),3);var node=(SyntheticNodeEntity)level.getBlockEntity(pos);
        var essence=TCItems.WISP_ESSENCE.get().of(air);essence.setCount(3);
        test.assertTrue(node.addEssence(essence)&&essence.isEmpty(),"Consumes actual essence");
        test.assertTrue(node.capacity().amount(air)==12&&node.aspects().amount(air)==0,"Capacity starts uncharged");
        var tag=node.saveWithoutMetadata();node.load(tag);
        test.assertTrue(node.capacity().amount(air)==12&&node.aspects().amount(air)==0,"Capacity persists without filling charge");test.succeed();
    }
}
