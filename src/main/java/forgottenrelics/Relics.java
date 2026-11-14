package forgottenrelics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

import tc4revival.aspect.*;
import tc4revival.content.*;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.*;

public final class Relics {
    private Relics() {}

    public static boolean is(ItemStack stack, Relic type) {
        return stack.is(ForgottenRelics.item(type));
    }

    public static ItemStack inventory(Player player, Relic type) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (is(stack, type)) return stack;
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack worn(Player player, Relic type) {
        return CuriosApi.getCuriosInventory(player)
                .resolve()
                .flatMap(i -> i.findFirstCurio(ForgottenRelics.item(type)))
                .map(r -> r.stack())
                .orElse(ItemStack.EMPTY);
    }

    public static boolean wearing(Player p, Relic type) {
        return !worn(p, type).isEmpty();
    }

    public static List<ServerPlayer> wearers(ServerPlayer player, Relic type) {
        return player.server.getPlayerList().getPlayers().stream()
                .filter(p -> p != player && p.isAlive() && !p.isSpectator() && wearing(p, type))
                .toList();
    }

    public static boolean canAffect(LivingEntity source, LivingEntity target) {
        return target != source
                && target.isAlive()
                && !target.isSpectator()
                && (!(target instanceof Player p) || !p.isCreative())
                && (!(source instanceof Player a)
                        || !(target instanceof Player b)
                        || a.canHarmPlayer(b));
    }

    public static List<LivingEntity> nearby(LivingEntity source, double radius) {
        return source.level()
                .getEntitiesOfClass(
                        LivingEntity.class,
                        source.getBoundingBox().inflate(radius),
                        t -> canAffect(source, t) && source.distanceToSqr(t) <= radius * radius);
    }

    public static LivingEntity pointed(Player player, double range) {
        Vec3 start = player.getEyePosition(), end = start.add(player.getLookAngle().scale(range));
        var block =
                player.level()
                        .clip(
                                new ClipContext(
                                        start,
                                        end,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        player));
        double best =
                block.getType() == HitResult.Type.MISS
                        ? range * range
                        : start.distanceToSqr(block.getLocation());
        LivingEntity result = null;
        for (var entity :
                player.level()
                        .getEntitiesOfClass(
                                LivingEntity.class,
                                player.getBoundingBox()
                                        .expandTowards(player.getLookAngle().scale(range))
                                        .inflate(1),
                                t -> canAffect(player, t))) {
            var hit = entity.getBoundingBox().inflate(.35).clip(start, end);
            if (hit.isPresent() && start.distanceToSqr(hit.get()) < best) {
                best = start.distanceToSqr(hit.get());
                result = entity;
            }
        }
        return result;
    }

    public static boolean pay(ServerPlayer player, Relic type) {
        double multiplier = type.multiplier.isEmpty() ? 1 : RelicsConfig.value(type.multiplier);
        return pay(player, multiplier, type.costs());
    }

    public static boolean pay(ServerPlayer player, double multiplier, int... base) {
        var amounts = new LinkedHashMap<Aspect, Integer>();
        String[] tags = {"aer", "terra", "ignis", "aqua", "ordo", "perditio"};
        for (int i = 0; i < 6; i++) {
            int amount = (int) Math.min(Integer.MAX_VALUE, Math.max(0, base[i] * multiplier));
            if (amount > 0) amounts.put(TCAspects.catalog().require(tags[i]), amount);
        }
        if (amounts.isEmpty()) return true;
        boolean paid = WandInventory.consume(player, amounts);
        if (paid) player.getInventory().setChanged();
        return paid;
    }

    public static void recharge(ServerPlayer player, Aspect aspect, int centivis) {
        List<ItemStack> candidates = new ArrayList<>();
        for (var stack : player.getInventory().items)
            if (stack.getItem() instanceof CastingWandItem w
                    && w.vis(stack, aspect) < w.capacity(stack)) candidates.add(stack);
        if (!candidates.isEmpty()) {
            var stack = candidates.get(player.getRandom().nextInt(candidates.size()));
            ((CastingWandItem) stack.getItem()).receiveVis(stack, aspect, centivis, false);
            player.getInventory().setChanged();
        }
    }

    public static Aspect randomPrimal(ServerPlayer p) {
        var primals = TCAspects.catalog().primals();
        return primals.get(p.getRandom().nextInt(primals.size()));
    }

    public static boolean cooling(Player p) {
        return p.getPersistentData().getLong("forgottenrelics:cooldown") > p.level().getGameTime();
    }

    public static void cooldown(Player p, int ticks) {
        p.getPersistentData().putLong("forgottenrelics:cooldown", p.level().getGameTime() + ticks);
    }

    public static boolean outer(Level level) {
        return level.dimension()
                .location()
                .equals(new net.minecraft.resources.ResourceLocation("thaumcraft", "outer_lands"));
    }

    public static boolean absolute(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || java.util.Set.of("true_damage", "oblivion", "fate", "soul").stream()
                        .anyMatch(
                                kind ->
                                        source.is(
                                                ResourceKey.create(
                                                        Registries.DAMAGE_TYPE,
                                                        ForgottenRelics.id(kind))));
    }

    public static DamageSource damage(Level level, String kind, Entity owner) {
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(
                                ResourceKey.create(
                                        Registries.DAMAGE_TYPE, ForgottenRelics.id(kind))),
                owner);
    }

    public static void burst(Level level, Vec3 pos) {
        if (level instanceof ServerLevel server)
            server.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y, pos.z, 24, .5, .5, .5, .25);
    }

    public static void sound(Level level, Vec3 pos, SoundEvent sound) {
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, .8f, 1f);
    }

    public static boolean valid(LivingEntity entity, ServerLevel level, Vec3 pos) {
        if (!Double.isFinite(pos.x)
                || !Double.isFinite(pos.y)
                || !Double.isFinite(pos.z)
                || pos.y < level.getMinBuildHeight()
                || pos.y + entity.getBbHeight() >= level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(BlockPos.containing(pos))) return false;
        level.getChunk(BlockPos.containing(pos));
        var box = entity.getBoundingBox().move(pos.subtract(entity.position()));
        return level.noCollision(entity, box) && !level.containsAnyLiquid(box);
    }

    public static boolean teleport(
            LivingEntity entity, ServerLevel target, Vec3 pos, boolean collisionCheck) {
        if (collisionCheck && !valid(entity, target, pos)) return false;
        var origin = entity.position();
        var originLevel = entity.level();
        entity.stopRiding();
        LivingEntity moved = entity;
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(target, pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
        } else if (entity.level() == target) {
            entity.teleportTo(pos.x, pos.y, pos.z);
        } else {
            var changed =
                    entity.changeDimension(
                            target,
                            new net.minecraftforge.common.util.ITeleporter() {
                                @Override
                                public Entity placeEntity(
                                        Entity original,
                                        ServerLevel current,
                                        ServerLevel destination,
                                        float yaw,
                                        java.util.function.Function<Boolean, Entity> reposition) {
                                    Entity repositioned = reposition.apply(false);
                                    if (repositioned != null)
                                        repositioned.moveTo(
                                                pos.x,
                                                pos.y,
                                                pos.z,
                                                original.getYRot(),
                                                original.getXRot());
                                    return repositioned;
                                }

                                @Override
                                public net.minecraft.world.level.portal.PortalInfo getPortalInfo(
                                        Entity original,
                                        ServerLevel destination,
                                        java.util.function.Function<
                                                        ServerLevel,
                                                        net.minecraft.world.level.portal.PortalInfo>
                                                fallback) {
                                    return new net.minecraft.world.level.portal.PortalInfo(
                                            pos, Vec3.ZERO, original.getYRot(), original.getXRot());
                                }
                            });
            if (!(changed instanceof LivingEntity living)) return false;
            moved = living;
        }
        if (moved.level() != target || moved.position().distanceToSqr(pos) > .0001) return false;
        moved.fallDistance = 0;
        burst(originLevel, origin.add(0, 1, 0));
        burst(target, pos.add(0, 1, 0));
        sound(target, pos, SoundEvents.ENDERMAN_TELEPORT);
        return true;
    }

    public static boolean randomTeleport(ServerPlayer player, int range) {
        for (int i = 0; i < 33; i++) {
            var p =
                    player.blockPosition()
                            .offset(
                                    player.getRandom().nextInt(range * 2 + 1) - range,
                                    player.getRandom().nextInt(9) - 4,
                                    player.getRandom().nextInt(range * 2 + 1) - range);
            for (int j = 0;
                    j < 12
                            && p.getY() > player.level().getMinBuildHeight()
                            && !player.level().getBlockState(p.below()).blocksMotion();
                    j++) p = p.below();
            var dest = Vec3.atBottomCenterOf(p);
            if (player.level().getBlockState(p.below()).blocksMotion()
                    && teleport(player, player.serverLevel(), dest, true)) return true;
        }
        return false;
    }

    public static boolean swap(ServerPlayer player, LivingEntity other) {
        if (!(other.level() instanceof ServerLevel theirs)) return false;
        var ours = player.serverLevel();
        var a = player.position();
        var b = other.position();
        if (!valid(player, theirs, b) || !valid(other, ours, a)) return false;
        if (!teleport(other, ours, a, true)) return false;
        if (teleport(player, theirs, b, true)) return true;
        // A dimension-travel listener can cancel a player's teleport. Restore the first
        // participant.
        teleport(other, theirs, b, false);
        return false;
    }

    public static void motion(LivingEntity entity, Vec3 velocity) {
        entity.setDeltaMovement(velocity);
        entity.hurtMarked = true;
        if (entity instanceof ServerPlayer player)
            player.connection.send(
                    new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(
                            entity));
    }
}
