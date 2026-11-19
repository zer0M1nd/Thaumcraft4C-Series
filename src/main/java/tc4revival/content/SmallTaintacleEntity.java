package tc4revival.content;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public  class SmallTaintacleEntity extends TaintacleEntity {
    private int lifetime=200;
    public SmallTaintacleEntity(EntityType<? extends SmallTaintacleEntity> type,Level level) { super(type,level); xpReward=0; }
    public static AttributeSupplier.Builder attributes() { return TaintacleEntity.attributes().add(Attributes.MAX_HEALTH,8).add(Attributes.ATTACK_DAMAGE,2); }
    @Override public void tick() { super.tick(); if (lifetime--<=0) actuallyHurt(damageSources().magic(),10); }
    @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason) { return false; }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) { }
}
