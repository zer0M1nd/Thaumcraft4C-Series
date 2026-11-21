package tc4revival.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Dungeon spiders also support the original viewer-specific, harmless hallucinations. */
public  class MindSpiderEntity extends Spider {
    private static final EntityDataAccessor<Boolean> HARMLESS=SynchedEntityData.defineId(MindSpiderEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> VIEWER=SynchedEntityData.defineId(MindSpiderEntity.class,EntityDataSerializers.STRING);
    private int lifespan=Integer.MAX_VALUE;
    public MindSpiderEntity(EntityType<? extends MindSpiderEntity> type,Level level) { super(type,level); xpReward=1; }
    public static AttributeSupplier.Builder attributes() { return Spider.createAttributes().add(Attributes.MAX_HEALTH,1).add(Attributes.ATTACK_DAMAGE,1).add(Attributes.FOLLOW_RANGE,12); }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(HARMLESS,false); entityData.define(VIEWER,""); }
    public boolean isHarmless() { return entityData.get(HARMLESS); }
    public void setHarmless(boolean value) { if (value) lifespan=1200; entityData.set(HARMLESS,value); }
    public String viewer() { return entityData.get(VIEWER); }
    public void setViewer(String value) { entityData.set(VIEWER,String.valueOf(value)); }
    @Override protected void registerGoals() {
        super.registerGoals(); targetSelector.removeAllGoals(goal->true);
        targetSelector.addGoal(1,new TargetGoal(this,false) {
            private Player selected;
            @Override public boolean canUse() {
                selected=null; double nearest=144;
                for (Player player:level().players()) {
                    if (!player.isAlive() || player.isSpectator() || player.getAbilities().instabuild) continue;
                    double distance=distanceToSqr(player);
                    if (distance<nearest) { nearest=distance; selected=player; }
                }
                return selected!=null;
            }
            @Override public void start() { setTarget(selected); super.start(); }
        });
        goalSelector.removeAllGoals(goal->goal instanceof LeapAtTargetGoal);
        goalSelector.addGoal(3,new LeapAtTargetGoal(this,.4f) {
            @Override public boolean canUse() { return !isHarmless() && super.canUse(); }
        });
    }
    @Override public boolean doHurtTarget(Entity target) { return !isHarmless() && super.doHurtTarget(target); }
    @Override public void tick() { super.tick(); if (!level().isClientSide && tickCount>lifespan) discard(); }
    @Override public int getExperienceReward() { return isHarmless() ? 0 : super.getExperienceReward(); }
    @Override public boolean isIgnoringBlockTriggers() { return true; }
    @Override protected MovementEmission getMovementEmission() { return MovementEmission.NONE; }
    @Override public float getVoicePitch() { return .7f; }

    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) { }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putBoolean("harmless",isHarmless()); tag.putString("viewer",viewer());tag.putInt("LifeRemaining",Math.max(0,lifespan-tickCount)); }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); entityData.set(HARMLESS,tag.getBoolean("harmless")); setViewer(tag.getString("viewer"));
        lifespan=tag.contains("LifeRemaining")?Math.max(0,tag.getInt("LifeRemaining")):(isHarmless()?1200:Integer.MAX_VALUE);tickCount=0;
    }
}
