package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/** Armored first phase followed by a regenerating, headless homing-shot phase. */
public  class EldritchGolemEntity extends ThaumcraftBossEntity implements EldritchMob,RangedAttackMob {
    private static final EntityDataAccessor<Boolean> HEADLESS=SynchedEntityData.defineId(EldritchGolemEntity.class,EntityDataSerializers.BOOLEAN);
    private int attackTimer,beamCharge;
    private int arcTicks;
    private BlockPos arcTarget=BlockPos.ZERO;
    private boolean charging;
    private RangedAttackGoal ranged;
    public EldritchGolemEntity(EntityType<? extends EldritchGolemEntity> type,Level level) { super(type,level); }
    public static AttributeSupplier.Builder attributes() { return bossAttributes().add(Attributes.MAX_HEALTH,250).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.ATTACK_DAMAGE,10); }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(HEADLESS,false); }
    public boolean headless() { return entityData.get(HEADLESS); }
    public int attackTimer() { return attackTimer; }
    public int beamCharge() { return beamCharge; }
    public boolean charging() { return charging; }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this)); goalSelector.addGoal(3,new MeleeAttackGoal(this,1.1,false));
        goalSelector.addGoal(6,new MoveTowardsRestrictionGoal(this,.8)); goalSelector.addGoal(7,new RandomStrollGoal(this,.8));
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8)); goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,null));
    }
    private void enableRanged() {
        if (ranged!=null) return;
        ranged=new RangedAttackGoal(this,1,5,5,24) {
            @Override public boolean canUse() { return super.canUse() && getTarget()!=null && getTarget().isAlive() && distanceToSqr(getTarget())>=9; }
        };
        goalSelector.addGoal(2,ranged);
    }
    @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions dimensions) { return headless() ? 3.33f : 3; }
    @Override public int getArmorValue() { return super.getArmorValue()+6; }
    @Override public boolean fireImmune() { return true; }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag) {
        spawnTimer=100; return super.finalizeSpawn(world,difficulty,reason,group,tag);
    }
    @Override public boolean hurt(DamageSource source,float damage) {
        // The original compares the raw incoming amount, before the common boss cap/armor.
        if (!level().isClientSide && damage>getHealth() && !headless()) {
            entityData.set(HEADLESS,true); spawnTimer=100;
            float angle=getYRot()%360*Mth.DEG_TO_RAD;
            level().explode(this,getX()+Mth.cos(angle)*.75,getEyeY(),getZ()+Mth.sin(angle)*.75,2,false,Level.ExplosionInteraction.NONE);
            enableRanged(); return false;
        }
        return super.hurt(source,damage);
    }
    @Override public boolean doHurtTarget(Entity target) {
        if (attackTimer>0) return false;
        attackTimer=10; level().broadcastEntityEvent(this,(byte)4);
        boolean hit=target.hurt(level().damageSources().mobAttack(this),(float)getAttributeValue(Attributes.ATTACK_DAMAGE)*.75f);
        if (hit) {
            target.push(0,.2000000059604645,0);
            if (headless()) target.push(-Mth.sin(getYRot()*Mth.DEG_TO_RAD)*1.5,.1,Mth.cos(getYRot()*Mth.DEG_TO_RAD)*1.5);
        }
        return hit;
    }
    @Override public void performRangedAttack(LivingEntity target,float power) {
        if (level().isClientSide || charging || beamCharge<=0 || !getSensing().hasLineOfSight(target)) return;
        beamCharge-=15+random.nextInt(5); getLookControl().setLookAt(target.getX(),target.getY()+target.getBbHeight()/2,target.getZ(),30,30);
        var shot=new GolemOrbEntity(level(),this,target,false); var look=getLookAngle(); shot.setPos(shot.position().add(look.x,0,look.z));
        shot.shoot(target.getX()+target.getDeltaMovement().x-getX(),target.getY()-getY()-target.getBbHeight()/2,target.getZ()+target.getDeltaMovement().z-getZ(),.66f,5);
        playSound(TCSounds.ELDRITCH_ATTACK.get(),1,1+random.nextFloat()*.1f); level().addFreshEntity(shot);
    }
    @Override public void aiStep() {
        super.aiStep(); if (attackTimer>0) attackTimer--;
        var motion=getDeltaMovement();
        if (motion.horizontalDistanceSqr()>2.500000277905201e-7 && random.nextInt(5)==0) {
            var pos=BlockPos.containing(getX(),getY()-.2f,getZ()); var state=level().getBlockState(pos);
            if (!state.isAir() && level().isClientSide) level().addParticle(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,state),
                    getX()+(random.nextFloat()-.5)*getBbWidth(),getBoundingBox().minY+.1,getZ()+(random.nextFloat()-.5)*getBbWidth(),4*(random.nextFloat()-.5),.5,4*(random.nextFloat()-.5));
            if (!level().isClientSide && state.getBlock() instanceof EldritchLootBlock) level().destroyBlock(pos,true,this);
        }
        if (!level().isClientSide) {
            var pos=BlockPos.containing(getX()+motion.x,getBoundingBox().minY,getZ()+motion.z); var state=level().getBlockState(pos); float hardness=state.getDestroySpeed(level(),pos);
            if (hardness>=0 && hardness<=.15f) level().destroyBlock(pos,true,this);
        }
    }
    @Override public void tick() {
        if (!level().isClientSide && spawnTimer==150) level().broadcastEntityEvent(this,(byte)18);
        if (spawnTimer>0) heal(2); super.tick();
        if (!level().isClientSide && headless()) {
            if (beamCharge<=0) charging=true;
            if (charging) { beamCharge++; level().broadcastEntityEvent(this,(byte)19); if (beamCharge==150) charging=false; }
        }
        if (level().isClientSide && headless()) {
            setXRot(0); float angle=yBodyRot*Mth.DEG_TO_RAD;
            double x=-Mth.sin(angle),z=Mth.cos(angle);
            if (random.nextInt(20)==0) tc4revival.client.EldritchSparkParticles.emit(level(),getX()+x+(random.nextFloat()-random.nextFloat())/2,
                    getEyeY()-.25,getZ()+z+(random.nextFloat()-random.nextFloat())/2,.3f,.65f+random.nextFloat()*.1f,1,1,.8f);
            tc4revival.client.EssentiaVentParticles.emit(level(),(float)getX()+x*.66,getEyeY()-.75,(float)getZ()+z*.66,0,.001,0,0x555555,4);
            if (arcTicks>0) {
                tc4revival.client.EldritchArcEffects.emit(level(),position().add(0,getBbHeight()/2,0),net.minecraft.world.phys.Vec3.atBottomCenterOf(arcTarget.above()),
                        .65f+random.nextFloat()*.1f,1,1,1-arcTicks/10f); arcTicks--;
            }
        }
    }
    @Override public void handleEntityEvent(byte event) {
        if (event==4) { attackTimer=10; playSound(SoundEvents.IRON_GOLEM_ATTACK,1,1); }
        else if (event==18) spawnTimer=150;
        else if (event==19) {
            if (arcTicks!=0) return;
            float radius=2+random.nextFloat()*2; double angle=Math.toRadians(random.nextInt(360));
            var pos=BlockPos.containing(getX()+radius*Math.cos(angle),getY(),getZ()+radius*Math.sin(angle));
            for (int step=0;step<5 && level().isEmptyBlock(pos);step++) pos=pos.below();
            if (level().isEmptyBlock(pos.above()) && !level().isEmptyBlock(pos)) {
                arcTarget=pos; arcTicks=8+random.nextInt(5);
                level().playLocalSound(getX(),getY(),getZ(),TCSounds.JACOBS.get(),getSoundSource(),.8f,1+(random.nextFloat()-random.nextFloat())*.05f,false);
            }
        }
        else super.handleEntityEvent(event);
    }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }
    @Override protected void playStepSound(BlockPos pos,BlockState state) { playSound(SoundEvents.IRON_GOLEM_STEP,1,1); }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putBoolean("headless",headless()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); entityData.set(HEADLESS,tag.getBoolean("headless")); if (headless()) enableRanged(); }
}
