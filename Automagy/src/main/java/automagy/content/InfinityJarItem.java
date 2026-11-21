package automagy.content;
import net.minecraft.world.item.*;
public final class InfinityJarItem extends BlockItem {
 public InfinityJarItem(net.minecraft.world.level.block.Block block){super(block,new Properties().rarity(Rarity.EPIC));}
 @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){lines.add(net.minecraft.network.chat.Component.translatable("tooltip.automagy.creative_only").withStyle(net.minecraft.ChatFormatting.ITALIC));}
}
