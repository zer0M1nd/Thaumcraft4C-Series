package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.event.level.LevelEvent;
import tc4revival.config.TCConfig;

public  class TaintacleSpawning {
    public static boolean maySpawn(EntityType<TaintacleEntity> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        return TCConfig.SERVER.spawns(TCConfig.SpawnKind.TAINTACLES) && level.getDifficulty()!=Difficulty.PEACEFUL
                && TCBiomes.isTainted(level,pos) && EerieSpawning.legacyDarkness(level,pos,random);
    }
    public static void potentialSpawns(LevelEvent.PotentialSpawns event) {
        if (event.getMobCategory()==MobCategory.MONSTER && TCConfig.SERVER.spawns(TCConfig.SpawnKind.TAINTACLES)
                && event.getLevel().getBiome(event.getPos()).is(TCBiomes.TAINTED_LAND)
                && event.getSpawnerDataList().stream().noneMatch(row->row.type==TCEntities.TAINTACLE.get()))
            event.addSpawnerData(NaturalSpawnEntries.get(TCEntities.TAINTACLE.get(),1,1,1));
    }
    private TaintacleSpawning() {}
}
