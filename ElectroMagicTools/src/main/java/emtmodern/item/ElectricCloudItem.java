package emtmodern.item;
import emtmodern.ElectroMagicTools;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
public class ElectricCloudItem extends BlockItem {
    public ElectricCloudItem(){super(ElectroMagicTools.CLOUD.get(),new Properties());}
    @Override public Component getName(ItemStack stack){return super.getName(stack).copy().withStyle(ChatFormatting.DARK_PURPLE,ChatFormatting.BOLD);}
}
