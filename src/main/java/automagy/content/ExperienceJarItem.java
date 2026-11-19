package automagy.content;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
public final class ExperienceJarItem extends BlockItem {
 public ExperienceJarItem(Block block){super(block,new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));}
 public static ItemStack full(){var stack=new ItemStack(Content.EXPERIENCE_JAR.get());stack.getOrCreateTagElement("BlockEntityTag").putInt("XP",ExperienceJarBlockEntity.CAPACITY);return stack;}
 @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){var tag=stack.getTagElement("BlockEntityTag");int xp=tag==null?0:Math.max(0,Math.min(ExperienceJarBlockEntity.CAPACITY,tag.getInt("XP")));lines.add(net.minecraft.network.chat.Component.translatable("tooltip.automagy.experience_jar",String.format(java.util.Locale.ROOT,"%.2f",automagy.logic.ExperienceLevels.level(xp)),xp));}
 @Override public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer){consumer.accept(new IClientItemExtensions(){private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new automagy.client.ExperienceJarItemRenderer();return renderer;}});}
}
