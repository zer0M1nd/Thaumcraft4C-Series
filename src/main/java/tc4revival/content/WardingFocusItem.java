package tc4revival.content;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;
import tc4revival.network.FocusEffectPacket;

public  class WardingFocusItem extends ArchitectFocusItem {
    private final Map<ServerLevel, Map<BlockPos, Long>> delays = new WeakHashMap<>();
    @Override public String sortingPrefix() { return "BWA"; }
    @Override public int color(ItemStack focus) { return 16771535; }
    @Override public String depthTexture() { return "focus_warding_depth"; }
    @Override public String ornamentTexture() { return "focus_warding_orn"; }
    @Override public boolean volume() { return true; }
    @Override public int maximumArea(ItemStack focus) { return thaumcraft.api.wands.SpellParameters.bounded("architect.radius",3 + upgradeLevel(focus, FocusUpgrade.ENLARGE.id),12); }
    @Override public Map<Aspect, Integer> costs(ItemStack focus) {
        return Map.of(TCAspects.catalog().require("terra"), 25, TCAspects.catalog().require("ordo"), 25, TCAspects.catalog().require("aqua"), 10);
    }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1 -> List.of(FocusUpgrade.FRUGAL);
            case 2 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.ARCHITECT);
            case 3, 4, 5 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.ENLARGE);
            default -> List.of();
        };
    }
    @Override public boolean canApplyUpgrade(ItemStack focus, Player player, FocusUpgrade upgrade, int rank) {
        return upgrade != FocusUpgrade.ENLARGE || upgradeLevel(focus, FocusUpgrade.ARCHITECT.id) > 0;
    }
    @Override protected boolean eligible(Level level, BlockPos origin, BlockPos pos, Player player) {
        var original = level.getBlockEntity(origin); var candidate = level.getBlockEntity(pos); var state = level.getBlockState(pos);
        if (original instanceof WardedBlockEntity) return candidate instanceof WardedBlockEntity ward && ward.ownedBy(player);
        return candidate == null && !state.hasBlockEntity() && !state.isAir() && state.isSolidRender(level, pos);
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        player.swing(hand);
        if (!(player instanceof ServerPlayer server) || hit == null || hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        long now = level.getGameTime(); var times = delays.computeIfAbsent(server.serverLevel(), key -> new java.util.HashMap<>());
        times.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (times.getOrDefault(hit.getBlockPos(), 0L) > now) return InteractionResultHolder.success(stack);
        times.put(hit.getBlockPos().immutable(), now + 10);
        var current = level.getBlockEntity(hit.getBlockPos());
        boolean removing = current instanceof WardedBlockEntity ward && ward.ownedBy(player);
        if (!removing && (current != null || !level.getBlockState(hit.getBlockPos()).isSolidRender(level, hit.getBlockPos()))) return InteractionResultHolder.success(stack);
        var selection = blocks(stack, level, hit.getBlockPos(), hit.getDirection(), player);
        var wand = (CastingWandItem) stack.getItem();
        for (var pos : selection) {
            if (!level.mayInteract(player, pos) || player.blockActionRestricted(level, pos, server.gameMode.getGameModeForPlayer())) continue;
            if (removing) {
                if (level.getBlockEntity(pos) instanceof WardedBlockEntity ward && ward.ownedBy(player)) {
                    level.setBlockAndUpdate(pos, ward.original()); FocusEffectPacket.blockSparkle(level, pos, 16556032);
                }
            } else {
                if (!wand.consumeForPlayer(stack, player, costs(stack), false, false, false)) break;
                if (WardedBlockEntity.protect(server, pos)) {
                    wand.consumeForPlayer(stack, player, costs(stack), false, false, true); FocusEffectPacket.blockSparkle(level, pos, 16556032);
                }
            }
        }
        level.playSound(null, hit.getBlockPos(), TCSounds.ZAP.get(), SoundSource.PLAYERS, .25f, 1);
        return InteractionResultHolder.success(stack);
    }
}
