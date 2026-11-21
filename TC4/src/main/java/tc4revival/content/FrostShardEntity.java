package tc4revival.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public  class FrostShardEntity extends ThrowableProjectile implements IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(FrostShardEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> FROST = SynchedEntityData.defineId(FrostShardEntity.class, EntityDataSerializers.INT);
    private double bounce = .5;
    private int bounces = 3;
    private boolean fragile;
    public FrostShardEntity(EntityType<? extends FrostShardEntity> type, Level level) { super(type, level); }
    public FrostShardEntity(Level level, LivingEntity owner, float scatter, float damage, int frost, boolean fragile, boolean boulder) {
        this(TCEntities.FROST_SHARD.get(), level);
        FocusProjectiles.launch(this, owner, 1.5f, scatter);
        setDamage(damage); entityData.set(FROST, frost); this.fragile = fragile;
        if (boulder) { bounce = .8; bounces = 6; }
    }
    @Override protected void defineSynchedData() { entityData.define(DAMAGE, 0f); entityData.define(FROST, 0); }
    public float damage() { return entityData.get(DAMAGE); }
    public int frost() { return entityData.get(FROST); }
    private void setDamage(float value) { entityData.set(DAMAGE, Float.isFinite(value) ? Mth.clamp(value, 0, 100) : 0); }
    @Override public EntityDimensions getDimensions(Pose pose) {
        float size = .15f + damage() * .15f;
        return EntityDimensions.scalable(size, size);
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DAMAGE.equals(key)) refreshDimensions();
    }
    @Override protected float getGravity() { return fragile ? .015f : .05f; }
    @Override protected void onHit(HitResult hit) {
        if (isRemoved()) return;
        var motion = getDeltaMovement();
        var state = Blocks.ICE.defaultBlockState();
        if (hit instanceof EntityHitResult entityHit) {
            var target = entityHit.getEntity();
            motion = motion.multiply(Mth.floor(getX()) != Mth.floor(target.getX()) ? -1 : 1,
                    Mth.floor(getY()) != Mth.floor(target.getY()) ? -.9 : 1,
                    Mth.floor(getZ()) != Mth.floor(target.getZ()) ? -1 : 1).scale(.66);
        } else if (hit instanceof BlockHitResult blockHit) {
            motion = switch (blockHit.getDirection().getAxis()) {
                case X -> motion.multiply(-1, 1, 1);
                case Y -> motion.multiply(1, -.9, 1);
                case Z -> motion.multiply(1, 1, -1);
            };
            state = level().getBlockState(blockHit.getBlockPos());
            playSound(state.getSoundType(level(), blockHit.getBlockPos(), this).getBreakSound(), .3f, breakPitch());
        }
        if (level().isClientSide) tc4revival.client.FocusProjectileEffects.impact(this, state, Mth.ceil(damage()), hit instanceof EntityHitResult);
        motion = motion.scale(bounce); setDeltaMovement(motion);
        // Vec3.normalize handles a stationary impact without TC4's division by zero.
        setPos(position().subtract(motion.normalize().scale(.05))); markHurt();
        if (!level().isClientSide && hit instanceof EntityHitResult entityHit) {
            var target = entityHit.getEntity(); var before = target.getDeltaMovement();
            boolean accepted=target.hurt(level().damageSources().thrown(this, getOwner()), damage());
            if (accepted && target instanceof LivingEntity living && frost() > 0) living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, frost() - 1));
            if (fragile) {
                if(accepted) {
                    target.invulnerableTime = 0;
                    target.setDeltaMovement(before.add(target.getDeltaMovement().subtract(before).scale(.1)));
                }
                discard(); iceBreak();
            }
        }
        // Post-decrement is intentional: the ordinary shard survives three impacts and breaks on the fourth.
        if (bounces-- <= 0) {
            if (level().isClientSide) tc4revival.client.FocusProjectileEffects.impact(this, Blocks.ICE.defaultBlockState(), Mth.ceil(damage() * 8), true);
            discard(); iceBreak();
        }
    }
    private float breakPitch() { return 1.2f / (random.nextFloat() * .2f + .9f); }
    private void iceBreak() { playSound(net.minecraft.sounds.SoundEvents.GLASS_BREAK, .3f, breakPitch()); }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide && frost() > 0) tc4revival.client.FocusProjectileEffects.frost(this);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putFloat("damage", damage()); tag.putBoolean("fragile", fragile); tag.putInt("frost", frost());
        tag.putDouble("Bounce",bounce);tag.putInt("BouncesRemaining",bounces);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); setDamage(tag.getFloat("damage")); fragile = tag.getBoolean("fragile"); entityData.set(FROST, Mth.clamp(tag.getInt("frost"), 0, 5));
        double saved=tag.contains("Bounce")?tag.getDouble("Bounce"):.5;
        bounce=Double.isFinite(saved)?Mth.clamp(saved,0,1):.5;
        bounces=tag.contains("BouncesRemaining")?Mth.clamp(tag.getInt("BouncesRemaining"),0,64):3;
    }
    @Override public void writeSpawnData(FriendlyByteBuf data) { data.writeDouble(bounce); data.writeInt(bounces); data.writeBoolean(fragile); }
    @Override public void readSpawnData(FriendlyByteBuf data) { bounce = data.readDouble(); bounces = data.readInt(); fragile = data.readBoolean(); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
