package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.research.ResearchTableBonuses;
import tc4revival.research.ResearchTableBonuses.Source;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ResearchEnvironmentSourceServerTests {
    @GameTest(template="wand_test_empty")
    public static void baseModEnvironmentSourcesMatchLegacyMaterialsAndMetadata(GameTestHelper test) {
        var pos=test.absolutePos(new BlockPos(2,2,2));
        var environment=new ResearchTableEnvironment(test.getLevel(),pos);
        String[] primals={"aer","ignis","aqua","terra","ordo","perditio"};
        int count=0;
        try {
            for(int md=0;md<8;md++) count+=check(test,pos,environment,TCArcane.ore(md),md>=1&&md<=6?Source.primal(primals[md-1],20):Source.NONE);
            for(var type:ShardType.values()) count+=check(test,pos,environment,TCArcane.crystal(type),type==ShardType.BALANCED?Source.NONE:Source.primal(primals[type.metadata()],10));
            for(int color=0;color<16;color++) count+=check(test,pos,environment,TCArcane.candle(color),Source.primal("ordo",20));
            count+=check(test,pos,environment,TCArcane.BRAIN_JAR.get(),Source.brainJar());
            for(var block:List.of(TCArcane.ESSENTIA_JAR.get(),TCArcane.VOID_JAR.get(),TCArcane.NODE_JAR.get(),TCArcane.FLUX_GOO.get(),TCArcane.FLUX_GAS.get()))
                count+=check(test,pos,environment,block,Source.NONE);
            count+=check(test,pos,environment,TCArcane.LIQUID_DEATH.get(),Source.primal("aqua",15));
            count+=check(test,pos,environment,TCCleansing.PURE_WATER.get(),Source.primal("aqua",15));
            tc4revival.TC4Revival.LOGGER.info("PASS baseModEnvironmentSourcesMatchLegacyMaterialsAndMetadata: {} actual block states and reward paths checked",count);
        } finally { test.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),2); }
        test.succeed();
    }
    private static int check(GameTestHelper test,BlockPos pos,ResearchTableEnvironment world,Block block,Source expected) {
        int count=0;
        for(var state:block.getStateDefinition().getPossibleStates()) {
            // A jar derives its light state from its real contents during onLoad.
            // Recreate each fixture so a previous filled jar cannot leak into the empty case.
            test.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            test.getLevel().setBlock(pos,state,2);
            if(test.getLevel().getBlockEntity(pos) instanceof EssentiaJarBlockEntity jar
                    && state.hasProperty(EssentiaJarBlock.LIT) && state.getValue(EssentiaJarBlock.LIT))
                jar.addToContainer(tc4revival.aspect.TCAspects.catalog().require("aer"),1);
            test.assertTrue(test.getLevel().getBlockState(pos).equals(state),"Fixture state installed: "+state);
            test.assertTrue(world.at(0,0,0).equals(expected),"Legacy source mapping: "+state+" got "+world.at(0,0,0)+" expected "+expected);
            // Isolate the real block lookup from altitude/night and unrelated surrounding sources.
            var isolated=new ResearchTableBonuses.Environment() {
                public int tableY(){return 32;} public int height(){return 256;}
                public boolean darkCoveredNight(){return false;}
                public Source at(int x,int y,int z){return x==0&&y==0&&z==0?world.at(0,0,0):Source.NONE;}
            };
            var draws=new ArrayList<Integer>();
            var reward=ResearchTableBonuses.recalculate(Set.of(),isolated,List.of("lux","motus"),bound->{draws.add(bound);return 0;});
            var wanted=expected.aspect()!=null?Set.of(expected.aspect()):expected.randomAspectChance()>0?Set.of("lux"):Set.<String>of();
            var bounds=expected.aspect()!=null?List.of(expected.chance()):expected.randomAspectChance()>0?List.of(expected.randomAspectChance(),2):List.<Integer>of();
            test.assertTrue(reward.equals(wanted)&&draws.equals(bounds),"Reward and random bounds: "+state);
            count++;
        }
        return count;
    }
}
