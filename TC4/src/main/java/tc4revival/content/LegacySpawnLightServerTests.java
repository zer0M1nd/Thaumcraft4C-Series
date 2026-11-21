package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

/** Real light-engine fixture detects accidentally reverting to modern zero-block-light spawning. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class LegacySpawnLightServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=150)
    public static void legacySpawnRulesAllowDimLight(GameTestHelper test) {
        var level=test.getLevel();var center=test.absolutePos(new BlockPos(3,3,3));
        var states=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        var columns=new java.util.ArrayList<BlockPos>();
        for(var mutable:BlockPos.betweenClosed(center.offset(-4,-1,-4),center.offset(4,4,4))) {
            var pos=mutable.immutable();states.put(pos,level.getBlockState(pos));
            boolean shell=Math.abs(pos.getX()-center.getX())==4 || Math.abs(pos.getZ()-center.getZ())==4 || pos.getY()==center.getY()-1 || pos.getY()==center.getY()+4;
            level.setBlock(pos,shell?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        }
        for(int x=-8;x<=8;x+=4)for(int z=-8;z<=8;z+=4) {
            var pos=center.offset(x,0,z);level.getChunkAt(pos);
            test.assertTrue(level.getNoiseBiome(QuartPos.fromBlock(pos.getX()),QuartPos.fromBlock(pos.getY()),QuartPos.fromBlock(pos.getZ())).is(Biomes.PLAINS),"Validation biome columns start as plains");
            columns.add(pos);
        }
        level.setBlock(center.east(2),Blocks.REDSTONE_TORCH.defaultBlockState(),3);
        test.runAfterDelay(20,()->{
            var difficulty=level.getDifficulty();
            var flags=new java.util.LinkedHashMap<net.minecraftforge.common.ForgeConfigSpec.BooleanValue,Boolean>();
            for(String name:java.util.List.of("pechs","taintacles","fire_bats")) {
                net.minecraftforge.common.ForgeConfigSpec.BooleanValue flag=tc4revival.config.TCConfig.SERVER_SPEC.getValues().get("monster_spawning.spawn_"+name);
                flags.put(flag,flag.get());
            }
            try {
                flags.keySet().forEach(flag->flag.set(true));level.getServer().setDifficulty(Difficulty.NORMAL,true);
                test.assertTrue(level.getMaxLocalRawBrightness(center)==5,"Actual redstone torch fixture provides light five");
                test.assertTrue(!net.minecraft.world.entity.monster.Monster.isDarkEnoughToSpawn(level,center,RandomSource.create(0)),"Modern monster rule rejects this fixture");
                WorldBiomeChanges.changeColumns(level,columns,TCBiomes.MAGICAL_FOREST);
                test.assertTrue(level.getBiome(center).is(TCBiomes.MAGICAL_FOREST),"Runtime biome query resolves to magical forest");
                int pech=0,inhabited=0,bat=0;
                for(int seed=0;seed<64;seed++) {
                    if(SpawnPlacements.checkSpawnRules(TCEntities.PECH.get(),level,MobSpawnType.NATURAL,center,RandomSource.create(seed)))pech++;
                    if(SpawnPlacements.checkSpawnRules(TCEntities.INHABITED_ZOMBIE.get(),level,MobSpawnType.NATURAL,center,RandomSource.create(seed)))inhabited++;
                    if(SpawnPlacements.checkSpawnRules(TCEntities.FIRE_BAT.get(),level,MobSpawnType.NATURAL,center,RandomSource.create(seed)))bat++;
                }
                test.assertTrue(pech>0 && pech<64 && inhabited>0 && inhabited<64 && bat>0 && bat<64,"Registered rules probabilistically permit dim light for Pech, inhabited zombies and fire bats");
                WorldBiomeChanges.changeColumns(level,columns,TCBiomes.TAINTED_LAND);int taint=0;
                for(int seed=0;seed<64;seed++)if(SpawnPlacements.checkSpawnRules(TCEntities.TAINTACLE.get(),level,MobSpawnType.NATURAL,center,RandomSource.create(seed)))taint++;
                test.assertTrue(taint>0 && taint<64,"Tainted-biome placement keeps the old probabilistic light allowance");
                level.getServer().setDifficulty(Difficulty.PEACEFUL,true);
                test.assertTrue(!EerieSpawning.legacyDarkness(level,center,RandomSource.create(0)),"Legacy allowance still excludes peaceful difficulty");
            } finally {
                flags.forEach((flag,value)->flag.set(value));level.getServer().setDifficulty(difficulty,true);
                WorldBiomeChanges.changeColumns(level,columns,Biomes.PLAINS);states.forEach((pos,state)->level.setBlock(pos,state,3));
            }
            tc4revival.TC4Revival.LOGGER.info("Legacy spawn light PASS: actual-light-five-registered-rules");test.succeed();
        });
    }
}
