package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import tc4revival.network.FocusEffectPacket;

public  class ShockOrbEntity extends ThrowableProjectile {
    private int area = 4;
    private float damage = 5;
    public ShockOrbEntity(EntityType<? extends ShockOrbEntity> type, Level level) { super(type, level); }
    public ShockOrbEntity(Level level, LivingEntity owner, int area, int damage) { this(level, owner, area, (float) damage); }
    public ShockOrbEntity(Level level, LivingEntity owner, int area, float damage) {
        this(TCEntities.SHOCK_ORB.get(), level);
        setOwner(owner); this.area = area; this.damage = damage;
        float yaw = owner.getYRot() * Mth.DEG_TO_RAD;
        setPos(owner.getX() - Mth.cos(yaw) * .16f, owner.getEyeY() - .1, owner.getZ() - Mth.sin(yaw) * .16f);
        setYRot(owner.getYRot()); setXRot(owner.getXRot());
        var direction = owner.getLookAngle();
        shoot(direction.x, direction.y, direction.z, 1.5f, 1);
    }
    @Override protected void defineSynchedData() {}
    @Override protected float getGravity() { return .05f; }
    @Override public void tick() { super.tick(); if (tickCount > 500) discard(); }
    @Override protected void onHit(HitResult hit) {
        if (level().isClientSide || isRemoved()) return;
        if(hit instanceof net.minecraft.world.phys.BlockHitResult block && getOwner() instanceof net.minecraft.server.level.ServerPlayer player)
            CopperProcessing.shock(player,player.getMainHandItem(),block);
        var origin = position();
        for (var entity : level().getEntities(this, new AABB(origin, origin).inflate(area))) {
            if (visibleFromPosition(entity.position())) entity.hurt(level().damageSources().indirectMagic(this, getOwner()), damage);
        }
        for (int attempt = 0; attempt < 20; attempt++) {
            int x = Mth.floor(getX()) + random.nextInt(area) - random.nextInt(area);
            int z = Mth.floor(getZ()) + random.nextInt(area) - random.nextInt(area);
            int y = Math.min(level().getMaxBuildHeight() - 2, Mth.floor(getY()) + area);
            int bottom = Math.max(level().getMinBuildHeight(), Mth.floor(getY()) - area);
            var ground = new BlockPos.MutableBlockPos(x, y, z);
            if (!level().hasChunkAt(ground)) continue;
            while (ground.getY() > bottom && level().isEmptyBlock(ground)) ground.move(0, -1, 0);
            var above = ground.above();
            if (!level().isEmptyBlock(ground) && level().isEmptyBlock(above)
                    && !level().getBlockState(above).is(TCArcane.SHOCK_FIELD.get())
                    && visibleFromPosition(Vec3.atCenterOf(above)))
                level().setBlock(above, TCArcane.SHOCK_FIELD.get().defaultBlockState(), 3);
        }
        FocusEffectPacket.burst(level(), origin, 3);
        level().playSound(null, getX(), getY(), getZ(), TCSounds.SHOCK.get(), SoundSource.PLAYERS, 1,
                1 + (random.nextFloat() - random.nextFloat()) * .2f);
        discard();
    }
    private boolean visibleFromPosition(Vec3 destination) {
        // TC4's area visibility uses entity positions, unlike the focus aiming ray.
        return level().clip(new net.minecraft.world.level.ClipContext(position(), destination,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        markHurt();
        if (source.getEntity() == null) return false;
        setDeltaMovement(source.getEntity().getLookAngle().scale(.9));
        level().playSound(null, getX(), getY(), getZ(), TCSounds.ZAP.get(), SoundSource.PLAYERS, 1,
                1 + (random.nextFloat() - random.nextFloat()) * .2f);
        return true;
    }
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { super.addAdditionalSaveData(tag);tag.putFloat("Damage",damage);tag.putInt("Area",area);tag.putInt("FlightAge",tickCount); }
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { super.readAdditionalSaveData(tag);if(tag.contains("Damage"))damage=Float.isFinite(tag.getFloat("Damage"))?Math.max(0,tag.getFloat("Damage")):5;if(tag.contains("Area"))area=Math.max(1,Math.min(128,tag.getInt("Area")));tickCount=Math.max(0,tag.getInt("FlightAge")); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
