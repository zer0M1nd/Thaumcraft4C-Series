package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class DeepslateOreGenerationServerTests {
    @GameTest(template="tree_test_empty",timeoutTicks=200)
    public static void variantsFollowTheSubstrateAcrossAVein(GameTestHelper t){
        var level=t.getLevel();var chunk=new ChunkPos(t.absolutePos(BlockPos.ZERO));
        int bx=chunk.getMinBlockX(),bz=chunk.getMinBlockZ();
        var origin=new BlockPos(bx,-24,bz);
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=-29;y<-19;y++)
            level.setBlock(new BlockPos(bx+x,y,bz+z),((x+z+y&1)==0?Blocks.STONE:Blocks.DEEPSLATE).defaultBlockState(),2);
        t.assertTrue(ForestOresFeature.vein(level,RandomSource.create(9),origin,TCArcane.ore(2),TCArcane.deepslateOre(2),6),"Mixed-substrate vein is placed below zero");
        int stone=0,deep=0;
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=-29;y<-19;y++){
            var state=level.getBlockState(new BlockPos(bx+x,y,bz+z));
            if(state.is(TCArcane.ore(2))){stone++;t.assertTrue((x+z+y&1)==0,"Normal variant follows stone, including negative Y");}
            if(state.is(TCArcane.deepslateOre(2))){deep++;t.assertTrue((x+z+y&1)!=0,"Deep variant follows deepslate per vein block");}
        }
        t.assertTrue(stone>0&&deep>0,"One vein crosses both substrate types");
        for(int i=0;i<8;i++){
            t.assertTrue(NaturalOreGeneration.replacement(Blocks.DEEPSLATE.defaultBlockState(),i,true).is(TCArcane.deepslateOre(i)),"Each deep variant available");
            t.assertTrue(NaturalOreGeneration.replacement(Blocks.STONE.defaultBlockState(),i,true).is(TCArcane.ore(i)),"Each original variant available");
            t.assertTrue(NaturalOreGeneration.replacement(Blocks.DEEPSLATE.defaultBlockState(),i,false)==null,"Branch off remains stone only");
            for(var protectedBlock:java.util.List.of(Blocks.BEDROCK,Blocks.AIR,Blocks.WATER,Blocks.DIAMOND_ORE))
                t.assertTrue(NaturalOreGeneration.replacement(protectedBlock.defaultBlockState(),i,true)==null,"Does not replace other substrates");
        }
        t.succeed();
    }
    @GameTest(template="tree_test_empty",timeoutTicks=200)
    public static void fullGeneratorPlacesEveryFamilyBelowZero(GameTestHelper t){
        var level=t.getLevel();var chunk=new ChunkPos(t.absolutePos(BlockPos.ZERO));int bx=chunk.getMinBlockX(),bz=chunk.getMinBlockZ();
        var branch=tc4revival.config.TCConfig.SERVER.branchFeatures;boolean previous=branch.get();
        var density=tc4revival.config.TCConfig.SERVER.infusedStoneDensityMultiplier;double previousDensity=density.get();
        try{
            branch.set(true);density.set(1d);
            for(int x=0;x<24;x++)for(int z=0;z<24;z++)for(int y=-60;y<=12;y++)
                level.setBlock(new BlockPos(bx+x,y,bz+z),(y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState(),2);
            int[] surfaces=new int[256];for(int column=0;column<256;column++)surfaces[column]=LegacySurfaceHeight.at(level,bx+(column&15),bz+(column>>4));
            NaturalOreGeneration.generate(level,chunk,RandomSource.create(1056),true);
            int cinnabar=0,amber=0,infused=0;
            for(int x=0;x<24;x++)for(int z=0;z<24;z++)for(int y=-60;y<0;y++){
                var block=level.getBlockState(new BlockPos(bx+x,y,bz+z)).getBlock();
                if(block instanceof DeepslateOreBlock ore){if(ore.metadata()==0)cinnabar++;else if(ore.metadata()==7){amber++;t.assertTrue(x<16&&z<16&&y>=surfaces[x+z*16]-48,"Amber never generates deeper than surface minus 48");}else infused++;}
            }
            t.assertTrue(cinnabar>0&&amber>0&&infused>0,"All three ore families generated in negative-height deepslate");
        }finally{branch.set(previous);density.set(previousDensity);}
        t.succeed();
    }
    @GameTest(template="tree_test_empty",timeoutTicks=200)
    public static void zeroDensityDisablesOnlyInfusedOre(GameTestHelper t){
        var level=t.getLevel();var chunk=new ChunkPos(t.absolutePos(BlockPos.ZERO));int bx=chunk.getMinBlockX(),bz=chunk.getMinBlockZ();
        var config=tc4revival.config.TCConfig.SERVER;boolean previous=config.branchFeatures.get();double density=config.infusedStoneDensityMultiplier.get();
        try{
            config.branchFeatures.set(true);config.infusedStoneDensityMultiplier.set(0d);
            for(int x=0;x<24;x++)for(int z=0;z<24;z++)for(int y=-60;y<=12;y++)
                level.setBlock(new BlockPos(bx+x,y,bz+z),(y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState(),2);
            NaturalOreGeneration.generate(level,chunk,RandomSource.create(1056),true);
            int other=0;
            for(int x=0;x<24;x++)for(int z=0;z<24;z++)for(int y=-60;y<=12;y++){
                var block=level.getBlockState(new BlockPos(bx+x,y,bz+z)).getBlock();
                if(block instanceof LegacyOreBlock ore){t.assertTrue(ore.metadata()==0||ore.metadata()==7,"Zero density prevents all six infused variants");other++;}
            }
            t.assertTrue(other>0,"Cinnabar/amber still generate");
        }finally{config.branchFeatures.set(previous);config.infusedStoneDensityMultiplier.set(density);}
        t.succeed();
    }
}
