package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** A flying hostile mob whose summoned variants retain TC4's short, target-bound life. */
public  class FireBatEntity extends Monster {
    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(FireBatEntity.class, EntityDataSerializers.BYTE);
    private BlockPos destination;
    private Player vampireOwner;
    private float damageBonus;
    private int attackDelay;

    public FireBatEntity(EntityType<? extends FireBatEntity> type, Level level) { super(type, level); setNoGravity(true); }
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 5).add(Attributes.ATTACK_DAMAGE, 1).add(Attributes.FOLLOW_RANGE, 12);
    }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(FLAGS, (byte) 1); }
    @Override protected void registerGoals() { }
    private boolean flag(int bit) { return (entityData.get(FLAGS) & (1 << bit)) != 0; }
    private void flag(int bit, boolean value) { int flags = entityData.get(FLAGS); entityData.set(FLAGS, (byte) (value ? flags | (1 << bit) : flags & ~(1 << bit))); }
    public boolean hanging() { return flag(0); }
    public boolean summoned() { return flag(1); }
    public boolean explosive() { return flag(2); }
    public boolean devil() { return flag(3); }
    public boolean vampire() { return flag(4); }
    public void summon(Player player, LivingEntity target, int potency, boolean bombs, boolean devil, boolean vampire) { summon(player, target, (float) potency, bombs, devil, vampire); }
    public void summon(Player player, LivingEntity target, float potency, boolean bombs, boolean devil, boolean vampire) {
        damageBonus = Mth.clamp(potency, 0, 1029);
        flag(0, false); flag(1, true); flag(2, bombs); flag(3, devil); flag(4, vampire);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue((devil ? 3 : 2) + damageBonus);
        // The legacy devil upgrade changes damage, but does not raise an already spawned bat's health.
        if (vampire) vampireOwner = player;
        setTarget(target);
    }
    @Override protected void customServerAiStep() {
        if (attackDelay > 0) attackDelay--;
        var target = getTarget();
        if (target != null && (!target.isAlive() || target.isRemoved() || target.level() != level()
                || target instanceof Player player && (player.getAbilities().invulnerable || player.isSpectator()))) setTarget(null);
        if (getTarget() == null && !summoned()) setTarget(findVulnerablePlayer());
        target = getTarget();
        BlockPos ceiling = BlockPos.containing(getX(), (int) getY() + 1, getZ());
        boolean canHang = level().getBlockState(ceiling).isRedstoneConductor(level(), ceiling);
        if (hanging()) {
            if (!canHang || level().getNearestPlayer(getX(), getY(), getZ(), 4, false) != null) {
                flag(0, false); level().levelEvent(null, 1025, blockPosition(), 0);
            } else if (random.nextInt(200) == 0) setYHeadRot(random.nextInt(360));
            return;
        }
        if (target == null) {
            if (summoned()) hurt(damageSources().generic(), 2);
            if (!isAlive()) return;
            if (destination != null && (!level().isEmptyBlock(destination) || destination.getY() < 1)) destination = null;
            if (destination == null || random.nextInt(30) == 0 || destination.distSqr(blockPosition()) < 4)
                destination = blockPosition().offset(random.nextInt(7) - random.nextInt(7), random.nextInt(6) - 2, random.nextInt(7) - random.nextInt(7));
            steer(Vec3.atCenterOf(destination).add(0, -.4, 0));
            if (random.nextInt(100) == 0 && canHang) flag(0, true);
        } else {
            steer(target.position().add(0, target.getEyeHeight() * .66, 0));
            if (hasLineOfSight(target)) strike(target);
        }
    }
    private Player findVulnerablePlayer() {
        Player nearest = null;
        double distance = Double.MAX_VALUE;
        for (var player : level().players()) {
            if (!player.isAlive() || player.isSpectator() || player.getAbilities().invulnerable) continue;
            double range = 12;
            if (player.isShiftKeyDown()) range *= (double) .8f;
            if (player.isInvisible()) {
                int equipped = 0;
                for (var armor : player.getArmorSlots()) if (!armor.isEmpty()) equipped++;
                range *= (double) (.7f * Math.max(.1f, equipped / 4f));
            }
            double candidate = distanceToSqr(player);
            if (candidate < range * range && candidate < distance) { nearest = player; distance = candidate; }
        }
        return nearest;
    }
    private void steer(Vec3 at) {
        var difference = at.subtract(position()); var velocity = getDeltaMovement();
        setDeltaMovement(velocity.add((Math.signum(difference.x) * .5 - velocity.x) * .1,
                (Math.signum(difference.y) * .7 - velocity.y) * .1, (Math.signum(difference.z) * .5 - velocity.z) * .1));
        setYRot((float) Math.toDegrees(Math.atan2(getDeltaMovement().z, getDeltaMovement().x)) - 90);
        setYBodyRot(getYRot());
    }
    private void strike(LivingEntity target) {
        if (attackDelay > 0 || distanceTo(target) >= Math.max(2.5, target.getBbWidth() * 1.1)
                || target.getBoundingBox().maxY <= getBoundingBox().minY || target.getBoundingBox().minY >= getBoundingBox().maxY) return;
        if (summoned()) {
            LegacyCombatCredit.set(target,100);
        }
        if (vampire()) {
            if (vampireOwner != null && !vampireOwner.hasEffect(MobEffects.REGENERATION)) vampireOwner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 26, 1));
            heal(1);
        }
        attackDelay = 20;
        // TC4 draws combat choices from the world, not the bat's flight RNG.
        if ((explosive() || level().random.nextInt(10) == 0) && !devil()) {
            target.invulnerableTime = 0;
            level().explode(this, getX(), getY(), getZ(), 1.5f + (explosive() ? damageBonus * .33f : 0), false, Level.ExplosionInteraction.NONE);
            discard();
        } else if (vampire() || level().random.nextBoolean()) {
            var velocity = target.getDeltaMovement(); doHurtTarget(target); target.setDeltaMovement(velocity); target.hasImpulse = false;
        } else target.setSecondsOnFire(summoned() ? 4 : 2);
        playSound(SoundEvents.BAT_HURT, .5f, .9f + level().random.nextFloat() * .2f);
    }
    @Override public void travel(Vec3 input) {
        if (isEffectiveAi()) { move(MoverType.SELF, getDeltaMovement()); setDeltaMovement(getDeltaMovement().multiply(.91, .98, .91)); }
        calculateEntityAnimation(false);
    }
    @Override public void aiStep() { if (isInWaterRainOrBubble()) hurt(damageSources().drown(), 1); super.aiStep(); }
    @Override public void tick() {
        super.tick();
        if (hanging()) { setDeltaMovement(Vec3.ZERO); setPos(getX(), Mth.floor(getY()) + 1 - getBbHeight(), getZ()); }
        else setDeltaMovement(getDeltaMovement().multiply(1, .6, 1));
        if (level().isClientSide) {
            if (explosive()) tc4revival.client.FocusProjectileEffects.batTrail(this);
            if (!vampire()) {
                double x = xo + (random.nextFloat() - random.nextFloat()) * .2;
                double y = yo + getBbHeight() / 2 + (random.nextFloat() - random.nextFloat()) * .2;
                double z = zo + (random.nextFloat() - random.nextFloat()) * .2;
                level().addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0); level().addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
            }
        }
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION)) return false;
        if (!level().isClientSide) flag(0, false);
        return super.hurt(source, amount);
    }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isIgnoringBlockTriggers() { return true; }
    @Override protected MovementEmission getMovementEmission() { return MovementEmission.NONE; }
    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }
    @Override protected void checkFallDamage(double distance, boolean ground, BlockState state, BlockPos pos) { }
    @Override protected SoundEvent getAmbientSound() { return hanging() && random.nextInt(4) != 0 ? null : SoundEvents.BAT_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.BAT_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.BAT_DEATH; }
    @Override protected float getSoundVolume() { return .1f; }
    @Override public float getVoicePitch() { return super.getVoicePitch() * .95f; }
    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) { }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putByte("BatFlags", entityData.get(FLAGS)); tag.putFloat("damBonus", damageBonus); }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); entityData.set(FLAGS, (byte) (tag.getByte("BatFlags") & 31)); damageBonus = Mth.clamp(tag.getFloat("damBonus"), 0, 1029);
        vampireOwner = null; destination = null; setNoGravity(true);
    }
}
