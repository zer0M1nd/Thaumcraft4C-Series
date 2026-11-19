package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FluxSpillServerTests {
    @GameTest(template="wand_test_empty")
    public static void fluxSpillsOnlyIncreaseLegacyPollutionFluids(GameTestHelper test) {
        var level=test.getLevel();var origin=test.absolutePos(new BlockPos(2,2,2));var above=origin.above();
        long hit=seed(true,false),miss=seed(false,false),east=seed(true,true);
        try {
            for(var block:java.util.List.of(TCArcane.FLUX_GAS.get(),TCArcane.FLUX_GOO.get(),TCArcane.LIQUID_DEATH.get())) for(int amount=1;amount<=9;amount++) {
                for(var pos:BlockPos.betweenClosed(origin.offset(-1,-1,-1),origin.offset(1,1,1)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
                var initial=block.defaultBlockState().setValue(FiniteFluidBlock.QUANTA,amount);level.setBlock(above,initial,2);
                FluxSpills.spill(level,origin,RandomSource.create(miss));test.assertTrue(level.getBlockState(above).equals(initial),"Failed 1/4 draw leaves fluid unchanged");
                FluxSpills.spill(level,origin,RandomSource.create(hit));
                int expected=block!=TCArcane.LIQUID_DEATH.get()&&amount<8?amount+1:amount;
                test.assertTrue(level.getBlockState(above).equals(initial.setValue(FiniteFluidBlock.QUANTA,expected)),"Only gas/goo below eight increase; death is never created: "+block+" "+amount);
            }
            // An obstructed upper cell falls back to the exact legacy x,y,z draw sequence.
            level.setBlock(above,TCArcane.LIQUID_DEATH.get().defaultBlockState(),2);level.setBlock(origin.east(),Blocks.AIR.defaultBlockState(),2);
            var expectedRandom=RandomSource.create(east);expectedRandom.nextInt(4);for(int i=0;i<3;i++)expectedRandom.nextInt(3);boolean gas=expectedRandom.nextBoolean();
            FluxSpills.spill(level,origin,RandomSource.create(east));
            test.assertTrue(level.getBlockState(origin.east()).is(gas?TCArcane.FLUX_GAS.get():TCArcane.FLUX_GOO.get()) && level.getBlockState(above).getValue(FiniteFluidBlock.QUANTA)==1,"Unrelated upper fluid uses neighbor fallback without increasing itself");
            for(long value=0;value<20;value++) {
                var random=RandomSource.create(value);int draw=random.nextInt(4);boolean chooseGas=draw==0&&random.nextBoolean();
                level.setBlock(above,Blocks.AIR.defaultBlockState(),2);FluxSpills.spill(level,origin,RandomSource.create(value));
                test.assertTrue(draw==0?level.getBlockState(above).is(chooseGas?TCArcane.FLUX_GAS.get():TCArcane.FLUX_GOO.get()):level.isEmptyBlock(above),"Air placement preserves probability and gas/goo random choice");
            }
        } finally {for(var pos:BlockPos.betweenClosed(origin.offset(-1,-1,-1),origin.offset(1,1,1)))level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
        tc4revival.TC4Revival.LOGGER.info("PASS fluxSpillsOnlyIncreaseLegacyPollutionFluids: 27 states with hit/miss, death exclusion, neighbor fallback and 20 air draws");test.succeed();
    }
    private static long seed(boolean hit,boolean east) {
        for(long seed=0;seed<10000;seed++){var random=RandomSource.create(seed);if((random.nextInt(4)==0)!=hit)continue;if(!east||random.nextInt(3)==2&&random.nextInt(3)==1&&random.nextInt(3)==1)return seed;}
        throw new IllegalStateException("No fixture seed");
    }
}
