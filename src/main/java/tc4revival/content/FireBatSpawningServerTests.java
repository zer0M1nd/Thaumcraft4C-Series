package tc4revival.content;

import java.time.MonthDay;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.gametest.*;
import tc4revival.config.TCConfig;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FireBatSpawningServerTests {
    @GameTest(template="wand_test_empty")
    public static void fireBatNaturalPoolsAndSeason(GameTestHelper test) {
        var biomes=test.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for(var key:java.util.List.of(Biomes.NETHER_WASTES,Biomes.PLAINS,Biomes.FOREST,Biomes.TAIGA,Biomes.JUNGLE,
                Biomes.CRIMSON_FOREST,Biomes.WARPED_FOREST,Biomes.BASALT_DELTAS,Biomes.SOUL_SAND_VALLEY,Biomes.DESERT,TCBiomes.MAGICAL_FOREST)) {
            var biome=biomes.getHolderOrThrow(key);boolean nether=biome.is(net.minecraft.tags.BiomeTags.IS_NETHER);
            boolean seasonal=biome.is(net.minecraft.tags.BiomeTags.IS_FOREST)||biome.is(net.minecraft.tags.BiomeTags.IS_TAIGA)||biome.is(net.minecraft.tags.BiomeTags.IS_JUNGLE)||key.equals(Biomes.PLAINS);
            test.assertTrue(FireBatSpawning.weight(biome,MonthDay.of(10,31))==(nether?41:seasonal?5:0),"Nether calibration and Halloween biome identities: "+key.location());
            test.assertTrue(FireBatSpawning.weight(biome,MonthDay.of(10,30))==(nether?41:0),"No early seasonal spawning: "+key.location());
            test.assertTrue(FireBatSpawning.weight(biome,MonthDay.of(11,1))==(nether?41:0),"No late seasonal spawning: "+key.location());
        }
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,2,3));
        test.assertTrue(level.getBiome(pos).is(Biomes.PLAINS),"Validation fixture uses actual plains biome");
        net.minecraftforge.common.ForgeConfigSpec.BooleanValue flag=TCConfig.SERVER_SPEC.getValues().get("monster_spawning.spawn_fire_bats");boolean enabled=flag.get();
        try {
            flag.set(true);var pool=new LevelEvent.PotentialSpawns(level,MobCategory.MONSTER,pos,net.minecraft.util.random.WeightedRandomList.create());
            FireBatSpawning.potentialSpawns(pool,MonthDay.of(10,31));FireBatSpawning.potentialSpawns(pool,MonthDay.of(10,31));
            var rows=pool.getSpawnerDataList();test.assertTrue(rows.size()==1 && rows.get(0).type==TCEntities.FIRE_BAT.get() && rows.get(0).getWeight().asInt()==5 && rows.get(0).minCount==1 && rows.get(0).maxCount==2,"Actual seasonal pool has weight five, groups one to two and no duplicates");
            flag.set(false);var disabled=new LevelEvent.PotentialSpawns(level,MobCategory.MONSTER,pos,net.minecraft.util.random.WeightedRandomList.create());
            FireBatSpawning.potentialSpawns(disabled,MonthDay.of(10,31));test.assertTrue(disabled.getSpawnerDataList().isEmpty(),"Live disabled config prevents future additions");
        } finally {flag.set(enabled);}
        passed(test,"pools-season-config");
    }
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void fireBatRegisteredSpawnPlacement(GameTestHelper test) {
        var level=test.getLevel();var center=test.absolutePos(new BlockPos(3,3,3));
        var original=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(var mutable:BlockPos.betweenClosed(center.offset(-3,-1,-3),center.offset(3,3,3))) {
            var pos=mutable.immutable();original.put(pos,level.getBlockState(pos));
            boolean wall=Math.abs(pos.getX()-center.getX())==3 || Math.abs(pos.getZ()-center.getZ())==3 || pos.getY()==center.getY()-1 || pos.getY()==center.getY()+3;
            level.setBlock(pos,wall?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        }
        test.runAfterDelay(15,()->{
            net.minecraftforge.common.ForgeConfigSpec.BooleanValue flag=TCConfig.SERVER_SPEC.getValues().get("monster_spawning.spawn_fire_bats");boolean enabled=flag.get();
            var difficulty=level.getDifficulty();
            try {
                flag.set(true);level.getServer().setDifficulty(Difficulty.NORMAL,true);
                test.assertTrue(level.getMaxLocalRawBrightness(center)==0,"Enclosed fixture has zero light");
                test.assertTrue(SpawnPlacements.getPlacementType(TCEntities.FIRE_BAT.get())==SpawnPlacements.Type.ON_GROUND,"Natural placement registration is active");
                test.assertTrue(SpawnPlacements.checkSpawnRules(TCEntities.FIRE_BAT.get(),level,MobSpawnType.NATURAL,center,RandomSource.create(0)),"Registered rule admits a dark supported position");
                flag.set(false);test.assertTrue(!SpawnPlacements.checkSpawnRules(TCEntities.FIRE_BAT.get(),level,MobSpawnType.NATURAL,center,RandomSource.create(0)),"Disabled natural spawning is rejected by placement rule too");
                flag.set(true);level.getServer().setDifficulty(Difficulty.PEACEFUL,true);
                test.assertTrue(!FireBatSpawning.maySpawn(TCEntities.FIRE_BAT.get(),level,MobSpawnType.NATURAL,center,RandomSource.create(0)),"Peaceful difficulty cannot spawn hostile bats");
            } finally {flag.set(enabled);level.getServer().setDifficulty(difficulty,true);original.forEach((pos,state)->level.setBlock(pos,state,3));}
            passed(test,"registered-placement-darkness-config");
        });
    }
    private static void passed(GameTestHelper test,String name) {tc4revival.TC4Revival.LOGGER.info("Fire bat spawning PASS: {}",name);test.succeed();}
}
