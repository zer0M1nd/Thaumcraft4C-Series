package heartwork.client;

import heartwork.Heartwork;
import heartwork.content.SwissItem;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid=Heartwork.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}
    @SubscribeEvent public static void models(net.minecraftforge.client.event.ModelEvent.ModifyBakingResult event) {
        if(net.minecraftforge.fml.ModList.get().isLoaded("slashblade"))heartwork.compat.blade.BladeClient.models(event);
    }
    @SubscribeEvent public static void keys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        if(net.minecraftforge.fml.ModList.get().isLoaded("slashblade"))heartwork.compat.blade.BladeClient.keys(event);
    }
    @SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Heartwork.STRAW.get(),StrawRenderer::new);
        if(net.minecraftforge.fml.ModList.get().isLoaded("slashblade"))heartwork.compat.blade.BladeClient.renderers(event);
    }
    @SubscribeEvent public static void layers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StrawModel.LAYER,StrawModel::createBodyLayer);
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Heartwork.ITEM_MAP.values().forEach(item -> {
                if (item.get() instanceof SwissItem) ItemProperties.register(item.get(),new ResourceLocation(Heartwork.ID,"active"),
                        (stack, level, entity, seed) -> SwissItem.active(stack) ? 1 : 0);
            });
            Heartwork.BLOCK_MAP.values().forEach(block -> {
                if (block.get() instanceof heartwork.content.HeartworkCrop) ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutout());
            });
        });
    }
}
