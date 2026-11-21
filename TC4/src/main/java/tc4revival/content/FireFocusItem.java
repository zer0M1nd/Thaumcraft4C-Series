package tc4revival.content;

import java.util.List;
import java.util.Map;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;

public  class FireFocusItem extends WandFocusItem {
    private boolean has(ItemStack focus, FocusUpgrade upgrade) { return upgradeLevel(focus, upgrade.id) > 0; }
    @Override public int color(ItemStack focus) { return 15028484; }
    @Override public String sortingPrefix() { return "AF"; }
    @Override public Map<Aspect, Integer> costs(ItemStack focus) {
        var catalog = TCAspects.catalog();
        if (has(focus, FocusUpgrade.FIREBEAM)) return Map.of(catalog.require("ignis"), 10, catalog.require("ordo"), 3);
        if (has(focus, FocusUpgrade.FIREBALL)) return Map.of(catalog.require("ignis"), 66, catalog.require("perditio"), 33);
        return Map.of(catalog.require("ignis"), 10);
    }
    @Override public boolean costPerTick(ItemStack focus) { return true; }
    @Override public int cooldownMillis(ItemStack focus) { return has(focus, FocusUpgrade.FIREBALL) ? 1000 : 0; }
    @Override public Animation animation(ItemStack focus) { return has(focus, FocusUpgrade.FIREBALL) ? Animation.WAVE : Animation.CHARGE; }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1, 5 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY);
            case 2, 4 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.ALCHEMISTS_FIRE);
            case 3 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.FIREBALL, FocusUpgrade.FIREBEAM);
            default -> List.of();
        };
    }
    @Override public boolean canApplyUpgrade(ItemStack focus, Player player, FocusUpgrade upgrade, int rank) {
        return upgrade != FocusUpgrade.ALCHEMISTS_FIRE || !has(focus, FocusUpgrade.FIREBALL) || !has(focus, FocusUpgrade.ALCHEMISTS_FIRE);
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        var focus = attached(stack); var wand = (CastingWandItem) stack.getItem();
        if (has(focus, FocusUpgrade.FIREBALL)) {
            if (wand.consumeForPlayer(stack, player, costs(attached(stack)), false, false, true)) {
                if (!level.isClientSide) {
                    level.addFreshEntity(FocusProjectiles.fromHand(new ExplosiveOrbEntity(level, player, 1 + wand.focusPotency(stack, player) * .4f, has(focus, FocusUpgrade.ALCHEMISTS_FIRE)), player, hand));
                    level.levelEvent(1018, player.blockPosition(), 0);
                }
                // Casting has its own effect; do not trigger the vanilla arm swing.
            }
        } else { player.startUsingItem(hand); WandCooldowns.clear(player); }
        return InteractionResultHolder.consume(stack);
    }
    @Override public void usingTick(ItemStack stack, Player player, int remaining) {
        var wand = (CastingWandItem) stack.getItem();
        if (!wand.consumeForPlayer(stack, player, costs(attached(stack)), false, false, false)) { player.stopUsingItem(); return; }
        var level = player.level(); if (level.isClientSide) return;
        if ((getUseDuration(stack)-remaining)%10==0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), TCSounds.FIRE_LOOP.get(), SoundSource.PLAYERS, .33f, 2);
        }
        if (!wand.consumeForPlayer(stack, player, costs(attached(stack)), false, false, true)) return;
        var focus = attached(stack); boolean beam = has(focus, FocusUpgrade.FIREBEAM);
        float potency = wand.focusPotency(stack, player);
        int count = FocusPotency.discrete(2 + potency, level.random);
        for (int i = 0; i < count; i++) {
            var ember = new EmberEntity(level, player, beam ? .25f : 15,
                    beam ? (2.5f + potency) * 1.5f : 2 + potency,
                    upgradeLevel(focus, FocusUpgrade.ALCHEMISTS_FIRE.id), beam ? 30 : 20);
            FocusProjectiles.fromHand(ember, player, player.getUsedItemHand());
            ember.setPos(ember.position().add(ember.getDeltaMovement())); level.addFreshEntity(ember);
        }
    }
}
