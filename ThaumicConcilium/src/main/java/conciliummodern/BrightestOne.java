package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tc4revival.content.*;

/** Fifteen short circuits, not ordinary weapon damage, defeat this summoned spirit. */
public final class BrightestOne extends FlyingMob implements Enemy {
    private static final String[] ASPECTS={"ignis","aer","vacuos","tenebrae"};
    private static final EntityDataAccessor<String> ASPECT=SynchedEntityData.defineId(BrightestOne.class,EntityDataSerializers.STRING);
    private final ServerBossEvent bar=new ServerBossEvent(Component.translatable("entity.thaumicconcilium.brightest_one"),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
    private int circuits,courseCooldown,aggroCooldown,attackCounter;
    private boolean continuous,rewarded;
    private Vec3 waypoint=Vec3.ZERO;
    public BrightestOne(EntityType<? extends BrightestOne> type,Level level){super(type,level);xpReward=5;setPersistenceRequired();}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,15).add(Attributes.ATTACK_DAMAGE,25);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ASPECT,"ignis");}
    public String aspectTag(){return entityData.get(ASPECT);}
    @Override public void tick(){super.tick();if(level().isClientSide)BrightestRenderer.effects(this);}
    @Override public void setHealth(float ignored){super.setHealth(Math.max(0,getMaxHealth()-circuits));}
    @Override public boolean hurt(DamageSource source,float damage){
        if(source.getDirectEntity() instanceof BrightestOne || source.getEntity() instanceof BrightestOne)return false;
        if(!level().isClientSide && source.getEntity() instanceof LivingEntity attacker && attacker!=this){setTarget(attacker);aggroCooldown=200;}
        return false;
    }
    public boolean shortCircuit(Entity source){
        if(level().isClientSide || !isAlive() || tickCount<=20 || circuits>=15)return false;
        circuits++;entityData.set(ASPECT,ASPECTS[random.nextInt(ASPECTS.length)]);super.setHealth(Math.max(0,getMaxHealth()-circuits));
        bar.setProgress(getHealth()/getMaxHealth());tc4revival.network.FocusEffectPacket.burst(level(),position().add(0,1.5,0),1);
        if(circuits>=15)die(damageSources().indirectMagic(source,source instanceof net.minecraft.world.entity.projectile.Projectile projectile?projectile.getOwner():source));
        return true;
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit){
        if(rewarded)return;rewarded=true;
        var essence=new ItemStack(TCItems.WISP_ESSENCE.get());var aspects=new tc4revival.aspect.AspectAmounts();
        aspects.add(tc4revival.aspect.TCAspects.catalog().require(aspectTag()),2);((StoredAspectItem)essence.getItem()).setStoredAspects(essence,aspects);
        spawnAtLocation(essence);spawnAtLocation(new ItemStack(ConciliumItems.PRIMORDIAL_LIFE.get()),1.5f);
    }
    @Override public void die(DamageSource source){if(dead)return;super.die(source);if(!level().isClientSide){bar.removeAllPlayers();tc4revival.network.FocusEffectPacket.burst(level(),position().add(0,.45,0),4);}}
    @Override public void startSeenByPlayer(ServerPlayer player){super.startSeenByPlayer(player);bar.addPlayer(player);}
    @Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bar.removePlayer(player);}
    @Override protected int decreaseAirSupply(int air){return air;}
    @Override protected MovementEmission getMovementEmission(){return MovementEmission.NONE;}
    @Override protected boolean shouldDespawnInPeaceful(){return true;}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public void makeStuckInBlock(net.minecraft.world.level.block.state.BlockState state,Vec3 motion){}
    @Override protected SoundEvent getAmbientSound(){return TCSounds.WISP_LIVE.get();}
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.LAVA_EXTINGUISH;}
    @Override protected SoundEvent getDeathSound(){return TCSounds.WISP_DEAD.get();}
    @Override protected float getSoundVolume(){return .25f;}
    @Override protected void customServerAiStep(){
        if(level().getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){discard();return;}
        bar.setProgress(getHealth()/getMaxHealth());
        BrightestHoles.tick(this);
        var delta=waypoint.subtract(position());double length=delta.length();
        if(length<1 || length>60)waypoint=position().add((random.nextFloat()*2-1)*16,(random.nextFloat()*2-1)*16,(random.nextFloat()*2-1)*16);
        if(courseCooldown--<=0){courseCooldown=random.nextInt(5)+2;delta=waypoint.subtract(position());length=delta.length();
            if(length>1e-8 && traversable(waypoint,length))setDeltaMovement(getDeltaMovement().add(delta.scale(.1/length)));else waypoint=position();}
        if(getTarget()!=null && (!getTarget().isAlive() || getTarget().isSpectator()))setTarget(null);
        if(--aggroCooldown<=0 || getTarget()==null){setTarget(level().getNearestPlayer(getX(),getY(),getZ(),64,true));if(getTarget()!=null)aggroCooldown=50;}
        var target=getTarget();
        if(target!=null && distanceToSqr(target)<256 && hasLineOfSight(target)){
            getLookControl().setLookAt(target,100,100);setYRot((float)(-Math.atan2(target.getX()-getX(),target.getZ()-getZ())*180/Math.PI));yBodyRot=getYRot();
            attackCounter++;
            if(continuous && attackCounter<10)BrightestAttacks.continuous(this,target);else if(attackCounter>10)continuous=false;
            if(attackCounter==20){continuous=BrightestAttacks.attack(this,target);summonWisps();attackCounter=-20+random.nextInt(20);}
        }else if(attackCounter>0)attackCounter--;
    }
    private boolean traversable(Vec3 destination,double length){
        var step=destination.subtract(position()).scale(1/length);var box=getBoundingBox();
        for(int i=1;i<=Math.ceil(length);i++){box=box.move(step);if(!level().hasChunkAt(BlockPos.containing(box.minX,box.minY,box.minZ)) || !level().hasChunkAt(BlockPos.containing(box.maxX,box.maxY,box.maxZ)) || !level().noCollision(this,box))return false;}
        var pos=BlockPos.containing(destination);if(!level().getWorldBorder().isWithinBounds(pos) || !level().getFluidState(pos).isEmpty())return false;
        for(int i=0;i<11;i++)if(!level().isEmptyBlock(pos.below(i)))return true;return false;
    }
    private void summonWisps(){
        if(level().getEntitiesOfClass(WispEntity.class,getBoundingBox().inflate(32)).size()>=8 || random.nextInt(3)!=2)return;
        for(int i=0,n=3+random.nextInt(3);i<n;i++){var wisp=TCEntities.WISP.get().create(level());if(wisp==null)return;wisp.setPos(position());wisp.setAspectTag(ASPECTS[random.nextInt(4)]);level().addFreshEntity(wisp);}
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("ShortCircuits",circuits);tag.putString("Type",aspectTag());tag.putBoolean("Rewarded",rewarded);tag.putInt("AttackCounter",attackCounter);tag.putBoolean("Continuous",continuous);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);circuits=net.minecraft.util.Mth.clamp(tag.getInt("ShortCircuits"),0,15);String type=tag.getString("Type");entityData.set(ASPECT,java.util.List.of(ASPECTS).contains(type)?type:"ignis");rewarded=tag.getBoolean("Rewarded");attackCounter=net.minecraft.util.Mth.clamp(tag.getInt("AttackCounter"),-20,20);continuous=tag.getBoolean("Continuous");super.setHealth(Math.max(0,getMaxHealth()-circuits));}
}
