package thaumicfutures.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import thaumicfutures.compat.machinery.MachineryContent;

@Mod.EventBusSubscriber(modid="thaumicfutures", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MachineryClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("modular_machinery_reborn"))
            event.enqueueWork(() -> MenuScreens.register(MachineryContent.PORT_MENU.get(), AspectPortScreen::new));
    }
}
