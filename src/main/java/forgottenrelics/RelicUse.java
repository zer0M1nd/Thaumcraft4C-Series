package forgottenrelics;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public final class RelicUse {
    /**
     * UUIDs and dimensions only: no player/entity references retained across logout or world
     * unload.
     */
    private record Cast(
            Relic type,
            UUID target,
            ResourceKey<Level> dimension,
            BlockPos block,
            double distance,
            Vec3 anchor) {}

    private static final Map<UUID, Cast> CASTS = new HashMap<>();

    private RelicUse() {}

    public static void clear(LivingEntity entity) {
        CASTS.remove(entity.getUUID());
    }

    public static void validate(ServerPlayer p) {
        if (!p.isAlive() || !p.isUsingItem() || !(p.getUseItem().getItem() instanceof RelicItem))
            clear(p);
    }

    private static void stop(ServerPlayer p) {
        p.stopUsingItem();
        clear(p);
    }

    public static InteractionResultHolder<ItemStack> begin(
            Relic type, Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide) {
            if (continuous(type) && !(type == Relic.DIMENSIONAL_MIRROR && user.isShiftKeyDown()))
                user.startUsingItem(hand);
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(user instanceof ServerPlayer p) || !p.isAlive() || p.isSpectator())
            return InteractionResultHolder.fail(stack);
        var n = stack.getOrCreateTag();
        switch (type) {
            case XP_TOME -> {
                String key = p.isShiftKeyDown() ? "active" : "extract";
                n.putBoolean(key, !n.getBoolean(key));
                Relics.sound(level, p.position(), SoundEvents.EXPERIENCE_ORB_PICKUP);
                return InteractionResultHolder.success(stack);
            }
            case OBLIVION_STONE -> {
                if (p.isShiftKeyDown()) n.putBoolean("disabled", !n.getBoolean("disabled"));
                else n.putInt("mode", (Math.floorMod(n.getInt("mode"), 3) + 1) % 3);
                return InteractionResultHolder.success(stack);
            }
            case GHASTLY_SKULL -> {
                p.setHealth(1);
                return InteractionResultHolder.success(stack);
            }
            case DIMENSIONAL_MIRROR -> {
                if (p.isShiftKeyDown()) {
                    var dest = new CompoundTag();
                    dest.putString("dimension", level.dimension().location().toString());
                    dest.putInt("x", p.blockPosition().getX());
                    dest.putInt("y", p.blockPosition().getY());
                    dest.putInt("z", p.blockPosition().getZ());
                    n.put("destination", dest);
                    Relics.sound(level, p.position(), SoundEvents.ENDERMAN_TELEPORT);
                    return InteractionResultHolder.success(stack);
                }
                if (mirrorLevel(p, stack) == null) return InteractionResultHolder.fail(stack);
            }
            case TELEPORTATION_TOME -> {
                return discord(p)
                        ? InteractionResultHolder.success(stack)
                        : InteractionResultHolder.fail(stack);
            }
            case CRIMSON_SPELL, ELDRITCH_SPELL, THUNDERPEAL -> {
                if (Relics.cooling(p) || !Relics.pay(p, type))
                    return InteractionResultHolder.fail(stack);
                RelicProjectiles.fire(p, type, null);
                Relics.cooldown(p, type == Relic.ELDRITCH_SPELL ? 20 : 30);
                return InteractionResultHolder.success(stack);
            }
            default -> {}
        }
        if (!continuous(type) || Relics.cooling(p)) return InteractionResultHolder.pass(stack);
        if (type == Relic.WEATHER_STONE && !level.isRaining())
            return InteractionResultHolder.fail(stack);
        UUID target = null;
        BlockPos block = null;
        double distance = 7.5;
        if (type == Relic.TELEKINESIS_TOME
                || type == Relic.OVERTHROWER
                || type == Relic.VOID_GRIMOIRE) {
            if (type == Relic.OVERTHROWER && level.dimension() == Level.NETHER
                    || type == Relic.VOID_GRIMOIRE && !RelicsConfig.enabled("voidGrimoireEnabled"))
                return InteractionResultHolder.fail(stack);
            if (type == Relic.TELEKINESIS_TOME
                    && p.getPersistentData().getLong("forgottenrelics:grab_cooldown")
                            > level.getGameTime()) return InteractionResultHolder.fail(stack);
            var entity = Relics.pointed(p, type == Relic.TELEKINESIS_TOME ? 32 : 64);
            if (entity == null || !movable(entity, type))
                return InteractionResultHolder.fail(stack);
            target = entity.getUUID();
            distance = p.distanceTo(entity);
        }
        if (type == Relic.OBELISK_DRAINER) {
            double nearest = 256;
            for (var pos :
                    BlockPos.betweenClosed(
                            p.blockPosition().offset(-16, -16, -16),
                            p.blockPosition().offset(16, 16, 16))) {
                if (!level.hasChunkAt(pos) || !obelisk(level, pos)) continue;
                double d = p.distanceToSqr(Vec3.atCenterOf(pos));
                if (d < nearest) {
                    nearest = d;
                    block = pos.immutable();
                }
            }
            if (block == null) return InteractionResultHolder.fail(stack);
        }
        CASTS.put(
                p.getUUID(),
                new Cast(type, target, level.dimension(), block, distance, p.position()));
        p.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    public static boolean continuous(Relic type) {
        return switch (type) {
            case DIMENSIONAL_MIRROR,
                            WEATHER_STONE,
                            TELEKINESIS_TOME,
                            OBELISK_DRAINER,
                            SOUL_TOME,
                            OVERTHROWER,
                            VOID_GRIMOIRE,
                            MISSILE_TOME,
                            LUNAR_FLARES,
                            APOTHEOSIS,
                            CHAOS_TOME ->
                    true;
            default -> false;
        };
    }

    private static boolean movable(LivingEntity target, Relic type) {
        if (!target.canChangeDimensions() && type == Relic.OVERTHROWER) return false;
        if (type != Relic.TELEKINESIS_TOME) return true;
        if (target instanceof Player && !RelicsConfig.enabled("telekinesisOnPlayers")) return false;
        return !(target instanceof net.minecraft.world.entity.boss.wither.WitherBoss)
                && !(target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)
                && !ForgeRegistries.ENTITY_TYPES
                        .getKey(target.getType())
                        .toString()
                        .equals("botania:doppleganger")
                && !(target instanceof tc4revival.content.ThaumcraftBossEntity);
    }

    private static boolean obelisk(Level level, BlockPos pos) {
        return ForgeRegistries.BLOCKS
                .getKey(level.getBlockState(pos).getBlock())
                .toString()
                .equals("thaumcraft:eldritch_obelisk");
    }

    private static ServerLevel mirrorLevel(ServerPlayer p, ItemStack stack) {
        if (!stack.getOrCreateTag().contains("destination")) return null;
        var id =
                ResourceLocation.tryParse(
                        stack.getOrCreateTag().getCompound("destination").getString("dimension"));
        if (id == null) return null;
        var dimension = ResourceKey.create(Registries.DIMENSION, id);
        if (dimension != p.level().dimension()
                && (!RelicsConfig.enabled("interdimensionalMirror")
                        || p.level().dimension() == Level.END)) return null;
        return p.server.getLevel(dimension);
    }

    public static void tick(Relic type, ItemStack stack, ServerPlayer p, int remaining) {
        int elapsed = stack.getUseDuration() - remaining;
        var cast = CASTS.get(p.getUUID());
        if (cast == null
                || cast.type != type
                || cast.dimension != p.level().dimension()
                || p.getUseItem() != stack) {
            stop(p);
            return;
        }
        LivingEntity target =
                cast.target == null
                        ? null
                        : p.serverLevel().getEntity(cast.target) instanceof LivingEntity living
                                ? living
                                : null;
        if (cast.target != null
                && (target == null
                        || !Relics.canAffect(p, target)
                        || p.distanceToSqr(target)
                                > (type == Relic.TELEKINESIS_TOME ? 1024 : 4096))) {
            stop(p);
            return;
        }
        switch (type) {
            case DIMENSIONAL_MIRROR -> {
                if (elapsed % 4 == 0) Relics.burst(p.level(), p.position());
                if (remaining == 1) {
                    var level = mirrorLevel(p, stack);
                    var n = stack.getOrCreateTag().getCompound("destination");
                    if (level != null)
                        Relics.teleport(
                                p,
                                level,
                                new Vec3(
                                        n.getInt("x") + .5, n.getInt("y") + .5, n.getInt("z") + .5),
                                true);
                }
            }
            case WEATHER_STONE -> {
                if (remaining == 1
                        && p.level().isRaining()
                        && Relics.pay(
                                p,
                                RelicsConfig.value("weatherStoneVisMult"),
                                2500,
                                2500,
                                0,
                                2500,
                                0,
                                0)) {
                    p.serverLevel()
                            .setWeatherParameters(
                                    24000 + p.getRandom().nextInt(976000), 0, false, false);
                    Relics.cooldown(p, 100);
                    Relics.burst(p.level(), p.position());
                }
            }
            case TELEKINESIS_TOME -> {
                if (!movable(target, type) || !Relics.pay(p, type)) {
                    stop(p);
                    return;
                }
                var point =
                        p.getEyePosition()
                                .add(
                                        p.getLookAngle()
                                                .scale(p.isShiftKeyDown() ? cast.distance : 7.5));
                var motion = point.subtract(target.position()).scale(.3);
                if (motion.length() > 3) motion = motion.normalize().scale(3);
                Relics.motion(target, motion);
                target.fallDistance = 0;
                if (elapsed % 4 == 0)
                    RelicsNetwork.beam(
                            p.serverLevel(),
                            p.getEyePosition(),
                            target.getBoundingBox().getCenter(),
                            0xC68FFF);
            }
            case OBELISK_DRAINER -> {
                if (cast.block == null
                        || !obelisk(p.level(), cast.block)
                        || p.distanceToSqr(Vec3.atCenterOf(cast.block)) > 256) {
                    stop(p);
                    return;
                }
                if (elapsed > 0 && elapsed % 30 == 0) {
                    p.hurt(p.damageSources().magic(), .01f);
                    p.heal(4);
                    p.getFoodData().eat(2, 1);
                    Relics.recharge(
                            p,
                            Relics.randomPrimal(p),
                            (int)
                                    ((7.5 + p.getRandom().nextDouble() * 22.5)
                                            * 100
                                            * RelicsConfig.value("obeliskDrainerVisMult")));
                    RelicsNetwork.beam(
                            p.serverLevel(),
                            Vec3.atCenterOf(cast.block).add(0, 2, 0),
                            p.getEyePosition(),
                            0xDF83FF);
                }
            }
            case SOUL_TOME -> {
                Relics.motion(p, new Vec3(0, p.getDeltaMovement().y, 0));
                if (elapsed <= 20) return;
                var targets = Relics.nearby(p, 20);
                for (var other : targets)
                    if (p.distanceToSqr(other) <= 9
                            && Relics.pay(
                                    p,
                                    RelicsConfig.value("soulTomeVisMult"),
                                    0,
                                    0,
                                    150,
                                    0,
                                    0,
                                    120)) {
                        other.hurt(
                                Relics.damage(p.level(), "lightning", p),
                                20 + p.getRandom().nextFloat() * 80);
                        Relics.motion(
                                other,
                                other.position()
                                        .subtract(p.position())
                                        .normalize()
                                        .scale(3)
                                        .add(0, 1, 0));
                        RelicsNetwork.beam(
                                p.serverLevel(),
                                p.getEyePosition(),
                                other.getEyePosition(),
                                0xADFFFF);
                    }
                if (elapsed % 4 == 0 && Relics.pay(p, type) && !targets.isEmpty()) {
                    var other = targets.get(p.getRandom().nextInt(targets.size()));
                    float amount =
                            (float)
                                    Math.max(
                                            1,
                                            Math.min(
                                                    20,
                                                    other.getMaxHealth()
                                                            / Math.max(
                                                                    .001,
                                                                    RelicsConfig.value(
                                                                            "soulTomeDivisor"))));
                    if (other.hurt(Relics.damage(p.level(), "soul", p), amount))
                        RelicProjectiles.soul(p, other);
                }
            }
            case OVERTHROWER, VOID_GRIMOIRE -> {
                if (!Relics.pay(p, type)) {
                    stop(p);
                    return;
                }
                if (type == Relic.VOID_GRIMOIRE && elapsed == 0)
                    Relics.sound(p.level(), target.position(), RelicSounds.VOID.get());
                target.addEffect(
                        new MobEffectInstance(
                                MobEffects.MOVEMENT_SLOWDOWN,
                                30,
                                type == Relic.VOID_GRIMOIRE ? 100 : 2));
                if (type == Relic.VOID_GRIMOIRE) {
                    Relics.motion(target, new Vec3(0, .03, 0));
                    target.fallDistance = 0;
                }
                if (elapsed % 5 == 0) {
                    Relics.burst(p.level(), target.position());
                    RelicsNetwork.beam(
                            p.serverLevel(),
                            p.getEyePosition(),
                            target.getEyePosition(),
                            type == Relic.VOID_GRIMOIRE ? 0x3C164E : 0xFF713A);
                }
                if (remaining == 1) {
                    if (type == Relic.OVERTHROWER) banish(p, target);
                    else {
                        Relics.teleport(
                                target,
                                p.serverLevel(),
                                new Vec3(
                                        p.getRandom().nextInt(20001) - 10000,
                                        p.level().getMinBuildHeight() - 128,
                                        p.getRandom().nextInt(20001) - 10000),
                                false);
                        Relics.cooldown(p, 30);
                    }
                }
            }
            case MISSILE_TOME, LUNAR_FLARES, APOTHEOSIS, CHAOS_TOME -> {
                if (elapsed == 0 || elapsed % 2 != 0) return;
                BlockPos lock = null;
                if (type == Relic.LUNAR_FLARES) {
                    var hit = p.pick(128, 0, false);
                    if (!(hit instanceof BlockHitResult b) || hit.getType() == HitResult.Type.MISS)
                        return;
                    lock = b.getBlockPos();
                }
                boolean paid;
                if (type == Relic.CHAOS_TOME) {
                    int[] costs = new int[6];
                    for (int i = 0; i < 6; i++) costs[i] = p.getRandom().nextInt(101);
                    paid = Relics.pay(p, RelicsConfig.value(type.multiplier), costs);
                } else paid = Relics.pay(p, type);
                if (paid) RelicProjectiles.fire(p, type, lock);
                else stop(p);
            }
            default -> {}
        }
    }

    public static boolean discord(ServerPlayer p) {
        if (Relics.outer(p.level())
                || Relics.cooling(p)
                || Relics.inventory(p, Relic.TELEPORTATION_TOME).isEmpty()) return false;
        if (p.isShiftKeyDown()) {
            var dest = p.position().add(p.getLookAngle().scale(16));
            if (!Relics.valid(p, p.serverLevel(), dest) || !Relics.pay(p, Relic.TELEPORTATION_TOME))
                return false;
            if (Relics.teleport(p, p.serverLevel(), dest, true)) {
                Relics.cooldown(p, 20);
                return true;
            }
            return false;
        }
        var other = Relics.pointed(p, 128);
        if (other != null) {
            if (!Relics.valid(p, p.serverLevel(), other.position())
                    || !Relics.valid(other, p.serverLevel(), p.position())
                    || !Relics.pay(p, Relic.TELEPORTATION_TOME)) return false;
            if (Relics.swap(p, other)) {
                Relics.cooldown(p, 20);
                return true;
            }
            return false;
        }
        var hit = p.pick(128, 0, false);
        if (!(hit instanceof BlockHitResult b) || hit.getType() == HitResult.Type.MISS)
            return false;
        for (int up = 1; up <= 32; up++) {
            var pos = b.getBlockPos().above(up);
            var dest = Vec3.atBottomCenterOf(pos);
            if (p.level().getBlockState(pos.below()).blocksMotion()
                    && Relics.valid(p, p.serverLevel(), dest)) {
                if (!Relics.pay(p, Relic.TELEPORTATION_TOME)) return false;
                if (Relics.teleport(p, p.serverLevel(), dest, true)) {
                    Relics.cooldown(p, 20);
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    public static void telekinesisAttack(ServerPlayer p) {
        if (Relics.cooling(p) || !Relics.is(p.getMainHandItem(), Relic.TELEKINESIS_TOME)) return;
        var cast = CASTS.get(p.getUUID());
        var target =
                cast != null && cast.type == Relic.TELEKINESIS_TOME
                        ? p.serverLevel().getEntity(cast.target)
                        : Relics.pointed(p, 16);
        if (!(target instanceof LivingEntity living)
                || !Relics.canAffect(p, living)
                || p.distanceToSqr(living) > 256) return;
        if (!Relics.pay(p, RelicsConfig.value("telekinesisTomeVisMult"), 80, 0, 200, 0, 50, 0))
            return;
        float min = (float) RelicsConfig.value("telekinesisTomeDamageMIN"),
                max = (float) RelicsConfig.value("telekinesisTomeDamageMAX");
        living.hurt(
                Relics.damage(p.level(), "lightning", p),
                Math.min(min, max) + p.getRandom().nextFloat() * Math.abs(max - min));
        RelicsNetwork.beam(p.serverLevel(), p.getEyePosition(), living.getEyePosition(), 0xC5A6FF);
        Relics.cooldown(p, 10);
        Relics.sound(p.level(), living.position(), RelicSounds.DISCHARGE.get());
        if (p.isShiftKeyDown()
                && movable(living, Relic.TELEKINESIS_TOME)
                && Relics.pay(
                        p, RelicsConfig.value("telekinesisTomeVisMult"), 150, 0, 0, 0, 80, 0)) {
            Relics.motion(living, p.getLookAngle().scale(4));
            p.getPersistentData()
                    .putLong("forgottenrelics:grab_cooldown", p.level().getGameTime() + 40);
            stop(p);
        }
    }

    private static boolean banish(ServerPlayer caster, LivingEntity target) {
        var nether = caster.server.getLevel(Level.NETHER);
        if (nether == null || !target.canChangeDimensions()) return false;
        for (int i = 0; i < 9; i++) {
            int x = caster.getRandom().nextInt(20001) - 10000,
                    z = caster.getRandom().nextInt(20001) - 10000;
            for (int y = 124; y > nether.getMinBuildHeight(); y--) {
                var pos = new BlockPos(x, y, z);
                if (nether.getBlockState(pos.below()).blocksMotion()
                        && Relics.valid(target, nether, Vec3.atBottomCenterOf(pos)))
                    return Relics.teleport(target, nether, Vec3.atBottomCenterOf(pos), true);
            }
        }
        caster.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                        "message.forgottenrelics.banish_failed"),
                true);
        return false;
    }
}
