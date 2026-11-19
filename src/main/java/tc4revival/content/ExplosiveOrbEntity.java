package tc4revival.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

public  class ExplosiveOrbEntity extends ThrowableProjectile {
    private float strength = 1;
    private boolean fire;
    public ExplosiveOrbEntity(EntityType<? extends ExplosiveOrbEntity> type, Level level) { super(type, level); }
    public ExplosiveOrbEntity(Level level, LivingEntity owner, float strength, boolean fire) {
        this(TCEntities.EXPLOSIVE_ORB.get(), level);
        FocusProjectiles.launch(this, owner, 1.5f, 0);
        this.strength = strength; this.fire = fire;
    }
    @Override protected void defineSynchedData() {}
    @Override protected float getGravity() { return .01f; }
    @Override protected void onHit(HitResult hit) {
        if (!level().isClientSide && !isRemoved()) {
            if (hit instanceof EntityHitResult entityHit) {
                var type = getOwner() == null ? DamageTypes.UNATTRIBUTED_FIREBALL : DamageTypes.FIREBALL;
                var source = new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), this, getOwner());
                entityHit.getEntity().hurt(source, strength * 1.5f);
            }
            level().explode(this, getX(), getY(), getZ(), strength, fire, Level.ExplosionInteraction.NONE);
        }
        discard();
    }
    @Override public void tick() {
        super.tick(); if (tickCount > 500) discard();
        if (level().isClientSide) tc4revival.client.FocusProjectileEffects.fireTrail(this);
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        markHurt(); if (source.getEntity() == null) return false;
        setDeltaMovement(source.getEntity().getLookAngle().scale(.9)); return true;
    }
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { super.addAdditionalSaveData(tag);tag.putFloat("Strength",strength);tag.putBoolean("Ignition",fire);tag.putInt("FlightAge",tickCount); }
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { super.readAdditionalSaveData(tag);if(tag.contains("Strength"))strength=Float.isFinite(tag.getFloat("Strength"))?Math.max(0,tag.getFloat("Strength")):1;fire=tag.getBoolean("Ignition");tickCount=Math.max(0,tag.getInt("FlightAge")); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
