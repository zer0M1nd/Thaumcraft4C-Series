package tc4revival.client;

/** Optional dependency boundary: Forge never reflects over JEI signatures when JEI is absent. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumcraft",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class InferenceClientEvents {
    private static boolean jei(){return net.minecraftforge.fml.ModList.get().isLoaded("jei");}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void commands(net.minecraftforge.client.event.RegisterClientCommandsEvent e){if(jei())tc4revival.compat.jei.JeiInferenceExport.commands(e);}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void recipes(net.minecraftforge.client.event.RecipesUpdatedEvent e){if(jei())tc4revival.compat.jei.JeiInferenceExport.recipes(e);}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){if(jei())tc4revival.compat.jei.JeiInferenceExport.logout(e);}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent e){if(jei())tc4revival.compat.jei.JeiInferenceExport.tick(e);}
    private InferenceClientEvents(){}
}
