package tc4revival.content;

import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;

public  class HellbatFocusItem extends WandFocusItem {
    @Override public int color(ItemStack focus) { return 14431746; }
    @Override public String sortingPrefix() { return "HH"; }
    @Override public String ornamentTexture() { return "focus_hellbat_orn"; }
    @Override public int cooldownMillis(ItemStack focus) { return 1000; }
    @Override public Map<Aspect, Integer> costs(ItemStack focus) {
        var a = TCAspects.catalog();
        if (upgradeLevel(focus, FocusUpgrade.BAT_BOMBS.id) > 0)
            return Map.of(a.require("ignis"), 100, a.require("perditio"), 200, a.require("aer"), 100);
        if (upgradeLevel(focus, FocusUpgrade.DEVIL_BATS.id) > 0)
            return Map.of(a.require("ignis"), 100, a.require("perditio"), 100, a.require("aer"), 100, a.require("terra"), 100);
        return Map.of(a.require("ignis"), 200, a.require("perditio"), 100, a.require("aer"), 100);
    }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1, 2, 4 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY);
            case 3 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.BAT_BOMBS, FocusUpgrade.DEVIL_BATS);
            case 5 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.VAMPIRE_BATS);
            default -> List.of();
        };
    }
    @Override public boolean canApplyUpgrade(ItemStack focus, Player player, FocusUpgrade upgrade, int rank) {
        if (upgrade != FocusUpgrade.VAMPIRE_BATS) return true;
        if (player instanceof ServerPlayer server) return tc4revival.research.KnowledgeSavedData.get(server.getServer()).snapshot(player.getUUID())
                .map(knowledge -> knowledge.researchMarkers().contains("VAMPBAT")).orElse(false);
        return player.level().isClientSide && tc4revival.client.ClientKnowledge.snapshot()
                .map(knowledge -> knowledge.researchMarkers().contains("VAMPBAT")).orElse(false);
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(FocusTargeting.pointed(player, 0, 32, 1.1, entity -> !(entity instanceof FireBatEntity)) instanceof LivingEntity target))
            return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            if (target instanceof Player && !level.getServer().isPvpAllowed()) return InteractionResultHolder.pass(stack);
            var wand = (CastingWandItem) stack.getItem();
            if (wand.consumeForPlayer(stack, player, costs(stack), false, false, true)) {
                var bat = new FireBatEntity(TCEntities.FIRE_BAT.get(), level);
                bat.summon(player, target, wand.focusPotency(stack, player),
                        wand.focusUpgrade(stack, FocusUpgrade.BAT_BOMBS) > 0, wand.focusUpgrade(stack, FocusUpgrade.DEVIL_BATS) > 0,
                        wand.focusUpgrade(stack, FocusUpgrade.VAMPIRE_BATS) > 0);
                double yaw = Math.toRadians(player.getYRot());
                var at = player.position().add(-Math.cos(yaw) * .16, player.getBbHeight() / 2 + .2 + bat.getBbHeight(), -Math.sin(yaw) * .16)
                        .add(player.getLookAngle().scale(.5));
                bat.moveTo(at.x, at.y, at.z, player.getYRot(), 0);
                if (level.addFreshEntity(bat)) {
                    level.levelEvent(2004, bat.blockPosition(), 0);
                    level.playSound(null, bat.blockPosition(), TCSounds.ICE.get(), SoundSource.PLAYERS, .2f, .95f + level.random.nextFloat() * .1f);
                } else level.playSound(null, player.blockPosition(), TCSounds.WAND_FAIL.get(), SoundSource.PLAYERS, .1f, .8f + level.random.nextFloat() * .1f);
            } else level.playSound(null, player.blockPosition(), TCSounds.WAND_FAIL.get(), SoundSource.PLAYERS, .1f, .8f + level.random.nextFloat() * .1f);
        }
        player.swing(hand); return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
