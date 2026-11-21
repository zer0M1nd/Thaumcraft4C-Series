package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class HellbatWeatherServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void fireBatWorldWaterRainAndRoofRespectWetDamage(GameTestHelper test) {
        var level=test.getLevel();var anchor=test.absolutePos(new BlockPos(5,20,5));var base=new BlockPos(anchor.getX(),150,anchor.getZ());
        var forced=new ArrayList<ChunkPos>();for(int x=(base.getX()-2)>>4;x<=(base.getX()+26)>>4;x++)for(int z=(base.getZ()-2)>>4;z<=(base.getZ()+2)>>4;z++)if(level.setChunkForced(x,z,true))forced.add(new ChunkPos(x,z));
        var saved=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();var bats=new ArrayList<FireBatEntity>();
        var savedBiomes=new HashMap<BlockPos,net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>>();
        var biomeChunks=new ArrayList<net.minecraft.world.level.chunk.LevelChunk>();
        boolean raining=level.getLevelData().isRaining();float rainLevel=level.getRainLevel(1);
        test.runAfterDelay(20,()->{
            var plains=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
            for(int cx=(base.getX()-2)>>4;cx<=(base.getX()+26)>>4;cx++)for(int cz=(base.getZ()-2)>>4;cz<=(base.getZ()+2)>>4;cz++){
                var chunk=level.getChunk(cx,cz);biomeChunks.add(chunk);
                for(int qx=cx*4;qx<cx*4+4;qx++)for(int qz=cz*4;qz<cz*4+4;qz++)for(int qy=level.getMinBuildHeight()>>2;qy<level.getMaxBuildHeight()>>2;qy++)
                    savedBiomes.put(new BlockPos(qx,qy,qz),chunk.getNoiseBiome(qx,qy,qz));
                chunk.fillBiomesFromNoise((x,y,z,s)->plains,level.getChunkSource().randomState().sampler());
            }
            for(var p:BlockPos.betweenClosed(base.offset(-2,-1,-2),base.offset(2,3,2))) {
                saved.put(p.immutable(),level.getBlockState(p));boolean wall=Math.abs(p.getX()-base.getX())==2||Math.abs(p.getZ()-base.getZ())==2||p.getY()==base.getY()-1||p.getY()==base.getY()+3;
                level.setBlock(p,wall?Blocks.GLASS.defaultBlockState():Blocks.WATER.defaultBlockState(),2);
            }
            for(var p:BlockPos.betweenClosed(base.offset(22,3,-2),base.offset(26,3,2))){saved.put(p.immutable(),level.getBlockState(p));level.setBlock(p,Blocks.STONE.defaultBlockState(),2);}
            level.getLevelData().setRaining(true);level.setRainLevel(1);
            for(int x:new int[]{0,12,24}) {var bat=new FireBatEntity(TCEntities.FIRE_BAT.get(),level);bat.setNoAi(true);bat.moveTo(Vec3.atBottomCenterOf(base.east(x)));bats.add(bat);level.addFreshEntity(bat);}
        });
        test.runAfterDelay(28,()->{
            try {
                test.assertTrue(level.isRainingAt(base.east(12))&&!level.isRainingAt(base.east(24)),"Actual world precipitation reaches exposed fixture but roof blocks it");
                test.assertTrue(bats.get(0).isInWater(),"Actual fluid update marks immersed bat in water");
                for(int index=0;index<3;index++){var bat=bats.get(index);test.assertTrue(bat.tickCount>0,"World ticks reached bat");test.assertTrue(bat.getHealth()==(index<2?4:5),"Water/rain inflicts one actual damage under vanilla immunity; roof protects: "+index);}
                tc4revival.TC4Revival.LOGGER.info("PASS fireBatWorldWaterRainAndRoofRespectWetDamage: natural water/rain damage and sheltered dry control");test.succeed();
            } finally {bats.forEach(net.minecraft.world.entity.Entity::discard);saved.forEach((p,s)->level.setBlock(p,s,2));level.getLevelData().setRaining(raining);level.setRainLevel(rainLevel);
                biomeChunks.forEach(chunk->chunk.fillBiomesFromNoise((x,y,z,s)->savedBiomes.get(new BlockPos(x,y,z)),level.getChunkSource().randomState().sampler()));
                forced.forEach(c->level.setChunkForced(c.x,c.z,false));}
        });
    }
}
