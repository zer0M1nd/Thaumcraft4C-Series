package tc4revival.client;
import java.util.*;
import net.minecraft.network.Connection;
import net.minecraft.client.Minecraft;
import tc4revival.network.BrowserRecipesPacket;
import tc4revival.research.BrowserRecipe;
/** Optional JEI attaches a consumer; the common network remains usable without JEI. */
public class ClientBrowserRecipes {
    /** Incremented by the existing login/datapack synchronization packet, including aspect-only reloads. */
    public static int dataVersion;
    public static java.util.function.Consumer<BrowserRecipesPacket> receiver=packet->{};
    public static void receive(BrowserRecipesPacket packet,Connection source){var mc=Minecraft.getInstance();if(mc.getConnection()!=null&&mc.getConnection().getConnection()==source)receiver.accept(packet);}
}
