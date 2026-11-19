package conciliummodern;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Upstream creative utility: removes rifts, but does not waive survival polishment costs. */
public class PolishmentDevice extends Item {
    public PolishmentDevice(){super(new Properties().stacksTo(1));}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){
        lines.add(Component.translatable("tc.tooltip.polishment"));lines.add(Component.translatable("tc.tooltip.creative"));
    }
}
