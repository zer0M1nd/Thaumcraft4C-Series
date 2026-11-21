package tc4revival.content;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import tc4revival.network.FocusEffectPacket;

/** Deflectable homing shot with normalized steering. */
public  class GolemOrbEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> TARGET=SynchedEntityData.defineId(GolemOrbEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RED=SynchedEntityData.defineId(GolemOrbEntity.class,EntityDataSerializers.BOOLEAN);
    private java.util.UUID targetIdentity;
    public GolemOrbEntity(EntityType<? extends GolemOrbEntity> type,Level level) { super(type,level); }
    public GolemOrbEntity(Level level,LivingEntity owner,LivingEntity target,boolean red) {
        this(TCEntities.GOLEM_ORB.get(),level); EldritchProjectileLaunch.initialize(this,owner);
        targetIdentity=target==null?null:target.getUUID();entityData.set(TARGET,target==null ? -1 : target.getId()); entityData.set(RED,red);
    }
    @Override protected void defineSynchedData() { entityData.define(TARGET,-1); entityData.define(RED,false); }
    public boolean red() { return entityData.get(RED); }
    @Override public void shoot(double x,double y,double z,float speed,float spread) { EldritchProjectileLaunch.shoot(this,random,x,y,z,speed,spread); }
    public void advanceCultistLaunch() { var motion=getDeltaMovement(); setPos(getX()+motion.x/2,getY(),getZ()+motion.z/2); }
    @Override protected float getGravity() { return 0; }
    @Override public void tick() {
        super.tick(); if (tickCount>(red() ? 240 : 160)) { discard(); return; }
        if(level() instanceof net.minecraft.server.level.ServerLevel server && targetIdentity!=null && server.getEntity(targetIdentity) instanceof LivingEntity restored)entityData.set(TARGET,restored.getId());
        if (level().getEntity(entityData.get(TARGET)) instanceof LivingEntity target && target.isAlive()) {
            var direction=target.position().add(0,target.getBbHeight()*.6,0).subtract(position());
            var motion=getDeltaMovement().add(direction.normalize().scale(.2));
            setDeltaMovement(Mth.clamp((float)motion.x,-.25f,.25f),Mth.clamp((float)motion.y,-.25f,.25f),Mth.clamp((float)motion.z,-.25f,.25f));
        }
    }
    @Override protected void onHit(HitResult hit) {
        if (level().isClientSide || isRemoved()) return;
        if (hit instanceof EntityHitResult entityHit && getOwner() instanceof LivingEntity owner) {
            var damage=owner.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage!=null) entityHit.getEntity().hurt(level().damageSources().indirectMagic(this,owner),(float)damage.getValue()*(red() ? 1 : .6f));
        }
        playSound(TCSounds.SHOCK.get(),1,1+(random.nextFloat()-random.nextFloat())*.2f);
        FocusEffectPacket.burst(level(),position(),1); discard();
    }
    @Override public boolean hurt(DamageSource source,float damage) {
        if (isInvulnerableTo(source)) return false; markHurt();
        if (source.getEntity()==null) return false;
        setDeltaMovement(source.getEntity().getLookAngle().scale(.9));
        playSound(TCSounds.ZAP.get(),1,1+(random.nextFloat()-random.nextFloat())*.2f); return true;
    }
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);if(targetIdentity!=null)tag.putUUID("TargetIdentity",targetIdentity);tag.putBoolean("Red",red());tag.putInt("FlightAge",tickCount);}
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);targetIdentity=tag.hasUUID("TargetIdentity")?tag.getUUID("TargetIdentity"):null;entityData.set(RED,tag.getBoolean("Red"));tickCount=Math.max(0,tag.getInt("FlightAge"));}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
