package thaumicomniverse.client;

import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import thaumicomniverse.content.ObeliskCompassItem;

@Mod.EventBusSubscriber(modid = "thaumicomniverse", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CollaborationClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (net.minecraftforge.fml.ModList.get().isLoaded("create")) thaumicomniverse.compat.create.CreateArcaneClient.setup();
            if (net.minecraftforge.fml.ModList.get().isLoaded("create") && net.minecraftforge.fml.ModList.get().isLoaded("ponder"))
                thaumicomniverse.compat.ponder.OmniPonder.register();
            if (net.minecraftforge.fml.ModList.get().isLoaded("railcraft")) ArcaneRailClient.setup();
            if (net.minecraftforge.fml.ModList.get().isLoaded("sophisticatedbackpacks")) BackpackClient.setup();
            for (String name : new String[]{"sinister_compass", "primal_compass"}) {
                ItemProperties.register(ForgeRegistries.ITEMS.getValue(new ResourceLocation("thaumicomniverse", name)),
                        new ResourceLocation("angle"), new CompassItemPropertyFunction((level, stack, entity) -> ObeliskCompassItem.target(stack)));
            }
        });
    }
    @SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event){
        if(net.minecraftforge.fml.ModList.get().isLoaded("create"))thaumicomniverse.compat.create.CreateArcaneClient.renderers(event);
        if(net.minecraftforge.fml.ModList.get().isLoaded("railcraft"))ArcaneRailClient.renderers(event);
    }
}
