package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.PlayerKnowledge;
import tc4revival.network.TCNetwork;
import tc4revival.network.WarpGainPacket;

public class EldritchGuardianEntity extends Monster implements RangedAttackMob,EldritchMob {
    public float leftArmLift,rightArmLift;
    private boolean lastRight;
    public EldritchGuardianEntity(EntityType<? extends EldritchGuardianEntity> type,Level level) {
        super(type,level); xpReward=20; if (getNavigation() instanceof GroundPathNavigation navigation) navigation.setCanOpenDoors(true);
    }
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,50).add(Attributes.FOLLOW_RANGE,40)
                .add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.ATTACK_DAMAGE,7).add(Attributes.ARMOR,4);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new RangedAttackGoal(this,1,20,40,24) {
            @Override public boolean canUse() {
                if (!super.canUse()) return false;
                var target=getTarget(); if (target==null) return false;
                if (!target.isAlive()) { setTarget(null); return false; }
                return distanceToSqr(target)>=64;
            }
            @Override public boolean canContinueToUse() {
                return attackableTarget(getTarget()) && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1,false));
        goalSelector.addGoal(5,new MoveTowardsRestrictionGoal(this,.8));
        goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,1));
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8)); goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,null));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,LivingEntity.class,0,true,false,
                entity->entity instanceof CultistEntity));
    }
    @Override public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level,MobSpawnType reason) {
        return level.getDifficulty()!=Difficulty.PEACEFUL && level().getEntitiesOfClass(EldritchGuardianEntity.class,
                new net.minecraft.world.phys.AABB(getX(),getY(),getZ(),getX()+1,getY()+1,getZ()+1).inflate(32,16,32)).isEmpty();
    }
    @Override public boolean canPickUpLoot() { return false; }
    private static boolean attackableTarget(LivingEntity target) {
        return target != null && target.isAlive()
                && (!(target instanceof Player player) || !player.isCreative() && !player.isSpectator());
    }
    @Override public void setTarget(LivingEntity target) { super.setTarget(attackableTarget(target) ? target : null); }
    @Override public boolean canAttack(LivingEntity target) { return attackableTarget(target) && super.canAttack(target); }
    @Override public MobType getMobType() { return MobType.UNDEAD; }
    @Override public int getArmorValue() { return 4; }
    @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size) { return 2.1f; }
    @Override public boolean hurt(DamageSource source,float damage) {
        if (source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC) || source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)) damage*=.5f;
        return super.hurt(source,damage);
    }
    @Override public boolean doHurtTarget(Entity target) {
        if (target instanceof LivingEntity living && !attackableTarget(living)) return false;
        boolean hit=super.doHurtTarget(target); int difficulty=level().getDifficulty().getId();
        if (hit && getMainHandItem().isEmpty() && isOnFire() && random.nextFloat()<difficulty*.3f) target.setSecondsOnFire(2*difficulty);
        return hit;
    }
    @Override public boolean isAlliedTo(Entity entity) { return entity instanceof EldritchMob; }
    @Override public boolean removeWhenFarAway(double distance) { return !hasRestriction(); }
    @Override protected SoundEvent getAmbientSound() { return TCSounds.ELDRITCH_IDLE.get(); }
    @Override protected SoundEvent getDeathSound() { return TCSounds.ELDRITCH_DEATH.get(); }
    @Override public int getAmbientSoundInterval() { return 500; }
    @Override protected float getSoundVolume() { return 1.5f; }
    public boolean inOuterLands() { return level().dimension().equals(TCEldritch.OUTER_LANDS); }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag) {
        var result=super.finalizeSpawn(world,difficulty,reason,group,tag);
        if (inOuterLands()) setAbsorptionAmount(getAbsorptionAmount()+(int)getAttribute(Attributes.MAX_HEALTH).getBaseValue()/2);
        return result;
    }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (inOuterLands() && invulnerableTime<=0 && tickCount%25==0
                && getAbsorptionAmount()<(int)getAttribute(Attributes.MAX_HEALTH).getBaseValue()/2) setAbsorptionAmount(getAbsorptionAmount()+1);
    }
    @Override public void tick() {
        // Warp events may explicitly assign a player, and game mode can change after
        // a ranged goal has already cached its victim.
        if (!level().isClientSide && getTarget() != null && !attackableTarget(getTarget())) setTarget(null);
        super.tick();
        if (level().isClientSide) {
            if (leftArmLift>0) leftArmLift-=.05f; if (rightArmLift>0) rightArmLift-=.05f;
            tc4revival.client.EldritchWisps.shroud(this);
        } else if (!inOuterLands() && tickCount%100==0 && level().getDifficulty()!=Difficulty.EASY) {
            double range=level().getDifficulty()==Difficulty.HARD ? 576 : 256;
            for (var player:((net.minecraft.server.level.ServerLevel)level()).players())
                if (player.isAlive() && player.distanceToSqr(this)<range)
                    TCNetwork.send(player,new tc4revival.network.FocusEffectPacket(level().dimension().location(),
                            tc4revival.network.FocusEffectPacket.Kind.MIST_SHORT,position(),position(),1,0));
        }
    }
    @Override public void handleEntityEvent(byte event) {
        if (event==15) leftArmLift=.5f;
        else if (event==16) rightArmLift=.5f;
        else if (event==17) { leftArmLift=.9f; rightArmLift=.9f; }
        else super.handleEntityEvent(event);
    }
    @Override public void performRangedAttack(LivingEntity target,float power) {
        if (level().isClientSide || !attackableTarget(target)) return;
        if (random.nextFloat()>.1f) {
            var orb=new EldritchOrbEntity(level(),this); lastRight=!lastRight;
            level().broadcastEntityEvent(this,(byte)(lastRight ? 16 : 15));
            float angle=(getYRot()+(lastRight ? 90 : 180))%360*(float)Math.PI/180;
            orb.setPos(orb.getX()-net.minecraft.util.Mth.cos(angle)*.5,orb.getY()-.057777777*getBbHeight(),orb.getZ()-net.minecraft.util.Mth.sin(angle)*.5);
            orb.shoot(target.getX()+target.getDeltaMovement().x-getX(),target.getY()-getY()-target.getBbHeight()/2,
                    target.getZ()+target.getDeltaMovement().z-getZ(),1,2);
            playSound(TCSounds.ELDRITCH_ATTACK.get(),2,1+random.nextFloat()*.1f); level().addFreshEntity(orb);
        } else if (getSensing().hasLineOfSight(target)) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER,400,0));
            if (target instanceof ServerPlayer player) addTemporaryWarp(player,1+random.nextInt(3));
            playSound(TCSounds.ELDRITCH_SCREECH.get(),3,1+random.nextFloat()*.1f);
            tc4revival.network.FocusEffectPacket.sonic(this);
        }
    }
    public static void addTemporaryWarp(ServerPlayer player,int amount) {
        if (tc4revival.config.TCConfig.SERVER.disableWarp.get()) return;
        KnowledgeSavedData.get(player.server).update(player.getUUID(),player.getRandom()::nextInt,knowledge->{
            knowledge.changeWarp(PlayerKnowledge.WarpKind.TEMPORARY,amount); knowledge.setWarpCounter(knowledge.totalWarp()); return null;
        });
        TCNetwork.send(player,new WarpGainPacket(PlayerKnowledge.WarpKind.TEMPORARY,amount));
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) { }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hasRestriction()) { tag.putLong("Home",getRestrictCenter().asLong()); tag.putInt("HomeDistance",(int)getRestrictRadius()); }
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HomeDistance")) restrictTo(BlockPos.of(tag.getLong("Home")),Math.max(0,tag.getInt("HomeDistance")));
    }
}
