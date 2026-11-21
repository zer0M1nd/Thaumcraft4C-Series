package thaumicfutures.foci;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

/** TC5's pulsing homing missile: steer each second, slow between pulses, rebound off walls. */
public class HomingShardEntity extends Projectile implements IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<Float> STRENGTH=SynchedEntityData.defineId(HomingShardEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TARGET=SynchedEntityData.defineId(HomingShardEntity.class,EntityDataSerializers.INT);
    private UUID targetUuid;
    private ResourceLocation targetType;
    private boolean persistent;
    private int age;
    public final Deque<Vec3> trail=new ArrayDeque<>();
    public HomingShardEntity(EntityType<? extends HomingShardEntity> type,Level level) { super(type,level); }
    public HomingShardEntity(Level level,LivingEntity owner,LivingEntity target,float strength,boolean persistent) {
        this(FutureFoci.SHARD_ENTITY.get(),level);setOwner(owner);setTarget(target);this.persistent=persistent;
        entityData.set(STRENGTH,Mth.clamp(strength,0,100));
        setPos(owner.getEyePosition().add(owner.getLookAngle().scale(.5)));
        float yaw=(owner.getYRot()+(random.nextFloat()-random.nextFloat())*60)*Mth.DEG_TO_RAD;
        float pitch=(owner.getXRot()+(random.nextFloat()-random.nextFloat())*60)*Mth.DEG_TO_RAD;
        setDeltaMovement(-Mth.sin(yaw)*Mth.cos(pitch)*.5,-Mth.sin(pitch)*.5,Mth.cos(yaw)*Mth.cos(pitch)*.5);
    }
    @Override protected void defineSynchedData() { entityData.define(STRENGTH,0f);entityData.define(TARGET,-1); }
    public float strength() { return entityData.get(STRENGTH); }
    public boolean persistent() { return persistent; }
    public LivingEntity target() {
        Entity e=level() instanceof ServerLevel server && targetUuid!=null ? server.getEntity(targetUuid) : level().getEntity(entityData.get(TARGET));
        return e instanceof LivingEntity living ? living : null;
    }
    public void setTarget(LivingEntity target) {
        targetUuid=target.getUUID();targetType=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());entityData.set(TARGET,target.getId());
    }
    private boolean valid(LivingEntity e) { return e!=null && e.isAlive() && !e.isSpectator() && e!=getOwner(); }
    @Override public void tick() {
        if(level().isClientSide) { trail.addFirst(position());while(trail.size()>6)trail.removeLast(); }
        super.tick();age++;
        LivingEntity target=target();
        if(!level().isClientSide) {
            if(persistent && (!valid(target) || distanceToSqr(target)>1250)) {
                var type=targetType;
                target=level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(16),e -> valid(e)
                        && net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).equals(type)
                        && distanceToSqr(e)<=256).stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
                if(target!=null)setTarget(target);
            }
            if(!valid(target) || age>300) { burst();return; }
            // Entity ids change on reload; refresh the client's target from the saved UUID.
            entityData.set(TARGET,target.getId());
        }
        var hit=ProjectileUtil.getHitResultOnMoveVector(this,this::canHitEntity);
        boolean impact=hit.getType()!=HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this,hit);
        if(impact)onHit(hit);
        if(isRemoved())return;
        if(!impact || !(hit instanceof BlockHitResult))setPos(position().add(getDeltaMovement()));
        if(age%20==0 && valid(target)) {
            var delta=new Vec3(target.getX()-getX(),target.getY()+target.getBbHeight()*.6-getY(),target.getZ()-getZ());
            setDeltaMovement(delta.normalize().scale(.85));
        } else setDeltaMovement(getDeltaMovement().scale(.99*.85));
    }
    @Override protected void onHitEntity(EntityHitResult hit) {
        if(!level().isClientSide && hit.getEntity()!=getOwner()) {
            hit.getEntity().hurt(damageSources().indirectMagic(this,getOwner()),1+strength()*.5f);
            playSound(FutureFoci.ZAP.get(),1,1+(random.nextFloat()-random.nextFloat())*.2f);burst();
        }
    }
    @Override protected void onHitBlock(BlockHitResult hit) {
        var motion=getDeltaMovement();
        setDeltaMovement(switch(hit.getDirection().getAxis()) { case X -> motion.multiply(-.8,1,1);case Y -> motion.multiply(1,-.8,1);case Z -> motion.multiply(1,1,-.8); });
        setPos(hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(.015)));hasImpulse=true;
    }
    private void burst() { level().broadcastEntityEvent(this,(byte)16);discard(); }
    @Override public void handleEntityEvent(byte event) {
        if(event==16) { if(level().isClientSide)thaumicfutures.foci.client.FocusVisuals.burst(this); }
        else super.handleEntityEvent(event);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);tag.putFloat("Strength",strength());tag.putBoolean("Persistent",persistent);tag.putInt("Age",age);
        if(targetUuid!=null)tag.putUUID("Target",targetUuid);if(targetType!=null)tag.putString("TargetType",targetType.toString());
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);float strength=tag.getFloat("Strength");entityData.set(STRENGTH,Float.isFinite(strength)?Mth.clamp(strength,0,100):0);
        persistent=tag.getBoolean("Persistent");age=Mth.clamp(tag.getInt("Age"),0,301);
        targetUuid=tag.hasUUID("Target")?tag.getUUID("Target"):null;targetType=ResourceLocation.tryParse(tag.getString("TargetType"));
    }
    @Override public void writeSpawnData(FriendlyByteBuf buf) { buf.writeInt(getOwner()==null?-1:getOwner().getId());buf.writeBoolean(persistent);buf.writeInt(age); }
    @Override public void readSpawnData(FriendlyByteBuf buf) { setOwner(level().getEntity(buf.readInt()));persistent=buf.readBoolean();age=buf.readInt(); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
