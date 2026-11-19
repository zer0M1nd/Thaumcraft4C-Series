package tc4revival.content;

import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MobSpawnSettings;

/** NaturalSpawner re-queries the pool and uses contains on the selected SpawnerData. */
 class NaturalSpawnEntries {
    private record Key(EntityType<?> type,int weight,int min,int max) {}
    private static final ConcurrentHashMap<Key,MobSpawnSettings.SpawnerData> ENTRIES=new ConcurrentHashMap<>();
    private NaturalSpawnEntries() {}
    static MobSpawnSettings.SpawnerData get(EntityType<?> type,int weight,int min,int max) {
        // SpawnerData has identity equality. A new row on each PotentialSpawns
        // event is always rejected at the later candidate-position check.
        return ENTRIES.computeIfAbsent(new Key(type,weight,min,max),
                key->new MobSpawnSettings.SpawnerData(key.type(),key.weight(),key.min(),key.max()));
    }
}
