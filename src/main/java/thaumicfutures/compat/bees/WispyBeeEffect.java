package thaumicfutures.compat.bees;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.api.genetics.IEffectData;
import forestry.api.genetics.IGenome;
import forestry.apiculture.genetics.effects.ThrottledBeeEffect;
import forestry.core.genetics.EffectData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.TCAspects;
import tc4revival.content.WispEntity;

public final class WispyBeeEffect implements IBeeEffect {
    @Override public boolean isDominant() { return false; }
    @Override public IEffectData validateStorage(IEffectData storage) {
        return storage instanceof EffectData data && data.getIntSize() >= 1 ? data : new EffectData(1, 0);
    }
    @Override public IEffectData doEffect(IGenome genome, IEffectData storage, IBeeHousing housing) {
        if (!thaumicfutures.FutureFeatures.Group.FORESTRY_BEES.enabled()) return storage;
        storage = validateStorage(storage);
        if (!(housing.getWorldObj() instanceof ServerLevel level)) return storage;
        int elapsed = storage.getInteger(0);
        if (elapsed < 1800) { storage.setInteger(0, elapsed + 1); return storage; }
        storage.setInteger(0, 0);
        if (level.random.nextInt(100) > 79) {
            var sound = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("thaumcraft:wisplive"));
            if (sound != null && level.random.nextInt(100) < 35) {
                var territory = genome.getActiveValue(forestry.api.genetics.alleles.BeeChromosomes.TERRITORY);
                var origin = housing.getCoordinates();
                level.playSound(null, origin.getX() + (level.random.nextDouble() * 2 - 1) * territory.getX(),
                        origin.getY() + (level.random.nextDouble() * 2 - 1) * territory.getY(),
                        origin.getZ() + (level.random.nextDouble() * 2 - 1) * territory.getZ(), sound,
                        net.minecraft.sounds.SoundSource.NEUTRAL, .5f,
                        1 + (level.random.nextFloat() - level.random.nextFloat()) * .2f);
            }
            return storage;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) return storage;
        var area = ThrottledBeeEffect.getBounding(housing, genome);
        double x = area.minX + level.random.nextDouble() * area.getXsize();
        double y = housing.getCoordinates().getY() + level.random.nextInt(3) - 1;
        double z = area.minZ + level.random.nextDouble() * area.getZsize();
        var position = BlockPos.containing(x, y, z);
        if (!level.hasChunkAt(position) || level.getEntitiesOfClass(WispEntity.class,
                new AABB(position).inflate(8, 4, 8)).size() >= 6) return storage;
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("thaumcraft", "wisp"));
        if (type == null || !(type.create(level) instanceof WispEntity wisp)) return storage;
        wisp.moveTo(x, y, z, level.random.nextFloat() * 360, 0);
        if (!wisp.checkSpawnRules(level, MobSpawnType.MOB_SUMMONED) || !wisp.checkSpawnObstruction(level)) return storage;
        net.minecraftforge.event.ForgeEventFactory.onFinalizeSpawn(wisp, level,
                level.getCurrentDifficultyAt(position), MobSpawnType.MOB_SUMMONED, null, null);
        if (wisp.isSpawnCancelled()) return storage;
        var aspects = TCAspects.catalog().all();
        if (!aspects.isEmpty()) wisp.setAspectTag(aspects.get(level.random.nextInt(aspects.size())).tag());
        level.addFreshEntity(wisp);
    
        return storage;
    }
}
