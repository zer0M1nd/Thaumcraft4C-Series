package advancedthaumaturgy;

import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import tc4revival.content.*;
import tc4revival.aspect.*;
import thaumcraft.api.nodes.NodeApi;
import advancedthaumaturgy.CrystalItem.Upgrade;

public class MercurialWandItem extends CastingWandItem {
    public static final String ROD = "advancedthaumaturgy:mercurial";
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.EPIC; }
    @Override public WandParts.Rod rod(ItemStack stack) { return WandParts.Rod.require(ROD); }
    @Override public float consumptionModifier(ItemStack stack, Player player, Aspect aspect, boolean crafting) {
        return Math.max(0, super.consumptionModifier(stack, player, aspect, crafting) - (CrystalItem.enabled(stack, Upgrade.DISCOUNT) ? .1f : 0));
    }
    @Override public float focusPotency(ItemStack stack, Player player) { return super.focusPotency(stack, player) + (CrystalItem.enabled(stack, Upgrade.POTENCY) ? 2 : 0); }
    /** Returns unaccepted INPUT, not multiplied output; simulations never mutate the stack. */
    @Override public int addVis(ItemStack stack, Aspect aspect, int amount, boolean whole, boolean commit) {
        if (amount < 0) throw new IllegalArgumentException("Negative vis transfer");
        if (!rechargeable(stack) || amount == 0) return amount;
        int factor = CrystalItem.enabled(stack, Upgrade.MULTIPLIER) ? 2 : 1;
        if (!aspect.isPrimal()) {
            if (!CrystalItem.enabled(stack, Upgrade.COMPOUND)) return amount;
            var primals = TCAspects.catalog().toPrimals(new AspectAmounts().add(aspect, 1), AspectCatalog.Reduction.SUM);
            long units = whole ? 100L : 1L;
            int accepted = amount;
            for (var a : primals.aspects()) accepted = (int)Math.min(accepted, (capacity(stack) - vis(stack, a)) / (units * primals.amount(a)));
            if (commit && accepted > 0) for (var a : primals.aspects()) super.addVis(stack, a, accepted * primals.amount(a), whole, true);
            return amount - accepted;
        }
        long units = whole ? 100L : 1L;
        long room = capacity(stack) - vis(stack, aspect);
        int accepted = (int)Math.min(amount, (room + units * factor - 1) / (units * factor));
        int output = (int)Math.min(room, accepted * units * factor);
        if (commit && output > 0) super.addVis(stack, aspect, output, false, true);
        return amount - accepted;
    }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !CrystalItem.enabled(stack, Upgrade.RECHARGE) || entity.tickCount % 30 != 0) return;
        var candidates = TCAspects.catalog().primals().stream().filter(a -> vis(stack, a) < capacity(stack) * .15 - 1).toList();
        if (!candidates.isEmpty()) addVis(stack, candidates.get(level.random.nextInt(candidates.size())), 1, true, true);
    }
    @Override public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!CrystalItem.enabled(stack, Upgrade.COMPOUND) || !(user instanceof Player player)) { super.onUseTick(level, user, stack, remaining); return; }
        var hit = player.pick(player.getBlockReach(), 1, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK || !(level.getBlockEntity(block.getBlockPos()) instanceof AuraNodeBlockEntity node)) {
            super.onUseTick(level, user, stack, remaining); return;
        }
        if (level.isClientSide || remaining % 5 != 0 || !(player instanceof net.minecraft.server.level.ServerPlayer server)) return;
        var research = tc4revival.research.KnowledgeSavedData.get(server.getServer()).snapshot(player.getUUID()).map(v -> v.researchMarkers()).orElse(Set.of());
        int rate = 1 + (research.contains("NODETAPPER1") ? 1 : 0) + (research.contains("NODETAPPER2") ? 1 : 0);
        boolean preserve = !player.isShiftKeyDown() && research.contains("NODEPRESERVE");
        var candidates = node.aspects().aspects().stream().filter(a -> node.aspects().amount(a) > (preserve ? 1 : 0) && addVis(stack, a, 1, true, false) == 0).toList();
        if (candidates.isEmpty()) return;
        var aspect = candidates.get(level.random.nextInt(candidates.size()));
        int amount = Math.min(rate, node.aspects().amount(aspect) - (preserve ? 1 : 0));
        int accepted = amount - addVis(stack, aspect, amount, true, false);
        if (accepted > 0 && NodeApi.take(node, thaumcraft.api.aspects.Aspect.wrap(aspect), accepted)) {
            addVis(stack, aspect, accepted, true, true);
            tc4revival.network.FocusEffectPacket.nodeZap(level, node.getBlockPos().getCenter(), player.getEyePosition());
        }
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        boolean wasCrafting=context.getLevel().getBlockEntity(context.getClickedPos()) instanceof InfusionMatrixBlockEntity old && old.crafting();
        var result = super.onItemUseFirst(stack, context);
        if (!context.getLevel().isClientSide && !wasCrafting && ATConfig.enabled("wand_upgrades") && CrystalItem.enabled(stack, Upgrade.STABILIZER)
                && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof InfusionMatrixBlockEntity matrix && matrix.crafting())
            StabilizerTracker.monitor(matrix);
        return result;
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<net.minecraft.network.chat.Component> lines, TooltipFlag flags) {
        super.appendHoverText(stack, level, lines, flags);
        for (var upgrade : Upgrade.values()) if (CrystalItem.has(stack, upgrade)) lines.add(net.minecraft.network.chat.Component.translatable("advancedthaumaturgy.upgrade." + upgrade.name().toLowerCase(Locale.ROOT)).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
    }
}
