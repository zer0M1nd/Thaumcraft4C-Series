package tc4revival.content;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;

public  class TradeFocusItem extends ArchitectFocusItem {
    @Override public String sortingPrefix() { return "BT"; }
    @Override public String ornamentTexture() { return "focus_trade_orn"; }
    @Override public int color(ItemStack focus) { return 8747923; }
    @Override public int maximumArea(ItemStack focus) { return thaumcraft.api.wands.SpellParameters.bounded("architect.radius",3 + upgradeLevel(focus, FocusUpgrade.ENLARGE.id) * 2,12); }
    @Override public boolean volume() { return false; }
    @Override public Map<Aspect, Integer> costs(ItemStack focus) {
        var catalog = TCAspects.catalog();
        return upgradeLevel(focus, FocusUpgrade.SILK_TOUCH.id) > 0
                ? Map.of(catalog.require("aer"), 1, catalog.require("ignis"), 1, catalog.require("aqua"), 1, catalog.require("terra"), 6, catalog.require("ordo"), 6, catalog.require("perditio"), 6)
                : Map.of(catalog.require("terra"), 5, catalog.require("ordo"), 5, catalog.require("perditio"), 5);
    }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1, 2, 4 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.ENLARGE);
            case 3 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.ENLARGE, FocusUpgrade.TREASURE, FocusUpgrade.ARCHITECT);
            case 5 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.ENLARGE, FocusUpgrade.SILK_TOUCH);
            default -> List.of();
        };
    }
    public static ItemStack picked(ItemStack wand) {
        if (!wand.hasTag() || !wand.getTag().contains("picked", Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
        var picked = ItemStack.of(wand.getTag().getCompound("picked"));
        return picked.getItem() instanceof BlockItem ? picked : ItemStack.EMPTY;
    }
    private static BlockHitResult target(Player player) {
        var eye = player.getEyePosition(); return player.level().clip(new net.minecraft.world.level.ClipContext(eye, eye.add(player.getLookAngle().scale(player.getBlockReach())),
                net.minecraft.world.level.ClipContext.Block.OUTLINE, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack wand, Level level, Player player, InteractionHand hand, BlockHitResult ignored) {
        var hit = target(player); if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(wand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && level.getBlockEntity(hit.getBlockPos()) == null) {
                var state = level.getBlockState(hit.getBlockPos());
                var item = state.getCloneItemStack(hit, level, hit.getBlockPos(), player);
                if (item.getItem() instanceof BlockItem) { item.setCount(1); wand.getOrCreateTag().put("picked", item.save(new CompoundTag())); }
            }
        } else if (!picked(wand).isEmpty()) {
            if (player instanceof ServerPlayer server) {
                if (architect(wand)) for (var pos : blocks(wand, level, hit.getBlockPos(), hit.getDirection(), player)) BlockSwaps.enqueue(server, hand, pos, picked(wand), 0);
                else BlockSwaps.enqueue(server, hand, hit.getBlockPos(), picked(wand), thaumcraft.api.wands.SpellParameters.bounded("trade.range",3 + upgradeLevel(attached(wand), FocusUpgrade.ENLARGE.id),32));
            }
        }
        // SUCCESS already asks Minecraft to swing after the use-item packet. Sending an extra
        // predicted swing here can execute a one-block swap before the range cast reaches the server.
        return InteractionResultHolder.sidedSuccess(wand, level.isClientSide);
    }
    @Override public boolean swing(ItemStack wand, LivingEntity entity) {
        if (entity instanceof ServerPlayer player && !picked(wand).isEmpty()) {
            var hit = target(player);
            if (hit.getType() == HitResult.Type.BLOCK) BlockSwaps.enqueue(player, player.getOffhandItem() == wand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                    hit.getBlockPos(), picked(wand), 0);
        }
        return false;
    }
    @Override protected boolean eligible(Level level, BlockPos origin, BlockPos pos, Player player) {
        var state = level.getBlockState(pos);
        return !state.isAir() && state.equals(level.getBlockState(origin)) && state.getDestroySpeed(level, pos) >= 0 && BlockSwaps.exposed(level, pos) && level.mayInteract(player, pos);
    }
}
