package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Floating swarm with target-bound summoned variants. */
public  class TaintSwarmEntity extends Monster implements TaintedMob {
    private static final EntityDataAccessor<Byte> FLAGS=SynchedEntityData.defineId(TaintSwarmEntity.class,EntityDataSerializers.BYTE);
    private BlockPos waypoint;
    private int damageBonus,attackDelay;
    public TaintSwarmEntity(EntityType<? extends TaintSwarmEntity> type,Level level) { super(type,level); }
    public static AttributeSupplier.Builder attributes() { return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.ATTACK_DAMAGE,2).add(Attributes.FOLLOW_RANGE,16); }
    @Override protected void registerGoals() {}
    @Override protected void defineSynchedData() { super.defineSynchedData();entityData.define(FLAGS,(byte)0); }
    public boolean summoned() { return (entityData.get(FLAGS)&2)!=0; }
    public void summoned(boolean value) { int flags=entityData.get(FLAGS);entityData.set(FLAGS,(byte)(value?flags|2:flags&~2)); }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag);tag.putByte("Flags",entityData.get(FLAGS));tag.putInt("damBonus",damageBonus);tag.putInt("AttackDelay",attackDelay); }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);entityData.set(FLAGS,tag.getByte("Flags"));damageBonus=Mth.clamp(tag.getInt("damBonus"),0,1029);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2+damageBonus);attackDelay=Mth.clamp(tag.getInt("AttackDelay"),0,14);
    }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isIgnoringBlockTriggers() { return true; }
    @Override protected MovementEmission getMovementEmission() { return MovementEmission.NONE; }
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source) { return false; }
    @Override protected void checkFallDamage(double height,boolean ground,BlockState state,BlockPos pos) {}
    @Override protected float getSoundVolume() { return .1f; }
    @Override protected SoundEvent getAmbientSound() { return null; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return TCSounds.SWARM_ATTACK.get(); }
    @Override protected SoundEvent getDeathSound() { return TCSounds.SWARM_ATTACK.get(); }
    @Override public boolean hurt(DamageSource source,float amount) {
        boolean accepted=super.hurt(source,amount);
        if(accepted && source.getEntity() instanceof LivingEntity attacker && attacker!=this && attacker!=getVehicle() && !hasPassenger(attacker))setTarget(attacker);
        return accepted;
    }
    @Override protected void customServerAiStep() {
        attackDelay=Math.max(0,attackDelay-1);
        if(getTarget()!=null && (!getTarget().isAlive() || !canAttack(getTarget()) || isAlliedTo(getTarget())))setTarget(null);
        if(getTarget()==null && !summoned())setTarget(level().getNearestPlayer(getX(),getY(),getZ(),12,true));
        var target=getTarget();
        if(target==null) {
            if(summoned())hurt(damageSources().generic(),5);
            if(waypoint!=null && (!level().isEmptyBlock(waypoint) || waypoint.getY()<level().getMinBuildHeight()+1
                    || waypoint.getY()>LegacySurfaceHeight.at((net.minecraft.server.level.ServerLevel)level(),waypoint.getX(),waypoint.getZ())+8
                    || !TCBiomes.isTainted(level(),waypoint)))waypoint=null;
            var truncated=blockPosition();
            if(waypoint==null || random.nextInt(30)==0 || waypoint.distSqr(truncated)<4)
                waypoint=truncated.offset(random.nextInt(7)-random.nextInt(7),random.nextInt(6)-2,random.nextInt(7)-random.nextInt(7));
            steer(Vec3.atLowerCornerOf(waypoint).add(.5,.1,.5),.015000000014901161,.015000000014901161);
        } else {
            if(hasLineOfSight(target))strike(target);
            steer(target.position().add(0,target.getEyeHeight(),0),.025000000149011613,.02500000001490116);
        }
        if(getTarget() instanceof Player player && player.getAbilities().invulnerable)setTarget(null);
    }
    private void steer(Vec3 destination,double xRate,double zRate) {
        var offset=destination.subtract(position());var velocity=getDeltaMovement();
        setDeltaMovement(velocity.add((Math.signum(offset.x)*.5-velocity.x)*xRate,
                (Math.signum(offset.y)*(double).7f-velocity.y)*(double).1f,(Math.signum(offset.z)*.5-velocity.z)*zRate));
        setYRot((float)Math.toDegrees(Math.atan2(getDeltaMovement().z,getDeltaMovement().x))-90);yBodyRot=getYRot();zza=.1f;
    }
    void strike(LivingEntity target) {
        if(attackDelay>0 || !canAttack(target) || isAlliedTo(target) || distanceTo(target)>=3 || target.getBoundingBox().maxY<=getBoundingBox().minY || target.getBoundingBox().minY>=getBoundingBox().maxY)return;
        if(summoned())LegacyCombatCredit.set(target,100);
        attackDelay=10+random.nextInt(5);var velocity=target.getDeltaMovement();
        if(doHurtTarget(target))target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100));
        target.setDeltaMovement(velocity);target.hasImpulse=false;
        playSound(TCSounds.SWARM_ATTACK.get(),.3f,.9f+level().random.nextFloat()*.2f);
    }
    @Override public void tick() {
        super.tick();setDeltaMovement(getDeltaMovement().multiply(1,(double).6f,1));
        if(level().isClientSide)tc4revival.client.TaintSporeParticles.maintainSwarm(this);
    }
    public static boolean maySpawn(EntityType<TaintSwarmEntity> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        return level.getMaxLocalRawBrightness(pos)<=random.nextInt(7) && level.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL;
    }

    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) { }
}
