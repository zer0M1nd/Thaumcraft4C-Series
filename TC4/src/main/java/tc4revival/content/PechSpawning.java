package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;

public  class PechSpawning {
    private PechSpawning() {}
    public static boolean biomeAllowed(ServerLevelAccessor level,BlockPos pos) {
        var biome=level.getBiome(pos);
        if (!LegacyBiomeProfiles.get(biome).magical() || biome.is(TCBiomes.TAINTED_LAND)) return false;
        return level.getLevel().dimension()==Level.OVERWORLD || biome.is(TCBiomes.MAGICAL_FOREST)
                || biome.unwrapKey().map(k->k.location().toString().equals("thaumcraft:eerie")).orElse(false);
    }
    public static boolean maySpawn(EntityType<PechEntity> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,net.minecraft.util.RandomSource random) {
        return tc4revival.config.TCConfig.SERVER.spawns(tc4revival.config.TCConfig.SpawnKind.PECHS) && biomeAllowed(level,pos)
                && level.getLevel().getEntitiesOfClass(PechEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(16)).size()<4
                && EerieSpawning.legacyDarkness(level,pos,random) && Mob.checkMobSpawnRules(type,level,reason,pos,random);
    }
    public static void potentialSpawns(net.minecraftforge.event.level.LevelEvent.PotentialSpawns event) {
        if (event.getMobCategory()!=MobCategory.MONSTER || event.getSpawnerDataList().isEmpty()
                || !tc4revival.config.TCConfig.SERVER.spawns(tc4revival.config.TCConfig.SpawnKind.PECHS)
                || !LegacyBiomeProfiles.get(event.getLevel().getBiome(event.getPos())).magical()) return;
        if (event.getSpawnerDataList().stream().noneMatch(row->row.type==TCEntities.PECH.get()))
            event.addSpawnerData(NaturalSpawnEntries.get(TCEntities.PECH.get(),10,1,1));
    }
}
