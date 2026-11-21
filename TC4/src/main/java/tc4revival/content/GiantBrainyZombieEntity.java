package tc4revival.content;

import net.minecraft.network.syncher.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Anger, collision size and visual scale remain consistent across saves. */
public  class GiantBrainyZombieEntity extends BrainyZombieEntity {
    private static final EntityDataAccessor<Float> ANGER=SynchedEntityData.defineId(GiantBrainyZombieEntity.class,EntityDataSerializers.FLOAT);
    public GiantBrainyZombieEntity(EntityType<? extends GiantBrainyZombieEntity> type,Level level){super(type,level);xpReward=15;refreshDimensions();}
    public static AttributeSupplier.Builder attributes(){return BrainyZombieEntity.attributes().add(Attributes.MAX_HEALTH,60).add(Attributes.ATTACK_DAMAGE,7);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ANGER,1f);}
    public float anger(){return entityData.get(ANGER);}
    void setAnger(float value){entityData.set(ANGER,value);}
    @Override protected void registerGoals(){super.registerGoals();goalSelector.addGoal(2,new LeapAtTargetGoal(this,.4f));}
    @Override public boolean hurt(DamageSource source,float amount){if(!level().isClientSide)setAnger(Math.min(2,anger()+.1f));return super.hurt(source,amount);}
    @Override public void aiStep(){super.aiStep();if(!level().isClientSide){if(anger()>1)setAnger(Math.max(1,anger()-.002f));getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7+(anger()-1)*5);}}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> data){super.onSyncedDataUpdated(data);if(ANGER.equals(data))refreshDimensions();}
    @Override public EntityDimensions getDimensions(Pose pose){return EntityDimensions.scalable(.6f*anger(),1.8f*anger());}

    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);tag.putFloat("Anger",anger());}
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);setAnger(tag.contains("Anger")?net.minecraft.util.Mth.clamp(tag.getFloat("Anger"),1,2):1);}
}
