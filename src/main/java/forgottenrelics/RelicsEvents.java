package forgottenrelics;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = ForgottenRelics.ID)
public final class RelicsEvents {
    private static final ThreadLocal<Boolean> TRANSFER = ThreadLocal.withInitial(() -> false);
    private static final UUID REACH = UUID.fromString("54de0001-668f-40fb-b7fa-5bb96898ac45"),
            KNOCKBACK = UUID.fromString("54de0002-668f-40fb-b7fa-5bb96898ac45");

    private RelicsEvents() {}

    public static boolean redirected() {
        return TRANSFER.get();
    }

    public static boolean transfer(LivingEntity target, DamageSource source, float damage) {
        boolean previous = TRANSFER.get();
        TRANSFER.set(true);
        try {
            return target.hurt(source, damage);
        } finally {
            TRANSFER.set(previous);
        }
    }

    private static boolean carry(Player p, Relic relic) {
        return !Relics.inventory(p, relic).isEmpty();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide || TRANSFER.get() || event.isCanceled()) return;
        var target = event.getEntity();
        var source = event.getSource();
        var owner = source.getEntity();
        if (owner instanceof ServerPlayer p
                && carry(p, Relic.CHAOS_CORE)
                && p.getRandom().nextDouble() < .45) {
            var candidates = Relics.nearby(target, 16);
            if (!candidates.isEmpty()) {
                var other =
                        p.getRandom().nextDouble() < .15
                                ? p
                                : candidates.get(p.getRandom().nextInt(candidates.size()));
                if (other == p || Relics.canAffect(p, other)) {
                    transfer(other, source, event.getAmount() * p.getRandom().nextFloat() * 2);
                    event.setCanceled(true);
                    return;
                }
            }
        }
        if (!(target instanceof ServerPlayer p)) return;
        var random = p.getRandom();
        if (carry(p, Relic.CHAOS_CORE) && random.nextDouble() < .42) {
            var candidates = Relics.nearby(p, 16);
            if (!candidates.isEmpty()) {
                transfer(
                        candidates.get(random.nextInt(candidates.size())),
                        source,
                        event.getAmount() * random.nextFloat() * 2);
                event.setCanceled(true);
                return;
            }
        }
        if (Relics.wearing(p, Relic.ARCANUM)
                && !Relics.absolute(source)
                && random.nextDouble() < RelicsConfig.value("nebulousCoreDodgeChance")
                && Relics.randomTeleport(p, 16)) {
            p.invulnerableTime = 20;
            event.setCanceled(true);
            return;
        }
        if (Relics.wearing(p, Relic.DARK_SUN_RING)) {
            if (source.is(DamageTypeTags.IS_FIRE)) {
                if (!RelicsConfig.enabled("darkSunRingHealLimit") || p.invulnerableTime == 0) {
                    p.heal(event.getAmount());
                    if (RelicsConfig.enabled("darkSunRingHealLimit")) p.invulnerableTime = 20;
                }
                event.setCanceled(true);
            } else if (owner instanceof LivingEntity living
                    && Relics.canAffect(p, living)
                    && random.nextDouble() <= RelicsConfig.value("darkSunRingDeflectChance")
                    && p.invulnerableTime == 0) {
                p.invulnerableTime = 20;
                transfer(living, source, event.getAmount());
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || event.isCanceled()) return;
        var target = event.getEntity();
        var source = event.getSource();
        float amount = event.getAmount();
        boolean absolute = Relics.absolute(source);
        if (!TRANSFER.get()
                && !absolute
                && RelicsConfig.enabled("falseJusticeEnabled")
                && ((target instanceof Player p && carry(p, Relic.FALSE_JUSTICE))
                        || (source.getEntity() instanceof Player a
                                && carry(a, Relic.FALSE_JUSTICE)))) {
            event.setCanceled(true);
            target.invulnerableTime = 0;
            transfer(
                    target,
                    Relics.damage(target.level(), "true_damage", source.getEntity()),
                    amount * 2);
            return;
        }
        if (!(target instanceof ServerPlayer p)) return;
        if (!TRANSFER.get() && carry(p, Relic.CHAOS_CORE)) amount *= p.getRandom().nextFloat() * 2;
        if (!absolute && Relics.wearing(p, Relic.DARK_SUN_RING)) {
            if (amount > RelicsConfig.value("darkSunRingDamageCap")) {
                event.setCanceled(true);
                return;
            }
            if (p.getRandom().nextDouble() <= .25) amount *= 1 + p.getRandom().nextFloat();
        }
        if (!TRANSFER.get() && !Relics.wearing(p, Relic.ANCIENT_AEGIS)) {
            var aegis =
                    p.serverLevel().players().stream()
                            .filter(
                                    q ->
                                            q != p
                                                    && q.isAlive()
                                                    && q.distanceToSqr(p) <= 1024
                                                    && Relics.wearing(q, Relic.ANCIENT_AEGIS))
                            .findFirst();
            if (aegis.isPresent()) {
                transfer(aegis.get(), source, amount * .4f);
                amount *= .6f;
            }
        }
        if (!absolute && Relics.wearing(p, Relic.ANCIENT_AEGIS))
            amount *= 1 - (float) Math.min(1, RelicsConfig.value("ancientAegisDamageReduction"));
        if (!TRANSFER.get() && Relics.wearing(p, Relic.SUPERPOSITION_RING)) {
            var others = Relics.wearers(p, Relic.SUPERPOSITION_RING);
            if (!others.isEmpty()) {
                float split = amount * (.12f + p.getRandom().nextFloat() * .62f);
                for (var other : others) transfer(other, source, split / others.size());
                amount -= split;
            }
        }
        var amulet = Relics.worn(p, Relic.OBLIVION_AMULET);
        if (!absolute && !amulet.isEmpty() && Float.isFinite(amount) && amount > 0) {
            int cost =
                    (int)
                            Math.min(
                                    Integer.MAX_VALUE,
                                    amount * 8 * RelicsConfig.value("oblivionAmuletVisMult"));
            if (Relics.pay(p, 1, 0, 0, cost, 0, 0, cost)) {
                amulet.getOrCreateTag()
                        .putFloat(
                                "damage",
                                Math.min(
                                        Float.MAX_VALUE / 2,
                                        amulet.getOrCreateTag().getFloat("damage") + amount));
                event.setCanceled(true);
                return;
            }
        }
        event.setAmount(Math.max(0, amount));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void death(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        RelicUse.clear(p);
        if (carry(p, Relic.OMEGA_CORE)) {
            event.setCanceled(true);
            p.setHealth(1);
            return;
        }
        var tome = Relics.inventory(p, Relic.FATE_TOME);
        if (tome.isEmpty()
                || tome.getOrCreateTag().getInt("fate_cooldown") > 0
                || !Relics.pay(p, Relic.FATE_TOME)) return;
        int min = Math.min(100000, RelicsConfig.integer("fateTomeCooldownMIN")),
                max = Math.min(100000, RelicsConfig.integer("fateTomeCooldownMAX"));
        tome.getOrCreateTag()
                .putInt(
                        "fate_cooldown",
                        max == 0
                                ? 0
                                : (Math.min(min, max)
                                                + p.getRandom()
                                                        .nextInt(
                                                                Math.max(
                                                                        1,
                                                                        Math.abs(max - min) + 1)))
                                        * 20);
        event.setCanceled(true);
        p.setHealth(p.getMaxHealth());
        if (p.getRandom().nextDouble() <= .75) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 2));
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 500, 1));
            p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1000));
        } else {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 2));
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200));
            p.addEffect(new MobEffectInstance(MobEffects.WITHER, 300, 1));
        }
        Relics.burst(p.level(), p.position());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void mining(PlayerEvent.BreakSpeed event) {
        var p = event.getEntity();
        float multiplier = 1;
        if (Relics.wearing(p, Relic.MINING_CHARM))
            multiplier += RelicsConfig.value("miningCharmBoost");
        if (Relics.wearing(p, Relic.ADVANCED_MINING_CHARM))
            multiplier += RelicsConfig.value("advancedMiningCharmBoost");
        event.setNewSpeed(event.getNewSpeed() * multiplier);
    }

    private static void attribute(Player p, Attribute attribute, UUID id, double amount) {
        var value = p.getAttribute(attribute);
        if (value == null) return;
        var old = value.getModifier(id);
        if (old != null && old.getAmount() == amount) return;
        if (old != null) value.removeModifier(id);
        if (amount != 0)
            value.addTransientModifier(
                    new AttributeModifier(
                            id, "Forgotten Relics", amount, AttributeModifier.Operation.ADDITION));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        double reach =
                (Relics.wearing(p, Relic.MINING_CHARM) ? RelicsConfig.value("miningCharmReach") : 0)
                        + (Relics.wearing(p, Relic.ADVANCED_MINING_CHARM)
                                ? RelicsConfig.value("advancedMiningCharmReach")
                                : 0);
        attribute(p, ForgeMod.BLOCK_REACH.get(), REACH, reach);
        attribute(
                p,
                Attributes.KNOCKBACK_RESISTANCE,
                KNOCKBACK,
                Relics.wearing(p, Relic.ANCIENT_AEGIS) ? 1 : 0);
        RelicUse.validate(p);
        ForgottenKnowledge.tick(p);
        if (RelicsConfig.enabled("outerLandsAntiAbuseEnabled")
                && Relics.outer(p.level())
                && p.tickCount % Math.max(1, RelicsConfig.integer("outerLandsCheckrate")) == 0) {
            var eye = p.getEyePosition();
            var up =
                    p.level()
                            .clip(
                                    new net.minecraft.world.level.ClipContext(
                                            eye,
                                            eye.add(0, 24, 0),
                                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                            net.minecraft.world.level.ClipContext.Fluid.NONE,
                                            p));
            var down =
                    p.level()
                            .clip(
                                    new net.minecraft.world.level.ClipContext(
                                            eye,
                                            eye.add(0, -24, 0),
                                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                            net.minecraft.world.level.ClipContext.Fluid.NONE,
                                            p));
            if (up.getType() == net.minecraft.world.phys.HitResult.Type.MISS
                    || down.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                if (!tc4revival.content.EldritchPortalTravel.transfer(p, p.server.overworld())) {
                    var pos = p.server.overworld().getSharedSpawnPos();
                    Relics.teleport(
                            p,
                            p.server.overworld(),
                            net.minecraft.world.phys.Vec3.atBottomCenterOf(pos),
                            true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void living(LivingEvent.LivingTickEvent event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel level)
                || entity.tickCount <= 100
                || entity.tickCount % 20 != 0
                || !net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                        .getKey(entity.getType())
                        .toString()
                        .equals("botania:doppleganger")) return;
        double radius = Math.min(64, RelicsConfig.value("guardianAntiAbuseRadius"));
        if (radius <= 0 || !level.containsAnyLiquid(entity.getBoundingBox().inflate(radius)))
            return;
        Relics.burst(level, entity.position());
        double notify = RelicsConfig.value("guardianNotificationRadius");
        for (var player :
                notify < 0 ? level.getServer().getPlayerList().getPlayers() : level.players())
            if (notify < 0 || notify > 0 && player.distanceToSqr(entity) <= notify * notify)
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.forgottenrelics.guardian_vanish"),
                        false);
        entity.discard();
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        RelicUse.clear(event.getEntity());
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        RelicUse.clear(event.getEntity());
    }
}
