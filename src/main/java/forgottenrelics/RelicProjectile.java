package forgottenrelics;

import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;

import java.util.*;

public class RelicProjectile extends Projectile {
    public enum Kind {
        MISSILE,
        CRIMSON,
        ELDRITCH,
        CHAOS,
        BABYLON,
        LUNAR,
        THUNDER,
        SOUL
    }

    private static final EntityDataAccessor<Integer>
            KIND = SynchedEntityData.defineId(RelicProjectile.class, EntityDataSerializers.INT),
            VARIANT = SynchedEntityData.defineId(RelicProjectile.class, EntityDataSerializers.INT);
    private UUID target;
    BlockPos lock;

    public RelicProjectile(EntityType<? extends RelicProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public Kind kind() {
        return Kind.values()[Math.floorMod(entityData.get(KIND), Kind.values().length)];
    }

    public void kind(Kind value) {
        entityData.set(KIND, value.ordinal());
    }

    public int variant() {
        return entityData.get(VARIANT);
    }

    public void variant(int value) {
        entityData.set(VARIANT, Math.floorMod(value, 12));
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(KIND, 0);
        entityData.define(VARIANT, 0);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag n) {
        super.addAdditionalSaveData(n);
        n.putInt("kind", kind().ordinal());
        n.putInt("variant", variant());
        n.putInt("age", tickCount);
        if (target != null) n.putUUID("target", target);
        if (lock != null) n.putLong("lock", lock.asLong());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag n) {
        super.readAdditionalSaveData(n);
        kind(Kind.values()[Math.floorMod(n.getInt("kind"), Kind.values().length)]);
        variant(n.getInt("variant"));
        tickCount = Math.max(0, n.getInt("age"));
        target = n.hasUUID("target") ? n.getUUID("target") : null;
        lock = n.contains("lock") ? BlockPos.of(n.getLong("lock")) : null;
    }

    private boolean eligible(LivingEntity entity) {
        if (!(getOwner() instanceof LivingEntity owner)) return false;
        if (kind() == Kind.CHAOS && entity == owner)
            return entity.isAlive() && !(entity instanceof Player p && p.isCreative());
        return Relics.canAffect(owner, entity);
    }

    private List<LivingEntity> around(Vec3 position, double radius) {
        return level().getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(position, position).inflate(radius),
                        this::eligible);
    }

    @Override
    public void tick() {
        super.tick();
        var kind = kind();
        if (level().isClientSide) {
            setPos(position().add(getDeltaMovement()));
            trail();
            return;
        }
        if (!(getOwner() instanceof LivingEntity owner)
                || !owner.isAlive()
                || owner.level() != level()
                || tickCount
                        > (kind == Kind.CRIMSON || kind == Kind.LUNAR
                                ? 1000
                                : kind == Kind.THUNDER ? 500 : 215)) {
            discard();
            return;
        }
        if (kind == Kind.BABYLON) {
            if (tickCount <= 15) return;
            if (tickCount == 16) {
                var hit = owner.pick(64, 0, false);
                var aim =
                        hit.getType() == HitResult.Type.MISS
                                ? owner.getEyePosition().add(owner.getLookAngle().scale(64))
                                : hit.getLocation();
                setDeltaMovement(aim.subtract(position()).normalize().scale(3));
            }
        }
        if (kind == Kind.SOUL) {
            var direction = owner.getEyePosition().subtract(position());
            if (direction.lengthSqr() < 1) {
                owner.heal(1);
                if (owner instanceof Player p) p.getFoodData().eat(1, 1);
                Relics.burst(level(), position());
                discard();
                return;
            }
            setDeltaMovement(getDeltaMovement().scale(.7).add(direction.normalize().scale(.3)));
            setPos(position().add(getDeltaMovement()));
            return;
        }
        if (kind == Kind.MISSILE || kind == Kind.CRIMSON || kind == Kind.CHAOS) home(owner);
        if (kind == Kind.THUNDER) setDeltaMovement(getDeltaMovement().add(0, -.05, 0));
        Vec3 start = position(), end = start.add(getDeltaMovement());
        boolean lunarImpact = kind == Kind.LUNAR && lock != null && end.y <= lock.getY() + 1;
        if (lunarImpact) end = new Vec3(lock.getX() + .5, lock.getY() + 1, lock.getZ() + .5);
        BlockHitResult block =
                level().clip(
                                new ClipContext(
                                        start,
                                        end,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        this));
        boolean blocks = kind != Kind.MISSILE && kind != Kind.CHAOS && kind != Kind.LUNAR;
        if (block.getType() != HitResult.Type.MISS
                && level().getBlockState(block.getBlockPos()).is(BlockTags.LEAVES)) blocks = false;
        double best =
                blocks && block.getType() != HitResult.Type.MISS
                        ? start.distanceToSqr(block.getLocation())
                        : start.distanceToSqr(end) + 1;
        LivingEntity victim = null;
        Vec3 hitPosition = null;
        for (var entity :
                level().getEntitiesOfClass(
                                LivingEntity.class,
                                getBoundingBox()
                                        .expandTowards(getDeltaMovement())
                                        .inflate(kind == Kind.BABYLON ? 2 : .5),
                                this::eligible)) {
            if (entity == owner && tickCount < 5) continue;
            var hit =
                    entity.getBoundingBox()
                            .inflate(kind == Kind.BABYLON ? 2 : .25)
                            .clip(start, end);
            if (hit.isPresent() && start.distanceToSqr(hit.get()) < best) {
                best = start.distanceToSqr(hit.get());
                victim = entity;
                hitPosition = hit.get();
            }
        }
        if (victim != null) {
            if (kind == Kind.LUNAR) hit(victim, "magic", value("damageLunarFlareDirect"), false);
            else {
                setPos(hitPosition);
                impact(victim);
                return;
            }
        }
        if (lunarImpact) {
            setPos(end);
            impact(null);
            return;
        }
        if (blocks && block.getType() != HitResult.Type.MISS) {
            setPos(block.getLocation());
            impact(null);
            return;
        }
        setPos(end);
    }

    private void home(LivingEntity owner) {
        var server = (ServerLevel) level();
        var living =
                target == null
                        ? null
                        : server.getEntity(target) instanceof LivingEntity e ? e : null;
        if (living == null || !eligible(living) || distanceToSqr(living) > 1024) {
            var candidates =
                    around(position(), 32).stream()
                            .filter(e -> kind() != Kind.CRIMSON || e.hasLineOfSight(this))
                            .filter(e -> e != owner || random.nextFloat() >= .8f)
                            .toList();
            living =
                    candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
            target = living == null ? null : living.getUUID();
        }
        if (living != null) {
            var direction = living.getBoundingBox().getCenter().subtract(position()).normalize();
            double speed = kind() == Kind.CRIMSON ? .3 : kind() == Kind.CHAOS ? .5 : 1.25;
            setDeltaMovement(getDeltaMovement().scale(.75).add(direction.scale(speed * .25)));
        } else if (kind() == Kind.MISSILE && tickCount % 10 == 0)
            setDeltaMovement(
                    new Vec3(
                                    random.nextDouble() - .5,
                                    random.nextDouble() - .5,
                                    random.nextDouble() - .5)
                            .normalize()
                            .scale(.6));
    }

    private float value(String key) {
        return (float) RelicsConfig.value(key);
    }

    private float between(String prefix) {
        float a = value(prefix + "MIN"), b = value(prefix + "MAX");
        return Math.min(a, b) + random.nextFloat() * Math.abs(b - a);
    }

    private void hit(LivingEntity target, String type, float amount, boolean reset) {
        if (reset) target.invulnerableTime = 0;
        target.hurt(Relics.damage(level(), type, getOwner()), amount);
    }

    private void impact(LivingEntity direct) {
        switch (kind()) {
            case MISSILE -> {
                if (direct != null) hit(direct, "magic", between("nuclearFuryDamage"), false);
            }
            case CRIMSON -> {
                if (direct != null) hit(direct, "magic", between("crimsonSpellDamage"), false);
            }
            case ELDRITCH -> {
                if (direct != null)
                    for (var target : around(position(), 1)) {
                        boolean outer = Relics.outer(level());
                        hit(
                                target,
                                "dark_matter",
                                value(outer ? "eldritchSpellDamageEx" : "eldritchSpellDamage"),
                                false);
                        target.addEffect(
                                new MobEffectInstance(
                                        MobEffects.WEAKNESS, outer ? 320 : 160, outer ? 2 : 1));
                        target.addEffect(
                                new MobEffectInstance(
                                        MobEffects.MOVEMENT_SLOWDOWN,
                                        outer ? 400 : 100,
                                        outer ? 2 : 1));
                        target.addEffect(
                                new MobEffectInstance(
                                        MobEffects.WITHER, outer ? 250 : 200, outer ? 3 : 0));
                    }
            }
            case CHAOS -> {
                if (direct != null)
                    hit(
                            direct,
                            "magic",
                            1 + random.nextFloat() * value("chaosTomeDamageCap"),
                            false);
                level().explode(
                                this,
                                getX(),
                                getY(),
                                getZ(),
                                1 + random.nextFloat() * 6,
                                Level.ExplosionInteraction.MOB);
            }
            case BABYLON, LUNAR -> {
                boolean lunar = kind() == Kind.LUNAR;
                if (direct != null)
                    hit(
                            direct,
                            "magic",
                            value(lunar ? "damageLunarFlareDirect" : "damageApotheosisDirect"),
                            false);
                for (var target : around(position(), lunar ? 2.5 : 3)) {
                    if (target != direct)
                        hit(
                                target,
                                "magic",
                                value(lunar ? "damageLunarFlareImpact" : "damageApotheosisImpact"),
                                false);
                    Relics.motion(
                            target,
                            target.getDeltaMovement()
                                    .add(
                                            target.position()
                                                    .subtract(position())
                                                    .normalize()
                                                    .scale(.8)));
                }
                Relics.sound(
                        level(),
                        position(),
                        lunar ? RelicSounds.LUNAR.get() : SoundEvents.GENERIC_EXPLODE);
            }
            case THUNDER -> {
                if (direct != null)
                    hit(direct, "lightning", value("damageThunderpealDirect"), true);
                for (var target : around(position(), 4)) {
                    if (target == direct) continue;
                    hit(target, "lightning", value("damageThunderpealBolt"), true);
                    RelicsNetwork.beam(
                            (ServerLevel) level(), position(), target.getEyePosition(), 0xBFE5FF);
                    var chain = new ArrayList<>(around(target.position(), 4));
                    chain.remove(target);
                    while (chain.size() > 3) chain.remove(random.nextInt(chain.size()));
                    for (var next : chain) {
                        hit(next, "lightning", value("damageThunderpealBolt") / 2, true);
                        RelicsNetwork.beam(
                                (ServerLevel) level(),
                                target.getEyePosition(),
                                next.getEyePosition(),
                                0xBFE5FF);
                    }
                }
            }
            default -> {}
        }
        Relics.burst(level(), position());
        discard();
    }

    public int color() {
        return switch (kind()) {
            case CRIMSON -> 0xFF2828;
            case ELDRITCH -> 0x4F226F;
            case CHAOS -> 0x9B3FFF;
            case BABYLON -> 0xFFDD35;
            case LUNAR, THUNDER -> 0x44E9FF;
            case SOUL -> 0x39F28A;
            default -> 0x4AFF99;
        };
    }

    private void trail() {
        int color = color();
        var dust =
                new DustParticleOptions(
                        new org.joml.Vector3f(
                                ((color >> 16) & 255) / 255f,
                                ((color >> 8) & 255) / 255f,
                                (color & 255) / 255f),
                        kind() == Kind.LUNAR ? 1.5f : .8f);
        Vec3 movement = getDeltaMovement();
        int count = Math.min(24, Math.max(1, (int) (movement.length() * 8)));
        for (int i = 0; i < count; i++) {
            var pos = position().subtract(movement.scale((double) i / count));
            level().addParticle(dust, pos.x, pos.y, pos.z, 0, 0, 0);
        }
    }
}
