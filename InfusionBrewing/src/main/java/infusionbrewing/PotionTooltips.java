package infusionbrewing;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class PotionTooltips {
    private PotionTooltips(){}
    public static void append(ItemStack stack,java.util.List<Component> lines){
        PotionData.read(stack).ifPresentOrElse(data->{
            var text=Component.translatable(data.effect().getDescriptionId());
            if(data.amplifier()>0)text.append(" ").append(Component.translatable("enchantment.level."+(data.amplifier()+1)));
            lines.add(text);
        },()->lines.add(Component.translatable("infusionbrewing.empty_potion")));
    }
}
