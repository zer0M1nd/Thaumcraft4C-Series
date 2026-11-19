package conciliummodern;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Metadata-one resource from the original item, retaining both explanatory tooltip lines. */
public final class PrimordialLife extends Item {
    public PrimordialLife(){super(new Properties().stacksTo(1).rarity(Rarity.EPIC));}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,level,lines,flag);
        lines.add(Component.translatable("tc.tooltip.life.0").withStyle(ChatFormatting.DARK_PURPLE));
        lines.add(Component.translatable("tc.tooltip.life.1").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
