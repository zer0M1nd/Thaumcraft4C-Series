package tc4revival.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public  class EmberEntity extends ThrowableProjectile implements IEntityAdditionalSpawnData {
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("thaumcraft:ember"));
    private int duration = 20, fire = 0;
    private float damage = 1;
    public EmberEntity(EntityType<? extends EmberEntity> type, Level level) { super(type, level); }
    public EmberEntity(Level level, LivingEntity owner, float scatter, float damage, int fire, int duration) {
        this(TCEntities.EMBER.get(), level); FocusProjectiles.launch(this, owner, 1, scatter);
        this.damage = damage; this.fire = fire; this.duration = duration;
    }
    public int duration() { return duration; }
    @Override protected void defineSynchedData() {}
    @Override protected float getGravity() { return 0; }
    @Override public void tick() {
        if (tickCount > duration) discard();
        setDeltaMovement(getDeltaMovement().scale((duration <= 20 ? .95 : .975) * (onGround() ? .66 : 1)));
        super.tick();
    }
    @Override protected void onHit(HitResult hit) {
        if (!level().isClientSide && !isRemoved()) {
            if (hit instanceof EntityHitResult entityHit) {
                var target = entityHit.getEntity();
                // Fire damage without the projectile tag: projectile protection did not protect against embers.
                var source = new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_TYPE), this, getOwner());
                if (!target.fireImmune() && target.hurt(source, damage)) target.setSecondsOnFire(3 + fire);
            } else if (hit instanceof BlockHitResult blockHit && random.nextFloat() < .025f * fire) {
                var target = blockHit.getBlockPos().relative(blockHit.getDirection());
                if (level().hasChunkAt(target) && level().isEmptyBlock(target)) level().setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
            }
        }
        discard();
    }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putFloat("damage", damage); tag.putInt("firey", fire); tag.putInt("duration", duration);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        float value = tag.getFloat("damage"); damage = Float.isFinite(value) ? Mth.clamp(value, 0, 100) : 0;
        fire = Mth.clamp(tag.getInt("firey"), 0, 5); duration = Mth.clamp(tag.getInt("duration"), 1, 100);
    }
    @Override public void writeSpawnData(FriendlyByteBuf data) { data.writeByte(duration); }
    @Override public void readSpawnData(FriendlyByteBuf data) { duration = Math.max(1, data.readUnsignedByte()); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
