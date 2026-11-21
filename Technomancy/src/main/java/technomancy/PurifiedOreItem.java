package technomancy;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public class PurifiedOreItem extends Item {
    public final int stage;
    public PurifiedOreItem(int stage){super(new Properties());this.stage=stage;}
    @Override public Component getName(ItemStack stack){var ingot=OreProcessing.ingot(stack);return Component.translatable("item.technomancy.purified_ore",ingot.isEmpty()?Component.literal("?"):ingot.getHoverName());}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
        lines.add(Component.translatable("tooltip.technomancy.purity",stage+1,stage+2));
        var passes=stack.hasTag()?stack.getTag().getIntArray("passes"):new int[0];
        for(int i=0;i<3;i++)lines.add(Component.translatable("tooltip.technomancy.passes",new String[]{"Thaumcraft","Botania","Blood Magic"}[i],i<passes.length?passes[i]:0));
    }
}
