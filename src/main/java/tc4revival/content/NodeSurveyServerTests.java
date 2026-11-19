package tc4revival.content;

import java.lang.reflect.Proxy;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class NodeSurveyServerTests {
    @GameTest(template="wand_test_empty")
    public static void sectionSurveyMatchesReferenceAtBoundaries(GameTestHelper h) {
        var level=h.getLevel();var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        int scenarios=0;
        for(var center:List.of(new BlockPos(0,64,0),new BlockPos(-16,63,-16),new BlockPos(15,-64,15),new BlockPos(-1,319,-1))) {
            var chunks=new HashMap<ChunkPos,ProtoChunk>();
            for(int x=(center.getX()-5)>>4;x<=(center.getX()+5)>>4;x++)
                for(int z=(center.getZ()-5)>>4;z<=(center.getZ()+5)>>4;z++) {
                    var cp=new ChunkPos(x,z);
                    chunks.put(cp,new ProtoChunk(cp,UpgradeData.EMPTY,level,registry,null));
                }
            var states=List.of(Blocks.AIR.defaultBlockState(),Blocks.WATER.defaultBlockState(),Blocks.LAVA.defaultBlockState(),Blocks.STONE.defaultBlockState(),Blocks.OAK_LEAVES.defaultBlockState());
            int i=0;
            for(var pos:BlockPos.betweenClosed(center.offset(-5,-5,-5),center.offset(5,5,5))) {
                if(!level.isOutsideBuildHeight(pos))chunks.get(new ChunkPos(pos)).setBlockState(pos,states.get((i++)%states.size()),false);
            }
            // Both fully available and partially unavailable regions must avoid loading neighbours.
            for(boolean partial:List.of(false,true)) {
                if(partial)chunks.remove(chunks.keySet().iterator().next());
                int[] calls={0};
                var region=(WorldGenLevel)Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),new Class<?>[]{WorldGenLevel.class},(proxy,method,args)->switch(method.getName()) {
                    // Proxy method names are SRG in the isolated production-JAR harness.
                    case "getMinBuildHeight", "m_141937_" -> level.getMinBuildHeight();
                    case "getMaxBuildHeight", "m_151558_" -> level.getMaxBuildHeight();
                    case "hasChunk", "m_7232_" -> chunks.containsKey(new ChunkPos((int)args[0],(int)args[1]));
                    case "getChunk", "m_6325_", "m_6522_" -> {calls[0]++;var chunk=chunks.get(new ChunkPos((int)args[0],(int)args[1]));if(chunk==null)throw new AssertionError("Requested unavailable chunk");yield chunk;}
                    default -> throw new AssertionError("Unexpected world access: "+method);
                });
                int water=0,lava=0,stone=0,foliage=0;
                for(var pos:BlockPos.betweenClosed(center.offset(-5,-5,-5),center.offset(5,5,5))) {
                    var chunk=chunks.get(new ChunkPos(pos));if(chunk==null||level.isOutsideBuildHeight(pos))continue;
                    var state=chunk.getBlockState(pos);
                    if(state.is(Blocks.WATER))water++;else if(state.is(Blocks.LAVA))lava++;else if(state.is(Blocks.STONE))stone++;
                    if(state.is(BlockTags.LEAVES))foliage++;
                }
                var expected=new AuraNodeGeneration.Environment(water,lava,stone,foliage);
                h.assertTrue(AuraNodeGeneration.survey(region,center).equals(expected),"Counts preserved at "+center+", partial="+partial);
                h.assertTrue(calls[0]<=4,"At most four chunk lookups per survey");scenarios++;
            }
        }
        h.assertTrue(scenarios==8,"Positive/negative chunk edges and both height limits covered");h.succeed();
    }
}
