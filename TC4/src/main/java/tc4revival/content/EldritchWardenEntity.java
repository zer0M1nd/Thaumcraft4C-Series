package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import tc4revival.network.FocusEffectPacket;

/** Outer Lands shielded boss and its one-time radial field retaliation. */
public  class EldritchWardenEntity extends ThaumcraftBossEntity implements RangedAttackMob,EldritchMob {
    private static final EntityDataAccessor<Byte> TITLE=SynchedEntityData.defineId(EldritchWardenEntity.class,EntityDataSerializers.BYTE);
    private static final String[] TITLES={"Aphoom-Zhah","Basatan","Chaugnar Faugn","Mnomquah","Nyogtha","Oorn","Shaikorth","Rhan-Tegoth","Rhogog","Shudde M'ell","Vulthoom","Yag-Kosha","Yibb-Tstll","Zathog","Zushakon"};
    private boolean frenzyUsed,lastRight;
    private int frenzyTicks;
    public float leftArmLift,rightArmLift;
    public EldritchWardenEntity(EntityType<? extends EldritchWardenEntity> type,Level level) {
        super(type,level); if (getNavigation() instanceof GroundPathNavigation navigation) navigation.setCanOpenDoors(true);
    }
    public static AttributeSupplier.Builder attributes() {
        return bossAttributes().add(Attributes.MAX_HEALTH,200).add(Attributes.MOVEMENT_SPEED,.33).add(Attributes.ATTACK_DAMAGE,10);
    }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(TITLE,(byte)0); }
    public String title() { return TITLES[Byte.toUnsignedInt(entityData.get(TITLE))%TITLES.length]; }
    @Override public void generateChampionName(ChampionMobs.Kind kind) {
        setCustomName(net.minecraft.network.chat.Component.translatable(getType().getDescriptionId(),title(),ChampionMobs.name(kind)));
    }
    public int frenzyTicks() { return frenzyTicks; }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new RangedAttackGoal(this,1,20,40,24) {
            @Override public boolean canUse() { return super.canUse() && getTarget()!=null && getTarget().isAlive() && distanceToSqr(getTarget())>=9; }
        });
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.1,false));
        goalSelector.addGoal(5,new MoveTowardsRestrictionGoal(this,.8));
        goalSelector.addGoal(7,new RandomStrollGoal(this,1));
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,null));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,LivingEntity.class,0,true,false,entity->{
            return entity instanceof CultistEntity;
        }));
    }
    @Override public int getArmorValue() { return super.getArmorValue()+4; }
    @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size) { return 3.1f; }
    @Override public boolean isEffectiveAi() { return frenzyTicks==0 && super.isEffectiveAi(); }
    @Override public boolean isInvulnerableTo(DamageSource source) { return frenzyTicks>0 || source.is(DamageTypes.DROWN) || source.is(DamageTypes.WITHER) || super.isInvulnerableTo(source); }
    @Override public boolean hurt(DamageSource source,float amount) {
        if (isInvulnerableTo(source)) return false;
        boolean hit=super.hurt(source,amount);
        if (!level().isClientSide && hit && !frenzyUsed && getAbsorptionAmount()<=0) { frenzyUsed=true; frenzyTicks=150; }
        return hit;
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag) {
        spawnTimer=150; entityData.set(TITLE,(byte)random.nextInt(TITLES.length));
        setAbsorptionAmount(getAbsorptionAmount()+(float)(getAttribute(Attributes.MAX_HEALTH).getBaseValue()*.66));
        return super.finalizeSpawn(world,difficulty,reason,group,tag);
    }
    @Override public void tick() {
        if (!level().isClientSide && spawnTimer==150) level().broadcastEntityEvent(this,(byte)18);
        super.tick();
        if (level().isClientSide) {
            if (leftArmLift>0) leftArmLift-=.05f; if (rightArmLift>0) rightArmLift-=.05f;
            tc4revival.client.EldritchWisps.shroud(this,.25);
            if (spawnTimer>0) tc4revival.client.EldritchWisps.spawnSpiral(this,spawnTimer);
        } else if (invulnerableTime<=0 && tickCount%25==0 && getAbsorptionAmount()<(int)(getAttribute(Attributes.MAX_HEALTH).getBaseValue()*.66))
            setAbsorptionAmount(getAbsorptionAmount()+1);
    }
    @Override public void aiStep() {
        super.aiStep(); if (!(level() instanceof ServerLevel server)) return;
        for (int corner=0;corner<4;corner++) {
            var pos=BlockPos.containing(getX()+(corner%2*2-1)*.25,getY(),getZ()+(corner/2%2*2-1)*.25);
            if (server.isEmptyBlock(pos)) server.setBlockAndUpdate(pos,TCEldritch.FIELD.get().defaultBlockState());
        }
        if (frenzyTicks>0) { if (frenzyTicks==150) teleportHome(server); spreadField(server); }
    }
    private void spreadField(ServerLevel server) {
        if (frenzyTicks<121 && frenzyTicks%10==0) {
            server.broadcastEntityEvent(this,(byte)17); int step=1+frenzyTicks/8; double radius=(150-frenzyTicks)/8.0;
            for (int ray=0;ray<180/step;ray++) {
                double angle=Math.toRadians(ray*2*step); var pos=blockPosition().offset((int)(radius*Math.cos(angle)),0,(int)(radius*Math.sin(angle)));
                if (!server.isEmptyBlock(pos) || !server.getBlockState(pos.below()).isRedstoneConductor(server,pos.below())) continue;
                server.setBlockAndUpdate(pos,TCEldritch.FIELD.get().defaultBlockState()); server.scheduleTick(pos,TCEldritch.FIELD.get(),250+random.nextInt(150));
                if (random.nextFloat()<.3f) FocusEffectPacket.lightning(server,Vec3.atCenterOf(pos),getBoundingBox().getCenter());
                else FocusEffectPacket.blockSparkle(server,pos,0x800080);
            }
            playSound(TCSounds.ZAP.get(),1,.9f+random.nextFloat()*.1f);
        }
        frenzyTicks--;
    }
    private void teleportHome(ServerLevel server) {
        var home=getRestrictCenter();
        var event=new net.minecraftforge.event.entity.EntityTeleportEvent(this,home.getX(),home.getY(),home.getZ());
        if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)) return;
        Vec3 old=position(); int baseX=Mth.floor(event.getTargetX()),y=Mth.floor(event.getTargetY()),baseZ=Mth.floor(event.getTargetZ());
        int x=baseX,z=baseZ; boolean found=false;
        if (server.hasChunkAt(new BlockPos(x,y,z))) for (int attempt=0;attempt<20;attempt++) {
            var pos=new BlockPos(x,y,z);
            if (server.getBlockState(pos.below()).blocksMotion() && !server.getBlockState(pos).blocksMotion()) { found=true; break; }
            x=baseX+random.nextInt(8)-random.nextInt(8); z=baseZ+random.nextInt(8)-random.nextInt(8);
        }
        if (!found) return;
        setPos(x+.5,y+.1,z+.5); if (!server.noCollision(this)) { setPos(old); return; }
        for (int index=0;index<128;index++) {
            var at=old.lerp(position(),index/127.0).add((random.nextDouble()-.5)*getBbWidth()*2,random.nextDouble()*getBbHeight(),(random.nextDouble()-.5)*getBbWidth()*2);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,at.x,at.y,at.z,0,(random.nextFloat()-.5)*.2,(random.nextFloat()-.5)*.2,(random.nextFloat()-.5)*.2,1);
        }
        server.playSound(null,old.x,old.y,old.z,SoundEvents.ENDERMAN_TELEPORT,getSoundSource(),1,1); playSound(SoundEvents.ENDERMAN_TELEPORT,1,1);
    }
    @Override public void performRangedAttack(LivingEntity target,float power) {
        if (level().isClientSide) return;
        if (random.nextFloat()>.2f) {
            var orb=new EldritchOrbEntity(level(),this); lastRight=!lastRight; level().broadcastEntityEvent(this,(byte)(lastRight ? 16 : 15));
            float angle=(getYRot()+(lastRight ? 90 : 180))%360*Mth.DEG_TO_RAD;
            orb.setPos(orb.getX()-Mth.cos(angle)*.5,orb.getY()-.13,orb.getZ()-Mth.sin(angle)*.5);
            orb.shoot(target.getX()+target.getDeltaMovement().x-getX(),target.getY()-getY()-target.getBbHeight()/2,target.getZ()+target.getDeltaMovement().z-getZ(),1,2);
            playSound(TCSounds.ELDRITCH_ATTACK.get(),2,1+random.nextFloat()*.1f); level().addFreshEntity(orb);
        } else if (getSensing().hasLineOfSight(target)) {
            target.push(-Mth.sin(getYRot()*Mth.DEG_TO_RAD)*1.5,.1,Mth.cos(getYRot()*Mth.DEG_TO_RAD)*1.5);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER,400,0)); target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,400,0));
            if (target instanceof ServerPlayer player) EldritchGuardianEntity.addTemporaryWarp(player,3+random.nextInt(3));
            playSound(TCSounds.ELDRITCH_SCREECH.get(),4,1+random.nextFloat()*.1f);
            tc4revival.network.FocusEffectPacket.sonic(this);
        }
    }
    @Override public void handleEntityEvent(byte event) {
        switch (event) {
            case 15 -> leftArmLift=.5f; case 16 -> rightArmLift=.5f;
            case 17 -> { leftArmLift=.9f; rightArmLift=.9f; } case 18 -> spawnTimer=150;
            default -> super.handleEntityEvent(event);
        }
    }
    @Override protected SoundEvent getAmbientSound() { return TCSounds.ELDRITCH_IDLE.get(); }
    @Override protected SoundEvent getDeathSound() { return TCSounds.ELDRITCH_DEATH.get(); }
    @Override public int getAmbientSoundInterval() { return 500; }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putByte("title",entityData.get(TITLE)); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); entityData.set(TITLE,(byte)(Byte.toUnsignedInt(tag.getByte("title"))%TITLES.length)); }
}
