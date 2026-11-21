package emtmodern.client;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public class HammerTooltips {
    public static void broken(List<Component> lines){lines.add(Component.translatable(Screen.hasShiftDown()?"tooltip.EMT.hammer.broken.plsRightClick":"tooltip.EMT.hammer.broken.plsNoRightClick"));}
    public static void electric(List<Component> lines){var player=Minecraft.getInstance().player;lines.add(Component.literal(Component.translatable("tooltip.EMT.hammer.electric").getString().replace("%player%",player==null?"":player.getGameProfile().getName())));}
}
