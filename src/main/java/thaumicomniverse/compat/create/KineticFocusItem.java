package thaumicomniverse.compat.create;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;
import tc4revival.content.*;
import thaumcraft.api.wands.ItemFocusBasic;

/** A wand supplies a real Create kinetic network through the selected shaft face. */
public class KineticFocusItem extends ItemFocusBasic {
    public static int rpm(ItemStack focus) {
        return focus.hasTag() && focus.getTag().contains("KineticRPM")
                ? (focus.getTag().getInt("KineticRPM")<0?-1:1)*net.minecraft.util.Mth.clamp(Math.abs(focus.getTag().getInt("KineticRPM")),1,256) : 128;
    }
    public static boolean programmed(ItemStack focus) {
        return focus.getItem() instanceof KineticFocusItem item
                && item.upgradeLevel(focus, FocusUpgrade.ARCHITECT.id) > 0;
    }
    public static int programSize(ItemStack focus) {
        return 5 + (programmed(focus) ? 3 * Math.min(2,
                ((KineticFocusItem) focus.getItem()).upgradeLevel(focus, FocusUpgrade.ENLARGE.id)) : 0);
    }
    @Override public int color(ItemStack focus) { return 0xCB973E; }
    @Override public Map<Aspect,Integer> costs(ItemStack focus) {
        return Map.of(TCAspects.catalog().require("aer"), 2, TCAspects.catalog().require("ignis"), 2,
                TCAspects.catalog().require("ordo"), 2);
    }
    @Override public boolean costPerTick(ItemStack focus) { return true; }
    @Override public String sortingPrefix() { return "KINETIC"; }
    @Override public String ornamentTexture(){return "thaumicomniverse:item/focus_kinetic_orn";}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus, int rank) {
        if (rank == 3 && !programmed(focus))
            return List.of(FocusUpgrade.POTENCY, FocusUpgrade.FRUGAL, FocusUpgrade.ARCHITECT);
        if (rank > 3 && programmed(focus) && upgradeLevel(focus, FocusUpgrade.ENLARGE.id) < 2)
            return List.of(FocusUpgrade.POTENCY, FocusUpgrade.FRUGAL, FocusUpgrade.ENLARGE);
        return List.of(FocusUpgrade.POTENCY, FocusUpgrade.FRUGAL);
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack wand, Level level, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!ArcaneContraptions.enabled()) return InteractionResultHolder.fail(wand);
        var focus = attached(wand);
        if (level.isClientSide) {
            if (!programmed(focus)) player.startUsingItem(hand);
            return InteractionResultHolder.consume(wand);
        }
        if (!PowerFocusSessions.begin(player, hand, wand, hit))
            return InteractionResultHolder.fail(wand);
        if (!programmed(focus)) player.startUsingItem(hand);
        return InteractionResultHolder.consume(wand);
    }
    @Override public void usingTick(ItemStack wand, Player player, int remaining) {
        if (!player.level().isClientSide && !PowerFocusSessions.hold(player, wand)) player.stopUsingItem();
    }
    @Override public void release(ItemStack wand, Level level, Player player, int remaining) {
        if (!level.isClientSide) PowerFocusSessions.release(player);
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.translatable("thaumicomniverse.focus.rpm", rpm(stack)));
    }
}
