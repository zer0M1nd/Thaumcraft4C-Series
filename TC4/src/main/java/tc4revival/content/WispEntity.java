package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tc4revival.aspect.TCAspects;
import tc4revival.network.FocusEffectPacket;

/** A drifting aspect spirit: retaliation, occasional aggression, and probabilistic line-of-sight zaps. */
public  class WispEntity extends FlyingMob implements Enemy {
    private static final EntityDataAccessor<String> TYPE=SynchedEntityData.defineId(WispEntity.class,EntityDataSerializers.STRING);
    int courseCooldown,aggroCooldown,attackCounter;
    Vec3 waypoint=Vec3.ZERO;
    public WispEntity(EntityType<? extends WispEntity> type,Level level){super(type,level);xpReward=5;}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,22).add(Attributes.ATTACK_DAMAGE,3);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(TYPE,"");}
    public String aspectTag(){return entityData.get(TYPE);}
    public void setAspectTag(String tag){entityData.set(TYPE,tag);}
    @Override protected int decreaseAirSupply(int air){return air;}
    @Override protected MovementEmission getMovementEmission(){return MovementEmission.NONE;}
    @Override protected boolean shouldDespawnInPeaceful(){return true;}
    @Override public boolean removeWhenFarAway(double distance){return true;}
    @Override public int getMaxSpawnClusterSize(){return 2;}
    @Override public boolean hurt(DamageSource source,float amount){
        if(!level().isClientSide){if(source.getDirectEntity() instanceof LivingEntity direct){setTarget(direct);aggroCooldown=200;}
            if(source.getEntity() instanceof LivingEntity owner){setTarget(owner);aggroCooldown=200;}}
        return super.hurt(source,amount);
    }
    void initializeAspect(){
        if(TCAspects.catalog().find(aspectTag()).isPresent())return;
        if(level().getBiome(blockPosition()).is(TCBiomes.EERIE))
            setAspectTag(java.util.List.of("tenebrae","exanimis","perditio","alienis","venenum","mortuus").get(random.nextInt(6)));
        else {var choices=random.nextInt(10)!=0?TCAspects.catalog().primals():TCAspects.catalog().all().stream().filter(a->!a.isPrimal()).toList();setAspectTag(choices.get(random.nextInt(choices.size())).tag());}
    }
    @Override protected void customServerAiStep(){
        initializeAspect();if(level().getDifficulty()==Difficulty.PEACEFUL){discard();return;}
        var delta=waypoint.subtract(position());double length=delta.length();
        if(length<1 || length>60)waypoint=position().add((random.nextFloat()*2-1)*16,(random.nextFloat()*2-1)*16,(random.nextFloat()*2-1)*16);
        if(courseCooldown--<=0){courseCooldown+=random.nextInt(5)+2;
            delta=waypoint.subtract(position());length=delta.length();
            if(length>1e-8 && traversable(waypoint,length))setDeltaMovement(getDeltaMovement().add(delta.scale(.1/length)));
            else waypoint=position();}
        if(getTarget()!=null && !getTarget().isAlive())setTarget(null);
        --aggroCooldown;
        if(random.nextInt(1000)==0 && (getTarget()==null || aggroCooldown<=0)){
            setTarget(level().getNearestPlayer(getX(),getY(),getZ(),16,true));if(getTarget()!=null)aggroCooldown=50;}
        var target=getTarget();
        if(target!=null && distanceToSqr(target)<256){
            setYRot((float)(-Math.atan2(target.getX()-getX(),target.getZ()-getZ())*180/Math.PI));yBodyRot=getYRot();
            if(hasLineOfSight(target)){if(++attackCounter==20)zap(target);}
            else if(attackCounter>0)--attackCounter;
        }else {setYRot((float)(-Math.atan2(getDeltaMovement().x,getDeltaMovement().z)*180/Math.PI));yBodyRot=getYRot();if(attackCounter>0)--attackCounter;}
    }
    boolean traversable(Vec3 destination,double length){
        var step=destination.subtract(position()).scale(1/length);var box=getBoundingBox();
        for(int i=1;i<length;i++){box=box.move(step);if(!level().noCollision(this,box))return false;}
        var pos=BlockPos.containing(destination);
        if(!level().hasChunkAt(pos) || !level().getFluidState(pos).isEmpty())return false;
        for(int i=0;i<11;i++)if(!level().isEmptyBlock(pos.below(i)))return true;
        return false;
    }
    void zap(LivingEntity target){
        level().playSound(null,getX(),getY(),getZ(),TCSounds.ZAP.get(),SoundSource.HOSTILE,1,1.1f);FocusEffectPacket.lightning(level(),this,target);
        var motion=target.getDeltaMovement();boolean moving=Math.abs(motion.x)>.1f || Math.abs(motion.y)>.1f || Math.abs(motion.z)>.1f;
        if(random.nextFloat()<(moving?.4f:.66f))target.hurt(damageSources().mobAttack(this),(float)getAttributeValue(Attributes.ATTACK_DAMAGE)+(moving?0:1));
        attackCounter=-20+random.nextInt(20);
    }
    @Override public void tick(){super.tick();if(level().isClientSide)tc4revival.client.WispRenderer.effects(this);}
    @Override public void die(DamageSource source){super.die(source);if(!level().isClientSide)FocusEffectPacket.burst(level(),position().add(0,.45,0),1);}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit){ }
    @Override protected SoundEvent getAmbientSound(){return TCSounds.WISP_LIVE.get();}
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.LAVA_EXTINGUISH;}
    @Override protected SoundEvent getDeathSound(){return TCSounds.WISP_DEAD.get();}
    @Override protected float getSoundVolume(){return .25f;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putString("Type",aspectTag());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setAspectTag(tag.getString("Type"));}
}
