package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumcraft")
public  class EldritchCrabEntity extends Monster {
    private static final EntityDataAccessor<Byte> FLAGS=SynchedEntityData.defineId(EldritchCrabEntity.class,EntityDataSerializers.BYTE);
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void followPlayer(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        for(var passenger:event.player.getPassengers())
            if(passenger instanceof EldritchCrabEntity)event.player.positionRider(passenger);
    }
    private int biteDelay;
    private record Brood(MobEffect effect) implements SpawnGroupData {}
    public EldritchCrabEntity(EntityType<? extends EldritchCrabEntity> type,Level level) {
        super(type,level); xpReward=6;
        if (getNavigation() instanceof GroundPathNavigation navigation) navigation.setCanOpenDoors(true);
        setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER,8);
    }
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.MOVEMENT_SPEED,.3);
    }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(FLAGS,(byte)0); }
    public boolean hasHelm() { return (entityData.get(FLAGS)&1)!=0; }
    @Override public boolean startRiding(Entity vehicle,boolean force) {
        boolean mounted=super.startRiding(vehicle,force);
        if(mounted)syncPlayerVehicle(vehicle);
        return mounted;
    }
    @Override public void removeVehicle() {
        Entity vehicle=getVehicle();
        super.removeVehicle();
        if(vehicle!=null&&getVehicle()!=vehicle)syncPlayerVehicle(vehicle);
    }
    private static void syncPlayerVehicle(Entity vehicle) {
        // ServerEntity broadcasts passenger changes to trackers, excluding the player being tracked.
        // That player's own client also needs the relationship before it can position/render this crab.
        // Send after the change, including an empty list on dismount; leave other trackers to vanilla.
        if(vehicle instanceof ServerPlayer player&&player.connection!=null)
            player.connection.send(new ClientboundSetPassengersPacket(vehicle));
    }
    public void synchronizePassengerPosition() {
        var vehicle=getVehicle();if(vehicle==null)return;
        vehicle.positionRider(this);
        double dx=getX()-vehicle.getX(),dy=getY()-vehicle.getY(),dz=getZ()-vehicle.getZ();
        xOld=vehicle.xOld+dx;yOld=vehicle.yOld+dy;zOld=vehicle.zOld+dz;
        xo=vehicle.xo+dx;yo=vehicle.yo+dy;zo=vehicle.zo+dz;
    }
    /** Render anchoring cannot depend on whether a local player's passenger was entity-ticked. */
    public net.minecraft.world.phys.Vec3 ridingRenderPosition(float partial) {
        var vehicle=getVehicle();
        if(vehicle==null)return getPosition(partial);
        return vehicle.getPosition(partial).add(0,vehicle.getPassengersRidingOffset()+getMyRidingOffset(),0);
    }
    public void setHelm(boolean value) { entityData.set(FLAGS,(byte)(value ? entityData.get(FLAGS)|1 : entityData.get(FLAGS)&~1)); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this)); goalSelector.addGoal(2,new LeapAtTargetGoal(this,.63f));
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1,false)); goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(8,new RandomLookAroundGoal(this)); targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,null));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,LivingEntity.class,0,true,false,entity->{
            return entity instanceof CultistEntity;
        }));
    }
    @Override public boolean canPickUpLoot() { return false; }
    @Override public int getArmorValue() { return hasHelm() ? 5 : 0; }
    @Override public MobType getMobType() { return MobType.ARTHROPOD; }
    @Override public boolean isAlliedTo(Entity entity) { return entity instanceof EldritchCrabEntity; }
    @Override public boolean canBeAffected(MobEffectInstance effect) { return effect.getEffect()!=MobEffects.POISON && super.canBeAffected(effect); }
    @Override public double getMyRidingOffset() { return isPassenger() ? .5 : 0; }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag) {
        setHelm(level.getDifficulty()==Difficulty.HARD || random.nextFloat()<.33f);
        if (group==null) {
            MobEffect effect=null;
            if (level.getDifficulty()==Difficulty.HARD && random.nextFloat()<.1f*difficulty.getSpecialMultiplier()) {
                effect=switch(random.nextInt(5)) { case 0,1->MobEffects.MOVEMENT_SPEED; case 2->MobEffects.DAMAGE_BOOST; case 3->MobEffects.REGENERATION; default->MobEffects.INVISIBILITY; };
            }
            group=new Brood(effect);
        }
        if (group instanceof Brood brood && brood.effect()!=null) addEffect(new MobEffectInstance(brood.effect(),Integer.MAX_VALUE));
        // TC4 only changed speed when the shell broke, not when finalizeSpawn assigned it.
        return super.finalizeSpawn(level,difficulty,reason,group,tag);
    }
    @Override public void tick() {
        super.tick(); if (tickCount<20) fallDistance=0;
        if (level().isClientSide) return;
        if (biteDelay>0) biteDelay--;
        LivingEntity revenge=getLastHurtByMob();
        if (!isPassenger() && revenge!=null && revenge.getPassengers().isEmpty() && !onGround() && !hasHelm() && revenge.isAlive()
                && getY()-revenge.getY()>=revenge.getBbHeight()/2 && distanceToSqr(revenge)<4) startRiding(revenge,true);
        if (isPassenger() && biteDelay<=0) {
            biteDelay=10+random.nextInt(10); doHurtTarget(getVehicle());
            if (isPassenger() && random.nextFloat()<.2f) stopRiding();
        }
    }
    @Override public boolean doHurtTarget(Entity target) {
        boolean hit=super.doHurtTarget(target); if (hit) playSound(TCSounds.CRAB_CLAW.get(),1,.9f+random.nextFloat()*.2f); return hit;
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        boolean hit=super.hurt(source,amount);
        if (hasHelm() && getHealth()/getMaxHealth()<=.5f) {
            setHelm(false); getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.3); level().broadcastEntityEvent(this,(byte)61);
        }
        return hit;
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) { }
    @Override public void handleEntityEvent(byte event) {
        if (event!=61) { super.handleEntityEvent(event); return; }
        if (!level().isClientSide) return;
        var plate=TCEldritch.armor(CultistEquipment.Armor.PLATE_CHEST).getDefaultInstance();
        if (!isSilent()) level().playLocalSound(getX(),getY(),getZ(),net.minecraft.sounds.SoundEvents.ITEM_BREAK,getSoundSource(),.8f,.8f+random.nextFloat()*.4f,false);
        var particle=new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM,plate);
        for (int i=0;i<5;i++) {
            var velocity=new net.minecraft.world.phys.Vec3((random.nextFloat()-.5)*.1,random.nextDouble()*.1+.1,0)
                    .xRot(-getXRot()*(float)Math.PI/180).yRot(-getYRot()*(float)Math.PI/180);
            var origin=new net.minecraft.world.phys.Vec3((random.nextFloat()-.5)*.3,-random.nextFloat()*.6-.3,.6)
                    .xRot(-getXRot()*(float)Math.PI/180).yRot(-getYRot()*(float)Math.PI/180).add(getX(),getEyeY(),getZ());
            level().addParticle(particle,origin.x,origin.y,origin.z,velocity.x,velocity.y+.05,velocity.z);
        }
    }
    @Override public int getAmbientSoundInterval() { return 160; }
    @Override protected SoundEvent getAmbientSound() { return TCSounds.CRAB_TALK.get(); }
    @Override protected SoundEvent getDeathSound() { return TCSounds.CRAB_DEATH.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.HOSTILE_HURT; }
    @Override protected void playStepSound(BlockPos pos,BlockState block) { playSound(SoundEvents.SPIDER_STEP,.15f,1); }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putByte("Flags",entityData.get(FLAGS)); }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); entityData.set(FLAGS,tag.getByte("Flags"));
        if (!hasHelm()) getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.3);
    }
}
