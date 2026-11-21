package timodern;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.level.block.Blocks;
import tc4revival.aspect.TCAspects;
import tc4revival.content.*;

@GameTestHolder("thaumicinfusion") @PrefixGameTestTemplate(false)
public class InfusionAcquisitionServerTests {
    @GameTest(template="test_empty") public static void selectedRemovalPreservesOriginalFee(GameTestHelper test) {
        var level=test.getLevel();var pos=InfusionServerTests.put(test,"lux","sensus");
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(level,
            new com.mojang.authlib.GameProfile(java.util.UUID.fromString("573a2ae2-b8a3-4617-b01c-c546d45f0695"),"TIFeeTest"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos.above()));
        var jarPos=pos.north(2);level.setBlockAndUpdate(jarPos,TCArcane.ESSENTIA_JAR.get().defaultBlockState());
        var jar=(EssentiaJarBlockEntity)level.getBlockEntity(jarPos);var lux=TCAspects.catalog().require("lux");
        test.assertTrue(!InfusingFocus.apply(level,pos,player,"lux")&&InfusionData.has(level,pos,"lux"),
            "Original multi-effect removal needs another fee and leaves effects unchanged if unavailable");
        jar.addToContainer(lux,4);
        test.assertTrue(InfusingFocus.apply(level,pos,player,"lux")&&jar.amount()==3&&!InfusionData.has(level,pos,"lux")&&InfusionData.has(level,pos,"sensus"),
            "Selected multi-effect removal consumes its original fee without refund");
        test.assertTrue(InfusingFocus.apply(level,pos,player,"sensus")&&InfusionData.at(level,pos)==null&&jar.amount()==3,
            "Selecting the final effect removes it without charging or refunding");
        InfusionServerTests.put(test,"lux");
        test.assertTrue(InfusingFocus.apply(level,pos,player,"")&&jar.amount()==4,
            "Only clearing with no selected aspect follows the original refund path");
        jar.empty();InfusionData.remove(level,pos);test.succeed();
    }
    @GameTest(template="test_empty") public static void gelumKeepsOriginalFirstCandidate(GameTestHelper test) {
        var level=test.getLevel();var base=InfusionServerTests.put(test,"gelum");var center=base.above(20);var first=center.offset(-10,-10,-10);
        level.setBlockAndUpdate(first,Blocks.WATER.defaultBlockState());InfusionRuntime.freeze(level,center);
        test.assertTrue(level.getBlockState(first).is(Blocks.ICE),"Original first candidate water becomes ice");
        level.setBlockAndUpdate(first,Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(first.above(),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(center.east(),Blocks.WATER.defaultBlockState());InfusionRuntime.freeze(level,center);
        test.assertTrue(level.getBlockState(center.east()).is(Blocks.WATER),"4.32 ordinary early-return bug is preserved; no invented nearest-water search");
        InfusionData.remove(level,base);test.succeed();
    }
}
