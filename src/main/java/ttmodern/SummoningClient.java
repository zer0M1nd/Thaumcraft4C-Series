package ttmodern;

import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid=Tinkerer.ID,value=Dist.CLIENT)
public final class SummoningClient {
    private static List<MobEssenceCatalog.Recipe> recipes=List.of();
    private static net.minecraft.network.Connection source,cropSource;
    private static List<CropDropPreview.Recipe> crops=List.of();
    public static List<CropDropPreview.Recipe> crops(){var connection=net.minecraft.client.Minecraft.getInstance().getConnection();return connection!=null&&connection.getConnection()==cropSource?crops:List.of();}
    public static void receiveCrops(CropDropPreview.Snapshot packet,net.minecraft.network.Connection connection){crops=packet.recipes();cropSource=connection;if(net.minecraftforge.fml.ModList.get().isLoaded("jei"))TinkererJei.refreshCrops();}
    public static List<MobEssenceCatalog.Recipe> recipes(){var connection=net.minecraft.client.Minecraft.getInstance().getConnection();return connection!=null&&connection.getConnection()==source?recipes:List.of();}
    public static void receive(SummoningSync.Snapshot packet,net.minecraft.network.Connection connection){recipes=packet.recipes();source=connection;if(net.minecraftforge.fml.ModList.get().isLoaded("jei"))TinkererJei.refresh();}
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){recipes=List.of();source=null;crops=List.of();cropSource=null;if(net.minecraftforge.fml.ModList.get().isLoaded("jei"))TinkererJei.clear();}
}
