package tc4revival.content;

import java.time.MonthDay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.*;
import net.minecraftforge.event.level.LevelEvent;
import tc4revival.config.TCConfig;

/** Native biome tags admit all current Nether and seasonal forest families. */
public  class FireBatSpawning {
    private FireBatSpawning() {}
    public static void potentialSpawns(LevelEvent.PotentialSpawns event) { potentialSpawns(event,MonthDay.now()); }
    static void potentialSpawns(LevelEvent.PotentialSpawns event,MonthDay date) {
        if(event.getMobCategory()!=MobCategory.MONSTER || !TCConfig.SERVER.spawns(TCConfig.SpawnKind.FIRE_BATS))return;
        int weight=weight(event.getLevel().getBiome(event.getPos()),date);
        boolean nether=event.getLevel().getBiome(event.getPos()).is(net.minecraft.tags.BiomeTags.IS_NETHER);
        if(weight>0 && event.getSpawnerDataList().stream().noneMatch(row->row.type==TCEntities.FIRE_BAT.get()))
            // Original natural spawning always made four attempts; registered 1-2 was ignored.
            event.addSpawnerData(NaturalSpawnEntries.get(TCEntities.FIRE_BAT.get(),weight,nether?4:1,nether?4:2));
    }
    static int weight(Holder<Biome> biome,MonthDay date) {
        // Modern native spawn attempts, range and competing pools differ from 1.7.10.
        // Calibrated against original-client/dedicated-server counts, not the old raw weight.
        if(biome.is(net.minecraft.tags.BiomeTags.IS_NETHER))return 41;
        if(date.equals(MonthDay.of(10,31)) && (biome.is(net.minecraft.tags.BiomeTags.IS_FOREST)||biome.is(Biomes.PLAINS)
                ||biome.is(net.minecraft.tags.BiomeTags.IS_TAIGA)||biome.is(net.minecraft.tags.BiomeTags.IS_JUNGLE)))return 5;
        return 0;
    }
    public static boolean maySpawn(EntityType<FireBatEntity> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        return TCConfig.SERVER.spawns(TCConfig.SpawnKind.FIRE_BATS) && level.getDifficulty()!=Difficulty.PEACEFUL
                && level.getMaxLocalRawBrightness(pos)<=random.nextInt(7)
                && Mob.checkMobSpawnRules(type,level,reason,pos,random);
    }
}
