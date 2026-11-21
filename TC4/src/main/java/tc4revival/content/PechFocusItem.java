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

public  class PechFocusItem extends WandFocusItem {
    @Override public int color(ItemStack focus) { return 0x229944; }
    @Override public String sortingPrefix() { return "PP"; }
    @Override public String depthTexture() { return "focus_pech_depth"; }
    @Override public int cooldownMillis(ItemStack focus) { return 250; }
    @Override public Map<Aspect, Integer> costs(ItemStack focus) {
        var catalog = TCAspects.catalog();
        if (upgradeLevel(focus, FocusUpgrade.NIGHTSHADE.id) > 0) return Map.of(catalog.require("aer"), 10, catalog.require("ignis"), 10,
                catalog.require("terra"), 10, catalog.require("aqua"), 10, catalog.require("ordo"), 10, catalog.require("perditio"), 10);
        return Map.of(catalog.require("terra"), 10, catalog.require("aqua"), 10, catalog.require("perditio"), 10);
    }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1, 3 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY);
            case 2, 4 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.EXTEND);
            case 5 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.NIGHTSHADE);
            default -> List.of();
        };
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        var wand = (CastingWandItem) stack.getItem();
        if (!level.isClientSide && wand.consumeForPlayer(stack, player, costs(stack), false, false, true)) {
            var blast = new PechBlastEntity(level, player, wand.focusPotency(stack, player), wand.focusUpgrade(stack, FocusUpgrade.EXTEND),
                    upgradeLevel(attached(stack), FocusUpgrade.NIGHTSHADE.id) > 0);
            level.addFreshEntity(FocusProjectiles.fromHand(blast, player, hand));
            level.playSound(null, blast.getX(), blast.getY(), blast.getZ(), TCSounds.ICE.get(), SoundSource.PLAYERS, .4f, 1 + level.random.nextFloat() * .1f);
        }
        player.swing(hand); return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
