package tc4revival.client;

import java.text.DecimalFormat;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import tc4revival.aspect.TCAspects;
import tc4revival.content.CastingWandItem;
import tc4revival.content.WandFocusItem;

public  class WandTooltip {
    private WandTooltip() {}
    public static void append(ItemStack stack, List<Component> lines, TooltipFlag flags) {
        var wand = (CastingWandItem) stack.getItem(); var player = Minecraft.getInstance().player;
        var format = new DecimalFormat("#######.##"); boolean detail = Screen.hasShiftDown();
        int start = lines.size(), count = 0, total = 0; var compact = Component.empty();
        var attached = WandFocusItem.attached(stack);
        if(AspectTooltips.enabled()) {
            lines.add(Component.translatable("item.capacity.text").withStyle(ChatFormatting.GOLD).append(" "+wand.capacity(stack)/100));
            if(!attached.isEmpty())lines.add(attached.getHoverName().copy().withStyle(ChatFormatting.BOLD,ChatFormatting.ITALIC,ChatFormatting.GREEN));
            return;
        }
        var costs = attached.isEmpty() ? java.util.Map.<tc4revival.aspect.Aspect, Integer>of() : ((WandFocusItem) attached.getItem()).costs(attached);
        for (var aspect : thaumcraft.api.client.ClientAspectViews.primals()) {
            if (!stack.hasTag() || !stack.getTag().contains(aspect.tag())) continue;
            float modifier = wand.consumptionModifier(stack, player, aspect, false);
            total = (int) (total + modifier * 100); count++;
            String amount = format.format(wand.vis(stack, aspect) / 100f);
            if (detail) {
                var line = Component.literal(" " + thaumcraft.api.client.ClientAspectViews.view(aspect).displayName()).withStyle(style -> style.withColor(thaumcraft.api.client.ClientAspectViews.view(aspect).color()))
                        .append(Component.literal(" x " + amount + ", (" + format.format(modifier * 100) + "% ").withStyle(ChatFormatting.RESET))
                        .append(Component.translatable("tc.vis.cost")).append(")");
                int cost = costs.getOrDefault(aspect, 0);
                if (cost > 0) line.append(", " + format.format(cost * modifier / 100f) + " ").append(Component.translatable(
                        ((WandFocusItem) attached.getItem()).costPerTick(attached) ? "item.Focus.cost2" : "item.Focus.cost1"));
                lines.add(line);
            } else {
                if (count > 1) compact.append(" | ");
                compact.append(Component.literal(amount).withStyle(style -> style.withColor(thaumcraft.api.client.ClientAspectViews.view(aspect).color())));
            }
        }
        var capacity = Component.translatable("item.capacity.text").withStyle(ChatFormatting.GOLD).append(" " + wand.capacity(stack) / 100);
        if (!detail && count > 0) {
            capacity.append(Component.literal(" (" + total / count + "% ").withStyle(ChatFormatting.RESET)).append(Component.translatable("tc.vis.costavg")).append(")");
            lines.add(compact);
        }
        lines.add(start, capacity);
        if (!attached.isEmpty()) {
            lines.add(attached.getHoverName().copy().withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC, ChatFormatting.GREEN));
            if (detail) ((WandFocusItem) attached.getItem()).appendUpgrades(attached, lines);
        }
    }
}
