package wgmodern;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="witchinggadgets",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public class WGJeiEvents {@net.minecraftforge.eventbus.api.SubscribeEvent public static void recipes(net.minecraftforge.client.event.RecipesUpdatedEvent e){if(net.minecraftforge.fml.ModList.get().isLoaded("jei"))WGJei.refresh();}}
