package thaumicfutures.foci;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public class GrappleEntity extends Projectile implements IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<Boolean> PULLING=SynchedEntityData.defineId(GrappleEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> STICKY=SynchedEntityData.defineId(GrappleEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> OWNER=SynchedEntityData.defineId(GrappleEntity.class,EntityDataSerializers.INT);
    private static final Map<ServerLevel,Map<UUID,UUID>> ACTIVE=new WeakHashMap<>();
    private float potency;
    private int age,stalled,previousDistance;
    private boolean boost,registered;
    private BlockPos anchor;
    public float amplitude;
    public GrappleEntity(EntityType<? extends GrappleEntity> type,Level level) { super(type,level); }
    public GrappleEntity(Level level,Player owner,InteractionHand hand,float potency,boolean sticky) {
        this(FutureFoci.GRAPPLE_ENTITY.get(),level);setOwner(owner);entityData.set(OWNER,owner.getId());entityData.set(STICKY,sticky);
        this.potency=Mth.clamp(potency,0,100);float yaw=owner.getYRot()*Mth.DEG_TO_RAD;
        var arm=hand==InteractionHand.MAIN_HAND?owner.getMainArm():owner.getMainArm().getOpposite();float offset=arm==HumanoidArm.RIGHT?-.16f:.16f;
        setPos(owner.getX()+Mth.cos(yaw)*offset,owner.getEyeY()-.1,owner.getZ()+Mth.sin(yaw)*offset);
        var look=owner.getLookAngle();shoot(look.x,look.y,look.z,3,0);
    }
    @Override protected void defineSynchedData() { entityData.define(PULLING,false);entityData.define(STICKY,false);entityData.define(OWNER,-1); }
    @Override public Entity getOwner() { var owner=super.getOwner();return owner!=null?owner:level().getEntity(entityData.get(OWNER)); }
    public boolean pulling() { return entityData.get(PULLING); }
    public boolean sticky() { return entityData.get(STICKY); }
    public void replacePrevious() {
        if(level() instanceof ServerLevel server && getOwner()!=null) {
            var map=ACTIVE.computeIfAbsent(server,k -> new HashMap<>());var old=map.put(getOwner().getUUID(),getUUID());
            if(old!=null && !old.equals(getUUID())) { var entity=server.getEntity(old);if(entity instanceof GrappleEntity)entity.discard(); }
            registered=true;
        }
    }
    @Override public void remove(RemovalReason reason) {
        if(level() instanceof ServerLevel server && getOwner()!=null) {
            var map=ACTIVE.get(server);if(map!=null)map.remove(getOwner().getUUID(),getUUID());
        }
        super.remove(reason);
    }
    @Override public void tick() {
        super.tick();age++;var owner=getOwner();
        if(!level().isClientSide && (!(owner instanceof LivingEntity living) || !living.isAlive() || owner.level()!=level()
                || owner.isShiftKeyDown() || owner.isSpectator() || !pulling() && age>10+potency*5)) { discard();return; }
        if(!level().isClientSide && owner!=null) {
            entityData.set(OWNER,owner.getId());
            if(!registered)replacePrevious();
        }
        if(!pulling()) {
            var hit=ProjectileUtil.getHitResultOnMoveVector(this,this::canHitEntity);
            if(hit.getType()!=HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this,hit)) onHit(hit);
            if(!pulling()) { setPos(position().add(getDeltaMovement()));setDeltaMovement(getDeltaMovement().scale(.99).add(0,-.03,0)); }
        }
        if(pulling() && owner!=null && !level().isClientSide) {
            if(anchor!=null && (!level().hasChunkAt(anchor) || level().getBlockState(anchor).getCollisionShape(level(),anchor).isEmpty())) { discard();return; }
            double distance=distanceTo(owner);
            if(!sticky() && (distance<2 || stalled>20)) { discard();return; }
            owner.fallDistance=0;
            double divisor=sticky() && distance<8 ? distance*(8-distance) : distance;
            var pull=divisor>1e-6?position().subtract(owner.position()).scale(1/(divisor*5)):Vec3.ZERO;
            if(pull.lengthSqr()>.0625)pull=pull.normalize().scale(.25);
            owner.setDeltaMovement(owner.getDeltaMovement().add(pull).add(0,.033+(boost?0:.4),0));boost=true;owner.hasImpulse=true;
            if(owner instanceof ServerPlayer player) {
                tc4revival.content.ElementalSwordCombat.resetFloating(player);
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
            }
            int d=(int)(distance/2);stalled=d==previousDistance?stalled+1:0;previousDistance=d;
        }
        if(level().isClientSide)amplitude=pulling()?amplitude*.66f:amplitude+.02f;
    }
    @Override protected void onHit(HitResult hit) {
        setPos(hit.getLocation());setDeltaMovement(Vec3.ZERO);entityData.set(PULLING,true);hasImpulse=true;
        if(hit instanceof BlockHitResult block)anchor=block.getBlockPos().immutable();
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);tag.putFloat("Potency",potency);tag.putBoolean("Sticky",sticky());tag.putBoolean("Pulling",pulling());
        tag.putInt("Age",age);tag.putBoolean("Boost",boost);if(anchor!=null)tag.putLong("Anchor",anchor.asLong());
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);float p=tag.getFloat("Potency");potency=Float.isFinite(p)?Mth.clamp(p,0,100):0;
        entityData.set(STICKY,tag.getBoolean("Sticky"));entityData.set(PULLING,tag.getBoolean("Pulling"));
        age=Math.max(0,tag.getInt("Age"));boost=tag.getBoolean("Boost");anchor=tag.contains("Anchor")?BlockPos.of(tag.getLong("Anchor")):null;
        replacePrevious();
    }
    @Override public void writeSpawnData(FriendlyByteBuf buf) { buf.writeFloat(potency); }
    @Override public void readSpawnData(FriendlyByteBuf buf) { potency=buf.readFloat(); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
