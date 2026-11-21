package emtmodern.test;
import emtmodern.ElectroMagicTools;
import emtmodern.energy.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;
import tc4revival.content.*;

@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class GenerationServerTests {
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void generatorsDrainEachMatchingContainerOnce(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(4,3,4));test.getLevel().setBlockAndUpdate(pos,ElectroMagicTools.GENERATORS.get("potentia_generator").get().defaultBlockState());
        var generator=(EssentiaGeneratorBlockEntity)test.getLevel().getBlockEntity(pos);
        var power=TCAspects.catalog().require("potentia");
        for(int x:new int[]{-4,3,4}){test.getLevel().setBlockAndUpdate(pos.offset(x,0,0),TCArcane.ESSENTIA_JAR.get().defaultBlockState());((EssentiaJarBlockEntity)test.getLevel().getBlockEntity(pos.offset(x,0,0))).addToContainer(power,10);}
        test.assertTrue(generator.generate()==2,"Legacy search is inclusive -4 but exclusive +4");
        test.assertTrue(((EssentiaJarBlockEntity)test.getLevel().getBlockEntity(pos.offset(-4,0,0))).amount()==9,"Exactly one unit from each jar");
        test.assertTrue(((EssentiaJarBlockEntity)test.getLevel().getBlockEntity(pos.offset(4,0,0))).amount()==10,"No out-of-range drain");
        test.assertTrue(Math.abs(generator.energy.getEnergyStored()-2*EmtEnergyConfig.essentiaOutput(power)/30)<.001,"Author output divided by thirty");
        var restored=new EssentiaGeneratorBlockEntity(pos,generator.getBlockState());restored.load(generator.saveWithoutMetadata());
        test.assertTrue(restored.energy.getEnergyStored()==generator.energy.getEnergyStored(),"Energy survives save/load");test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty",timeoutTicks=140)
    public static void solarFeedsRealIndustrialCraftEnergyNet(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(2,3,2));test.getLevel().setBlockAndUpdate(pos,ElectroMagicTools.SOLARS.get("compressed_solar").get().defaultBlockState());
        var solar=(SolarBlockEntity)test.getLevel().getBlockEntity(pos);
        var batteryPos=pos.east();test.getLevel().setBlockAndUpdate(batteryPos,ic2.core.ref.Ic2Blocks.MFSU.defaultBlockState());
        solar.energy.addEnergy(5000);
        test.runAfterDelay(100,()->{
            test.assertTrue(test.getLevel().getBlockEntity(batteryPos) instanceof ic2.core.block.wiring.tileentity.TileEntityElectricMFSU,"MFSU remains intact at source tier four");
            var battery=(ic2.core.block.wiring.tileentity.TileEntityElectricMFSU)test.getLevel().getBlockEntity(batteryPos);
            test.assertTrue(battery.energy.getEnergy()>0,"Actual IC2 energy network received EU from addon source");
            test.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());test.succeed();
        });
    }
}
