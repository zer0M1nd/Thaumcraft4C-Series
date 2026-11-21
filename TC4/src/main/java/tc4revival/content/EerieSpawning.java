package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.event.level.LevelEvent;
import tc4revival.config.TCConfig;

/** Configured entries are added to the live pool, so server config reload does not require data regeneration. */
public  class EerieSpawning {
    private EerieSpawning(){}
    public static void potentialSpawns(LevelEvent.PotentialSpawns event){
        if(event.getMobCategory()!=MobCategory.MONSTER)return;
        var biome=event.getLevel().getBiome(event.getPos());
        if(biome.is(TCBiomes.MAGICAL_FOREST) && TCConfig.SERVER.spawns(TCConfig.SpawnKind.WISPS) && event.getSpawnerDataList().stream().noneMatch(row->row.type==TCEntities.WISP.get()))event.addSpawnerData(NaturalSpawnEntries.get(TCEntities.WISP.get(),10,1,2));
        if(!biome.is(TCBiomes.EERIE)){
            if(biome.is(net.minecraft.world.level.biome.Biomes.NETHER_WASTES) && TCConfig.SERVER.spawns(TCConfig.SpawnKind.WISPS)
                    && event.getSpawnerDataList().stream().noneMatch(row->row.type==TCEntities.WISP.get()))
                // 1.7.10 tried four pack members regardless of the registered group size.
                // Weight is calibrated with the modern native spawner against the original engine.
                event.addSpawnerData(NaturalSpawnEntries.get(TCEntities.WISP.get(),27,4,4));
            if(TCConfig.SERVER.spawns(TCConfig.SpawnKind.ANGRY_ZOMBIES) && !event.getSpawnerDataList().isEmpty()
                    && (biome.is(net.minecraft.world.level.biome.Biomes.FOREST)||biome.is(net.minecraft.world.level.biome.Biomes.PLAINS)||biome.is(net.minecraft.world.level.biome.Biomes.TAIGA)||biome.is(net.minecraft.world.level.biome.Biomes.JUNGLE)))add(event,TCEntities.BRAINY_ZOMBIE.get(),10);
            return;
        }
        if(TCConfig.SERVER.spawns(TCConfig.SpawnKind.ANGRY_ZOMBIES)){add(event,TCEntities.BRAINY_ZOMBIE.get(),32);add(event,TCEntities.GIANT_BRAINY_ZOMBIE.get(),8);}
        if(TCConfig.SERVER.spawns(TCConfig.SpawnKind.WISPS))add(event,TCEntities.WISP.get(),3);
        if(TCConfig.SERVER.spawns(TCConfig.SpawnKind.ELDERCREATURES))add(event,TCEntities.ELDRITCH_GUARDIAN.get(),1);
    }
    private static void add(LevelEvent.PotentialSpawns event,EntityType<?> type,int weight){if(event.getSpawnerDataList().stream().noneMatch(row->row.type==type))event.addSpawnerData(NaturalSpawnEntries.get(type,weight,1,1));}
    static boolean legacyDarkness(ServerLevelAccessor level,BlockPos pos,RandomSource random){
        if(level.getDifficulty()==Difficulty.PEACEFUL || level.getBrightness(LightLayer.SKY,pos)>random.nextInt(32))return false;
        return (level.getLevel().isThundering()?level.getMaxLocalRawBrightness(pos,10):level.getMaxLocalRawBrightness(pos))<=random.nextInt(8);
    }
    public static boolean zombie(EntityType<? extends BrainyZombieEntity> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random){
        return TCConfig.SERVER.spawns(TCConfig.SpawnKind.ANGRY_ZOMBIES)&&legacyDarkness(level,pos,random)&&Mob.checkMobSpawnRules(type,level,reason,pos,random);
    }
    public static boolean wisp(EntityType<WispEntity> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random){
        // Recheck each pack member's biome, not just the position used to choose the spawn pool.
        // TC4 naturally spawned wisps only in magical forest, eerie and Nether biomes.
        var biome=level.getBiome(pos);
        boolean natural=reason==MobSpawnType.NATURAL || reason==MobSpawnType.CHUNK_GENERATION;
        if(natural && (pos.getY()<0 || !(biome.is(TCBiomes.MAGICAL_FORESTS)||biome.is(TCBiomes.MAGICAL_FOREST)||biome.is(TCBiomes.EERIE)
                || biome.is(net.minecraft.tags.BiomeTags.IS_NETHER))))return false;
        // The old natural-spawn engine required ground even for flying wisps.
        // Keep summoned/spawner wisps and other dimensions on their existing placement path.
        if(natural && biome.is(net.minecraft.tags.BiomeTags.IS_NETHER)
                && !NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND,level,pos,type))return false;
        return TCConfig.SERVER.spawns(TCConfig.SpawnKind.WISPS)&&legacyDarkness(level,pos,random)
                && level.getLevel().getEntitiesOfClass(WispEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(16)).size()<8;
    }
}
