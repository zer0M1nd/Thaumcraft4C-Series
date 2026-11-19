package gadomancymodern.item;
import gadomancymodern.Gadomancy;
import net.minecraft.world.item.*;
public class ExtendedNodeJarItem extends BlockItem implements tc4revival.content.StoredAspectItem {
    public ExtendedNodeJarItem(){super(Gadomancy.EXTENDED_JAR.get(),new Properties().stacksTo(1));}
    @Override public String getDescriptionId(){return tc4revival.content.TCItems.NODE_JAR.get().getDescriptionId();}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return tc4revival.client.NodeJarRenderer.itemRenderer();}});}
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flags){tc4revival.content.TCItems.NODE_JAR.get().appendHoverText(stack,level,lines,flags);if(stack.hasTag()&&stack.getTag().getCompound("Gadomancy").getString("Type").equals("gadomancy:growing"))lines.add(net.minecraft.network.chat.Component.translatable("gadomancy.nodes.GROWING").withStyle(net.minecraft.ChatFormatting.BLUE));}
    @Override public net.minecraft.world.InteractionResult place(net.minecraft.world.item.context.BlockPlaceContext context){var result=super.place(context);if(result.consumesAction()&&context.getPlayer()!=null&&context.getPlayer().getAbilities().instabuild)context.getItemInHand().shrink(1);return result;}
}
