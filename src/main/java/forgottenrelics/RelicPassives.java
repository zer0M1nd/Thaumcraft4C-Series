package forgottenrelics;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import tc4revival.aspect.TCAspects;

import top.theillusivec4.curios.api.*;

import java.util.*;

public final class RelicPassives {
    private RelicPassives() {}

    public static void inventory(Relic relic, ItemStack stack, ServerPlayer p) {
        var n = stack.getOrCreateTag();
        var random = p.getRandom();
        switch (relic) {
            case XP_TOME -> {
                if (!n.getBoolean("active")) return;
                long stored = Math.max(0, n.getLong("experience"));
                long current =
                        ExperienceMath.atLevel(p.experienceLevel)
                                + Math.round(p.experienceProgress * p.getXpNeededForNextLevel());
                int amount =
                        (int)
                                Math.min(
                                        5,
                                        n.getBoolean("extract")
                                                ? Math.min(stored, Integer.MAX_VALUE - current)
                                                : Math.min(current, Long.MAX_VALUE - stored));
                if (amount <= 0) return;
                long next = current + (n.getBoolean("extract") ? amount : -amount);
                int level = ExperienceMath.level(next);
                p.experienceLevel = level;
                p.experienceProgress =
                        (float) (next - ExperienceMath.atLevel(level))
                                / ExperienceMath.toNext(level);
                p.totalExperience = (int) Math.min(Integer.MAX_VALUE, next);
                p.giveExperiencePoints(
                        0); // Forces vanilla XP packet bookkeeping to observe the new total.
                n.putLong("experience", stored + (n.getBoolean("extract") ? -amount : amount));
                p.getInventory().setChanged();
            }
            case OBLIVION_STONE -> OblivionRecipe.tick(stack, p);
            case OMEGA_CORE -> {
                for (var aspect : TCAspects.catalog().primals()) Relics.recharge(p, aspect, 100);
            }
            case CHAOS_CORE -> {
                if (random.nextDouble() <= .000208) {
                    var effects =
                            net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS
                                    .getValues()
                                    .stream()
                                    .filter(e -> !e.isInstantenous())
                                    .toList();
                    if (!effects.isEmpty())
                        p.addEffect(
                                new MobEffectInstance(
                                        effects.get(random.nextInt(effects.size())),
                                        100 + random.nextInt(2400),
                                        random.nextInt(3)));
                }
            }
            case FATE_TOME -> {
                int cooldown = n.getInt("fate_cooldown");
                if (cooldown > 0) {
                    n.putInt("fate_cooldown", cooldown - 1);
                    if (cooldown == 1)
                        p.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable(
                                        "message.forgottenrelics.fate_ready"),
                                true);
                }
                if (random.nextDouble() <= .000016
                        && p.getInventory().items.stream()
                                        .filter(s -> Relics.is(s, Relic.FATE_TOME))
                                        .count()
                                > 1) fateDisaster(p);
            }
            default -> {}
        }
    }

    private static void fateDisaster(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++)
            if (Relics.is(p.getInventory().getItem(i), Relic.FATE_TOME))
                p.getInventory().setItem(i, ItemStack.EMPTY);
        var targets = new ArrayList<>(Relics.nearby(p, 64));
        targets.add(p);
        for (var entity : targets) {
            entity.hurt(Relics.damage(p.level(), "fate", p), 40000);
            p.level()
                    .explode(
                            p,
                            entity.getX(),
                            entity.getY(),
                            entity.getZ(),
                            16,
                            true,
                            Level.ExplosionInteraction.TNT);
        }
        p.level()
                .explode(
                        p, p.getX(), p.getY(), p.getZ(), 100, true, Level.ExplosionInteraction.TNT);
    }

    public static void worn(Relic relic, ItemStack stack, ServerPlayer p, SlotContext context) {
        var n = stack.getOrCreateTag();
        var random = p.getRandom();
        switch (relic) {
            case DEIFIC_AMULET -> {
                if (RelicsConfig.enabled("deificAmuletEffectImmunity"))
                    for (var effect : new ArrayList<>(p.getActiveEffects()))
                        if (!RelicsConfig.enabled("deificAmuletOnlyNegatesDebuffs")
                                || !effect.getEffect().isBeneficial())
                            p.removeEffect(effect.getEffect());
                p.clearFire();
                if (p.getAirSupply() <= 0
                        && Relics.pay(
                                p,
                                RelicsConfig.value("deificAmuletVisMult"),
                                1000,
                                0,
                                0,
                                1000,
                                0,
                                0)) p.setAirSupply(300);
                int cooldown = n.getInt("immunity_cooldown");
                if (RelicsConfig.enabled("deificAmuletInvincibility")
                        && cooldown == 0
                        && p.invulnerableTime > 10) {
                    p.invulnerableTime = 40;
                    cooldown = 32;
                }
                if (cooldown > 0) n.putInt("immunity_cooldown", cooldown - 1);
            }
            case ARCANUM -> {
                if (random.nextDouble() <= .025 * RelicsConfig.value("arcanumGenRate"))
                    Relics.recharge(p, Relics.randomPrimal(p), 100);
                else if (random.nextDouble() <= .000208) Relics.randomTeleport(p, 32);
                else if (random.nextDouble() <= .000027) {
                    var dormant = new ItemStack(ForgottenRelics.item(Relic.DORMANT_ARCANUM));
                    dormant.getOrCreateTag()
                            .putInt(
                                    "lifetime",
                                    (int)
                                            Math.min(
                                                    Integer.MAX_VALUE,
                                                    (1200 + random.nextInt(6000))
                                                            * RelicsConfig.value(
                                                                    "dormantArcanumVisMult")));
                    replace(context, dormant);
                }
            }
            case DORMANT_ARCANUM -> {
                if (!n.contains("lifetime")) n.putInt("lifetime", 1200);
                int life = n.getInt("lifetime");
                if (life <= 0) replace(context, new ItemStack(ForgottenRelics.item(Relic.ARCANUM)));
                else if (Relics.pay(p, 1, 3, 3, 3, 3, 3, 3)) n.putInt("lifetime", life - 1);
            }
            case SHINY_STONE -> {
                int check = Math.max(1, RelicsConfig.integer("shinyStoneCheckrate"));
                if (p.tickCount % check == 0) {
                    boolean still =
                            n.contains("last_x")
                                    && Math.abs(n.getDouble("last_x") - p.getX()) < 1e-6
                                    && Math.abs(n.getDouble("last_y") - p.getY()) < 1e-6
                                    && Math.abs(n.getDouble("last_z") - p.getZ()) < 1e-6;
                    n.putInt("still", still ? Math.min(400, n.getInt("still") + check) : 0);
                    n.putDouble("last_x", p.getX());
                    n.putDouble("last_y", p.getY());
                    n.putDouble("last_z", p.getZ());
                    if (still)
                        p.serverLevel()
                                .sendParticles(
                                        ParticleTypes.END_ROD,
                                        p.getX(),
                                        p.getY() + 1,
                                        p.getZ(),
                                        1,
                                        .7,
                                        .7,
                                        .7,
                                        .02);
                }
                int still = n.getInt("still");
                int interval = still >= 200 ? 1 : still >= 80 ? 2 : still >= 40 ? 5 : 10;
                interval = Math.max(1, (int) Math.round(interval * check / 4.0));
                if (still > 0 && p.tickCount % interval == 0) p.heal(1);
            }
            case ANCIENT_AEGIS -> {
                if (p.tickCount % 20 == 0) p.heal(1);
            }
            case OBLIVION_AMULET -> {
                float stored = n.getFloat("damage");
                if (random.nextDouble() <= .0008 && stored > 0) {
                    float amount = stored * random.nextFloat();
                    if (amount > 100 && random.nextDouble() <= .9)
                        amount = random.nextFloat() * 100;
                    n.putFloat("damage", stored - amount);
                    p.hurt(Relics.damage(p.level(), "oblivion", null), amount);
                } else if (random.nextDouble() <= .0004) {
                    var effects =
                            List.of(
                                    MobEffects.DIG_SLOWDOWN,
                                    MobEffects.BLINDNESS,
                                    MobEffects.WEAKNESS,
                                    MobEffects.WITHER);
                    p.addEffect(
                            new MobEffectInstance(
                                    effects.get(random.nextInt(4)),
                                    100 + random.nextInt(2000),
                                    random.nextInt(3)));
                }
            }
            case SUPERPOSITION_RING -> {
                if (p.tickCount % 600 == 0 && random.nextDouble() <= .025) {
                    var others = Relics.wearers(p, relic);
                    if (!others.isEmpty())
                        Relics.swap(p, others.get(random.nextInt(others.size())));
                }
            }
            default -> {}
        }
    }

    private static void replace(SlotContext context, ItemStack replacement) {
        CuriosApi.getCuriosInventory(context.entity())
                .ifPresent(
                        inv ->
                                inv.getStacksHandler(context.identifier())
                                        .ifPresent(
                                                handler -> {
                                                    if (context.index() < handler.getSlots())
                                                        handler.getStacks()
                                                                .setStackInSlot(
                                                                        context.index(),
                                                                        replacement);
                                                }));
    }
}
