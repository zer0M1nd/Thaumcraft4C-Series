package tc4revival.content;

import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public  class VisAmuletItem extends Item implements ICurioItem, WandInventory.VisAmulet, RunicGear {
    private final boolean greater;
    public VisAmuletItem(boolean greater) { super(new Properties().stacksTo(1).rarity(greater ? Rarity.RARE : Rarity.UNCOMMON)); this.greater = greater; }
    public int capacity(ItemStack stack) { return greater ? 25000 : 2500; }
    public int vis(ItemStack stack, Aspect aspect) { return WandVis.stored(stack.getTag(), aspect); }
    public int addVis(ItemStack stack, Aspect aspect, int amount, boolean whole, boolean commit) {
        var tag = stack.hasTag() ? stack.getTag().copy() : new net.minecraft.nbt.CompoundTag();
        int remainder = WandVis.add(tag, aspect, amount, capacity(stack), whole, commit); if (commit) stack.setTag(tag); return remainder;
    }
    @Override public boolean consumeVis(ItemStack stack, Player player, Map<Aspect, Integer> costs, boolean commit) {
        if (player.level().isClientSide) return false;
        var tag = stack.hasTag() ? stack.getTag().copy() : new net.minecraft.nbt.CompoundTag();
        boolean result = WandVis.consume(tag, costs, aspect -> 1, false, commit); if (result && commit) stack.setTag(tag); return result;
    }
    @Override public void curioTick(SlotContext context, ItemStack stack) {
        var wearer = context.entity();
        if (wearer.level().isClientSide || wearer.tickCount % 5 != 0) return;
        var held = wearer.getMainHandItem();
        if (held.getItem() instanceof CastingWandItem wand) for (var aspect : TCAspects.catalog().primals()) {
            int amount = Math.min(5, Math.min(wand.capacity(held) - wand.vis(held, aspect), vis(stack, aspect)));
            amount = wand.receiveVis(held, aspect, Math.max(0, amount), true);
            if (amount > 0) {
                int accepted = wand.receiveVis(held, aspect, amount, false);
                stack.getOrCreateTag().putInt(aspect.tag(), vis(stack, aspect) - accepted);
            }
        }
        if (wearer instanceof Player player) {
            var relay = VisRelayBlockEntity.nearby(player);
            if (relay != null) for (var aspect : TCAspects.catalog().primals()) {
                int requested = Math.min(5, capacity(stack) - vis(stack, aspect));
                if (requested > 0) { int received = relay.consumeVis(aspect, requested); if (received > 0) addVis(stack, aspect, received, false, true); }
            }
        }
    }
    @Override public boolean canEquip(SlotContext context, ItemStack stack) {
        if (!greater) return true;
        if (context.entity() instanceof ServerPlayer player) return tc4revival.research.KnowledgeSavedData.get(player.getServer())
                .snapshot(player.getUUID()).map(knowledge -> knowledge.researchMarkers().contains("VISAMULET")).orElse(false);
        return context.entity().level().isClientSide && tc4revival.client.ClientKnowledge.snapshot()
                .map(knowledge -> knowledge.researchMarkers().contains("VISAMULET")).orElse(false);
    }
    @Override public boolean canEquipFromUse(SlotContext context, ItemStack stack) { return false; }
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack material) { return false; }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flags) {
        if (!greater) lines.add(Component.translatable("item.ItemAmuletVis.text").withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("item.capacity.text").append(" " + capacity(stack) / 100).withStyle(ChatFormatting.GOLD));
        if(tc4revival.client.AspectTooltips.enabled())return;
        for (var aspect : TCAspects.catalog().primals()) if (stack.hasTag() && stack.getTag().contains(aspect.tag())) {
            String amount = java.math.BigDecimal.valueOf(vis(stack, aspect), 2).stripTrailingZeros().toPlainString();
            lines.add(Component.literal(" " + thaumcraft.api.aspects.AspectViews.display(aspect).displayName()).withStyle(style -> style.withColor(thaumcraft.api.aspects.AspectViews.display(aspect).color())).append(Component.literal(" x " + amount).withStyle(ChatFormatting.RESET)));
        }
    }
}
