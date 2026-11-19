package conciliummodern;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.world.*;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;
import tc4revival.content.BrainyZombieEntity;

@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID)
public final class ConciliumSpawns implements BiomeModifier {
    public static final DeferredRegister<Codec<? extends BiomeModifier>> CODECS=DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,ThaumicConcilium.ID);
    public static final RegistryObject<Codec<ConciliumSpawns>> CODEC=CODECS.register("mobs",()->Codec.unit(ConciliumSpawns::new));
    @Override public void modify(Holder<Biome> biome,Phase phase,ModifiableBiomeInfo.BiomeInfo.Builder builder){
        if(phase!=Phase.ADD)return;
        int ranger=ConciliumMobConfig.RANGER_WEIGHT.get();
        if(ranger>0&&CrimsonContent.dyes()&&!biome.unwrapKey().map(key->ConciliumMobConfig.RANGER_BIOME_BLACKLIST.get().contains(key.location().toString())).orElse(false))
            builder.getMobSpawnSettings().addSpawn(MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(CrimsonContent.RANGER.get(),ranger,1,2));
        int quicksilver=ConciliumMobConfig.QUICKSILVER_WEIGHT.get();
        if(quicksilver>0 && !biome.unwrapKey().map(key->ConciliumMobConfig.QUICKSILVER_BIOME_BLACKLIST.get().contains(key.location().toString())).orElse(false))
            builder.getMobSpawnSettings().addSpawn(MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(ConciliumEntities.QUICKSILVER_ELEMENTAL.get(),quicksilver,1,2));
        int regular=ConciliumMobConfig.THAUMATURGE_WEIGHT.get();
        if(regular>0 && !biome.unwrapKey().map(key->ConciliumMobConfig.THAUMATURGE_BIOME_BLACKLIST.get().contains(key.location().toString())).orElse(false))
            builder.getMobSpawnSettings().addSpawn(MobCategory.CREATURE,new MobSpawnSettings.SpawnerData(ConciliumEntities.THAUMATURGE.get(),regular,1,3));
        int weight=ConciliumMobConfig.MAD_WEIGHT.get();
        if(weight<=0 || biome.unwrapKey().map(key->ConciliumMobConfig.MAD_BIOME_BLACKLIST.get().contains(key.location().toString())).orElse(false))return;
        builder.getMobSpawnSettings().addSpawn(MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(ConciliumEntities.MAD_THAUMATURGE.get(),weight,1,3));
    }
    @Override public Codec<? extends BiomeModifier> codec(){return CODEC.get();}
    public static void placements(SpawnPlacementRegisterEvent event){
        event.register(CrimsonContent.RANGER.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Monster::checkMonsterSpawnRules,SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ConciliumEntities.QUICKSILVER_ELEMENTAL.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ConciliumEntities.THAUMATURGE.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.Mob::checkMobSpawnRules,SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ConciliumEntities.MAD_THAUMATURGE.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
    @SubscribeEvent public static void replaceBrainy(MobSpawnEvent.FinalizeSpawn event){
        if(!(event.getEntity() instanceof BrainyZombieEntity zombie) || event.isSpawnCancelled()
                || zombie.getRandom().nextInt(100)>=ConciliumMobConfig.MAD_REPLACEMENT.get())return;
        var level=event.getLevel().getLevel();var replacement=ConciliumEntities.MAD_THAUMATURGE.get().create(level);
        if(replacement==null)return;
        replacement.moveTo(zombie.getX(),zombie.getY(),zombie.getZ(),zombie.getYRot(),zombie.getXRot());
        replacement.setCustomName(zombie.getCustomName());replacement.setCustomNameVisible(zombie.isCustomNameVisible());
        replacement.finalizeSpawn(event.getLevel(),event.getDifficulty(),event.getSpawnType(),null,null);
        if(level.addFreshEntity(replacement))event.setSpawnCancelled(true);
    }
}
