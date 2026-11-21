package tc4revival.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

/** Native equipment-slot grouping with the appropriate beneficial/harmful attribute colors. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class RunicTooltip {
    private RunicTooltip() {}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void tooltip(ItemTooltipEvent event) {
        var lines=event.getToolTip();for(int i=0;i<lines.size();i++)lines.set(i,style(lines.get(i)));
    }
    private static Component style(Component line) {
        if(line.getContents() instanceof TranslatableContents text && text.getKey().startsWith("attribute.modifier.")
                && text.getArgs().length>=2 && text.getArgs()[1] instanceof Component name
                && name.getContents() instanceof TranslatableContents attribute) {
            String key=attribute.getKey();
            if(key.startsWith("attribute.name.thaumcraft.") && key.endsWith("vis_cost")) {
                try {
                    double amount=Double.parseDouble(text.getArgs()[0].toString().replace(",",""));
                    if(text.getKey().endsWith(".0"))amount*=100;
                    boolean increase=text.getKey().contains(".plus.");
                    return Component.literal((increase?"+":"-")+new java.text.DecimalFormat("0.##").format(amount)+"% ")
                            .append(name.copy()).withStyle(increase?ChatFormatting.RED:ChatFormatting.BLUE);
                }catch(NumberFormatException ignored) { return line; }
            }
            if(key.equals("attribute.name.thaumcraft.warp"))return line.copy().withStyle(ChatFormatting.DARK_PURPLE);
            if(key.equals("attribute.name.thaumcraft.runic_shield"))return line.copy().withStyle(ChatFormatting.YELLOW);
        }
        if(line.getSiblings().isEmpty())return line;
        var copy=line.copy();copy.getSiblings().clear();
        for(var sibling:line.getSiblings())copy.append(style(sibling));
        return copy;
    }
}
