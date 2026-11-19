package nodalmechanics;

import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;

public class MatrixItem extends Item {
    public MatrixItem() { super(new Properties()); }
    public static Map<String, Integer> counts(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof MatrixItem) || !stack.hasTag()) return Map.of();
        return NodalRules.counts(stack.getTag().getString("aspects"), key -> TCAspects.catalog().find(key).isPresent());
    }
    public static ItemStack attuned(List<String> tags) {
        var encoded = String.join(",", tags);
        if (NodalRules.counts(encoded, key -> TCAspects.catalog().find(key).isPresent()).isEmpty()) return ItemStack.EMPTY;
        var stack = new ItemStack(NodalMechanics.MATRIX.get());
        stack.getOrCreateTag().putString("aspects", encoded);
        return stack;
    }
    public static AspectAmounts amounts(ItemStack stack, int multiplier) {
        var result = new AspectAmounts();
        counts(stack).forEach((tag, count) -> result.add(TCAspects.catalog().require(tag), count * multiplier));
        return result;
    }
    @Override public String getDescriptionId(ItemStack stack) {
        return "item.nodalmechanics.matrix." + (stack.hasTag() ? "attuned" : "unattuned");
    }
    @Override public Rarity getRarity(ItemStack stack) { return stack.hasTag() ? Rarity.UNCOMMON : Rarity.COMMON; }
    @Override public boolean isFoil(ItemStack stack) { return stack.hasTag(); }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        var counts = counts(stack);
        counts.forEach((tag, count) -> {
            var aspect = TCAspects.catalog().require(tag);
            var color=aspect.chatColor().isEmpty()?ChatFormatting.GRAY:ChatFormatting.getByCode(aspect.chatColor().charAt(0));
            lines.add(Component.literal(thaumcraft.api.aspects.AspectViews.display(aspect).displayName()).withStyle(color==null?ChatFormatting.GRAY:color)
                    .append(Component.literal(" x" + count).withStyle(ChatFormatting.WHITE)));
        });
        lines.add(Component.translatable("tooltip.nodalmechanics.matrix." + (counts.isEmpty() ? "unattuned" : "attuned"))
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
