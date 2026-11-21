package tc4revival.content;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.AABB;

public  class GiantTaintacleEntity extends TaintacleEntity {
    private static final EntityDataAccessor<Integer> ANGER=SynchedEntityData.defineId(GiantTaintacleEntity.class,EntityDataSerializers.INT);
    private final ServerBossEvent bossBar=new ServerBossEvent(getDisplayName(),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
    public GiantTaintacleEntity(EntityType<? extends GiantTaintacleEntity> type,Level level) { super(type,level); xpReward=20; }
    public static AttributeSupplier.Builder attributes() { return TaintacleEntity.attributes().add(Attributes.MAX_HEALTH,125).add(Attributes.ATTACK_DAMAGE,9); }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(ANGER,0); }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag) { ChampionMobs.make(this,true); return group; }
    @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason) { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override protected int decreaseAirSupply(int air) { return air; }
    @Override public void tick() {
        super.tick(); int anger=entityData.get(ANGER); if (anger>0) entityData.set(ANGER,anger-1);
        if (level().isClientSide && random.nextInt(15)==0 && entityData.get(ANGER)>0)
            level().addParticle(ParticleTypes.ANGRY_VILLAGER,getX()+(random.nextFloat()-.5)*getBbWidth(),getY()+getBbHeight()+random.nextFloat()*.5,getZ()+(random.nextFloat()-.5)*getBbWidth(),random.nextGaussian()*.02,random.nextGaussian()*.02,random.nextGaussian()*.02);
        if (!level().isClientSide) { if (tickCount%30==0) heal(1); bossBar.setName(getDisplayName()); bossBar.setProgress(getHealth()/getMaxHealth()); }
    }
    @Override public boolean hurt(DamageSource source,float damage) {
        if (!level().isClientSide && damage>35) {
            if (entityData.get(ANGER)==0) {
                addEffect(new MobEffectInstance(MobEffects.REGENERATION,200,(int)(damage/15)));
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,200,(int)(damage/40)));
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,200,(int)(damage/40))); entityData.set(ANGER,200);
                if (source.getEntity() instanceof Player player) player.displayClientMessage(getDisplayName().copy().append(" ").append(Component.translatable("tc.boss.enrage")),false);
            }
            damage=35;
        }
        return super.hurt(source,damage);
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) { }
    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossBar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossBar.removePlayer(player); }
    @Override public net.minecraft.world.entity.item.ItemEntity spawnAtLocation(net.minecraft.world.item.ItemStack stack,float offset) {
        if(!stack.is(TCItems.PRIMORDIAL_PEARL.get()))return super.spawnAtLocation(stack,offset);
        var item=TCEntities.SPECIAL_ITEM.get().create(level());if(item==null)return null;
        item.setPos(getX(),getY()+getBbHeight()/2,getZ());item.setItem(stack);item.setDefaultPickUpDelay();
        if(captureDrops()!=null)captureDrops().add(item);else level().addFreshEntity(item);return item;
    }

}
