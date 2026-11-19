package tc4revival.content;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;
import tc4revival.network.FocusEffectPacket;

public  class ShockFocusItem extends WandFocusItem {
    @Override public int color(ItemStack focus) { return 10466239; }
    @Override public String sortingPrefix() { return "BL"; }
    private boolean upgraded(ItemStack focus, FocusUpgrade upgrade) { return upgradeLevel(focus, upgrade.id) > 0; }
    @Override public Map<Aspect, Integer> costs(ItemStack focus) {
        var aspects = TCAspects.catalog();
        if (upgraded(focus, FocusUpgrade.CHAIN_LIGHTNING)) return Map.of(aspects.require("aer"), 40, aspects.require("aqua"), 10);
        if (upgraded(focus, FocusUpgrade.EARTH_SHOCK)) return Map.of(aspects.require("aer"), 75, aspects.require("terra"), 25);
        return Map.of(aspects.require("aer"), 25);
    }
    @Override public int cooldownMillis(ItemStack focus) {
        return upgraded(focus, FocusUpgrade.CHAIN_LIGHTNING) ? 500 : upgraded(focus, FocusUpgrade.EARTH_SHOCK) ? 1000 : 250;
    }
    @Override public Animation animation(ItemStack focus) { return upgraded(focus, FocusUpgrade.EARTH_SHOCK) ? Animation.WAVE : Animation.CHARGE; }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1, 2 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY);
            case 3 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.CHAIN_LIGHTNING, FocusUpgrade.EARTH_SHOCK);
            case 4, 5 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.ENLARGE);
            default -> List.of();
        };
    }
    @Override public boolean canApplyUpgrade(ItemStack focus, Player player, FocusUpgrade upgrade, int rank) {
        return upgrade != FocusUpgrade.ENLARGE || upgraded(focus, FocusUpgrade.CHAIN_LIGHTNING) || upgraded(focus, FocusUpgrade.EARTH_SHOCK);
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        var wand = (CastingWandItem) stack.getItem();
        var focus = attached(stack);
        if (upgraded(focus, FocusUpgrade.EARTH_SHOCK)) {
            if (wand.consumeForPlayer(stack, player, costs(attached(stack)), false, false, true)) {
                if (!level.isClientSide) {
                    var orb = new ShockOrbEntity(level, player, 4 + upgradeLevel(focus, FocusUpgrade.ENLARGE.id) * 2,
                            (float) (5 + wand.focusPotency(stack, player) * 1.33));
                    level.addFreshEntity(FocusProjectiles.fromHand(orb, player, hand));
                    level.playSound(null, orb.getX(), orb.getY(), orb.getZ(), TCSounds.ZAP.get(), SoundSource.PLAYERS, 1, 1 + (level.random.nextFloat() - level.random.nextFloat()) * .2f);
                }
                player.swing(hand);
            }
        } else {
            player.startUsingItem(hand);
            WandCooldowns.clear(player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    @Override public void usingTick(ItemStack stack, Player player, int remaining) {
        var wand = (CastingWandItem) stack.getItem();
        if (!wand.consumeForPlayer(stack, player, costs(attached(stack)), false, false, true)) { player.stopUsingItem(); return; }
        var level = player.level();
        var target = FocusTargeting.pointed(player, 0, 20, 1.1);
        if (level.isClientSide) {
            tc4revival.client.ClientFocusEffects.shock(player, target);
            return;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), TCSounds.SHOCK.get(), SoundSource.PLAYERS, .25f, 1);
        if (!(target instanceof LivingEntity first)) {
            if(target==null && player instanceof net.minecraft.server.level.ServerPlayer server) {
                var hit=level.clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(),player.getEyePosition().add(player.getLookAngle().scale(20)),
                        net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
                if(hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK)CopperProcessing.shock(server,stack,hit);
            }
            return;
        }
        if (!canDamage(player, first)) return;
        var actor=thaumcraft.api.wands.WandActors.resolve(player);
        if(actor!=player)FocusEffectPacket.lightning(level,actor,first);
        var focus = attached(stack);
        float potency = wand.focusPotency(stack, player);
        int hops = upgradeLevel(focus, FocusUpgrade.CHAIN_LIGHTNING.id) * 2;
        first.hurt(level.damageSources().playerAttack(player), (hops > 0 ? 6 : 4) + potency);
        if (hops == 0) return;
        hops += upgradeLevel(focus, FocusUpgrade.ENLARGE.id) * 2;
        var visited = new HashSet<Integer>(); visited.add(first.getId());
        LivingEntity center = first;
        while (hops-- > 0) {
            LivingEntity next = null; double distance = Double.MAX_VALUE;
            var position = center.position();
            // TC4 uses an eight-block cube here, with no line-of-sight requirement for the chain.
            for (var candidate : level.getEntitiesOfClass(LivingEntity.class, new AABB(position, position).inflate(8))) {
                if (candidate == player || visited.contains(candidate.getId()) || !canDamage(player, candidate)) continue;
                double current = candidate.distanceToSqr(center);
                if (current < distance) { next = candidate; distance = current; }
            }
            if (next == null) break;
            FocusEffectPacket.lightning(level, center, next);
            visited.add(next.getId());
            next.hurt(level.damageSources().playerAttack(player), 4 + potency);
            center = next;
        }
    }
    private static boolean canDamage(Player player, LivingEntity target) {
        return thaumcraft.api.wands.WandActors.canTarget(player,target)
                && (!(target instanceof Player) || player.getServer() != null && player.getServer().isPvpAllowed());
    }
}
