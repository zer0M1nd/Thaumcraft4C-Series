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

public  class FrostFocusItem extends WandFocusItem {
    @Override public int color(ItemStack focus) { return 5204428; }
    @Override public String sortingPrefix() { return "BF"; }
    private boolean has(ItemStack focus, FocusUpgrade upgrade) { return upgradeLevel(focus, upgrade.id) > 0; }
    @Override public Map<Aspect, Integer> costs(ItemStack focus) {
        var catalog = TCAspects.catalog();
        if (has(focus, FocusUpgrade.SCATTERSHOT)) return Map.of(catalog.require("aqua"), 20, catalog.require("ignis"), 2, catalog.require("perditio"), 2, catalog.require("aer"), 5);
        if (has(focus, FocusUpgrade.ICE_BOULDER)) return Map.of(catalog.require("aqua"), 20, catalog.require("ignis"), 2, catalog.require("perditio"), 2, catalog.require("terra"), 5);
        return Map.of(catalog.require("aqua"), 5, catalog.require("ignis"), 2, catalog.require("perditio"), 2);
    }
    @Override public int cooldownMillis(ItemStack focus) {
        return has(focus, FocusUpgrade.SCATTERSHOT) || has(focus, FocusUpgrade.ICE_BOULDER) ? 500 : 200;
    }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1, 5 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.ALCHEMISTS_FROST);
            case 2, 4 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY);
            case 3 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.SCATTERSHOT, FocusUpgrade.ICE_BOULDER, FocusUpgrade.ALCHEMISTS_FROST);
            default -> List.of();
        };
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        var wand = (CastingWandItem) stack.getItem();
        var focus = attached(stack);
        // TC4 charges from the top-level wand upgrade tag, as it does for shock and fire.
        if (!level.isClientSide && wand.consumeForPlayer(stack, player, costs(stack), false, false, true)) {
            float potency = wand.focusPotency(stack, player);
            boolean scatter = has(focus, FocusUpgrade.SCATTERSHOT);
            boolean boulder = !scatter && has(focus, FocusUpgrade.ICE_BOULDER);
            int count = scatter ? FocusPotency.discrete(5 + potency * 2, level.random) : 1;
            FrostShardEntity last = null;
            for (int i = 0; i < count; i++) {
                last = new FrostShardEntity(level, player, scatter ? 8 : 1,
                        scatter ? 1 : boulder ? 4 + potency * 2 : 3 + potency * 1.5f,
                        upgradeLevel(focus, FocusUpgrade.ALCHEMISTS_FROST.id), scatter, boulder);
                level.addFreshEntity(FocusProjectiles.fromHand(last, player, hand));
            }
            if (last != null) level.playSound(null, last.getX(), last.getY(), last.getZ(), TCSounds.ICE.get(), SoundSource.PLAYERS, .4f, 1 + level.random.nextFloat() * .1f);
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
