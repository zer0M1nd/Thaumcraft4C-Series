package automagy.client;

import automagy.Automagy;
import automagy.content.Content;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Automagy.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Content.MAW_TILE.get(),MawRenderer::new);
        event.registerBlockEntityRenderer(Content.SCRIBE_TILE.get(),UnseenScribeRenderer::new);
        event.registerBlockEntityRenderer(Content.THAUMOSTATIC_SPIRE_TILE.get(),ThaumostaticSpireRenderer::new);
        event.registerBlockEntityRenderer(Content.MOB_LURE_TILE.get(),MobLureRenderer::new);
        event.registerBlockEntityRenderer(Content.NETHERMIND_TILE.get(),NethermindRenderer::new);
        event.registerBlockEntityRenderer(Content.DIMENSION_LURE_TILE.get(),DimensionLureRenderer::new);
        event.registerBlockEntityRenderer(Content.EXPERIENCE_JAR_TILE.get(),ExperienceJarRenderer::new);
        event.registerBlockEntityRenderer(Content.INFINITY_JAR_TILE.get(),InfinityJarRenderer::new);
        event.registerBlockEntityRenderer(Content.EXPERIENCE_STONE_TILE.get(),ExperienceStoneRenderer::new);
        event.registerBlockEntityRenderer(Content.GOLEM_WORKBENCH_TILE.get(),GolemWorkbenchRenderer::new);
        event.registerBlockEntityRenderer(Content.THIRSTY_TANK_TILE.get(),ThirstyTankRenderer::new);
        event.registerBlockEntityRenderer(Content.BOILER_TILE.get(),BoilerRenderer::new);
        event.registerBlockEntityRenderer(Content.TASKMASTER_TILE.get(),context->new GolemDeviceRenderer<>(false));
        event.registerBlockEntityRenderer(Content.INHIBITOR_TILE.get(),context->new GolemDeviceRenderer<>(true));
        event.registerBlockEntityRenderer(Content.DELIVERY_MIRROR_TILE.get(),DeliveryMirrorRenderer::new);
        event.registerBlockEntityRenderer(Content.REQUISITION_TOME_TILE.get(),RequisitionTomeRenderer::new);
        event.registerEntityRenderer(Content.HOVERING_ITEM.get(),net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        event.registerEntityRenderer(Content.NETHER_WISP.get(),NetherWispRenderer::new);
        event.registerEntityRenderer(Content.SUPPRESSED_ORB.get(),net.minecraft.client.renderer.entity.ExperienceOrbRenderer::new);
        event.registerEntityRenderer(Content.PEARL_ENTITY.get(),net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerBlockEntityRenderer(Content.ESSENTIA_LOCUS_TILE.get(),EssentiaLocusRenderer::new);
        event.registerBlockEntityRenderer(Content.INVENTARIUM_TILE.get(), InventariumRenderer::new);
        event.registerBlockEntityRenderer(Content.TENACIOUS_TILE.get(), TenaciousChestRenderer::new);
        event.registerBlockEntityRenderer(Content.GREEDY_TILE.get(), GreedyChestRenderer::new);
        event.registerBlockEntityRenderer(Content.EAGER_TILE.get(), EagerChestRenderer::new);
        event.registerBlockEntityRenderer(Content.HOURGLASS_TILE.get(), HourglassRenderer::new);
        event.registerBlockEntityRenderer(Content.REDCRYSTAL_TILE.get(), RedcrystalRenderer::new);
        event.registerBlockEntityRenderer(Content.REMOTE_COMPARATOR_TILE.get(), RemoteComparatorRenderer::new);
    }
    @SubscribeEvent public static void additionalModels(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {
        event.register(CraftingFocusOrnamentRenderer.ENTRY);
        event.register(CraftingFocusOrnamentRenderer.SPRITE);
    }
    @SubscribeEvent public static void reload(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(ExternalObj.BOILER);
        event.registerReloadListener(ExternalObj.HOURGLASS);
        event.registerReloadListener(ExternalObj.REDCRYSTAL);
        event.registerReloadListener(ExternalObj.MERCCRYSTAL);
        event.registerReloadListener(ExternalObj.COMPARATOR);
    }
    @SubscribeEvent public static void colors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
        String[] names = {"air", "fire", "water", "earth", "order", "entropy"};
        int[] colors = {0xffff7e, 0xff5a01, 0x3cd4fc, 0x56c000, 0xd5d4ec, 0x404040};
        for (int i=0; i<names.length; i++) {
            final int color = colors[i];
            event.register((stack, tint) -> color, Content.SLIVERS.get(names[i]).get());
            event.register((stack,tint)->tint==0?color:0xffffff,Content.CRYSTAL_BRAINS.get(names[i]).get());
        }
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(()->{for(var soup:java.util.List.of(automagy.content.SoupFluids.MUSHROOM,automagy.content.SoupFluids.VISHROOM)){net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(soup.source.get(),net.minecraft.client.renderer.RenderType.translucent());net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(soup.flowing.get(),net.minecraft.client.renderer.RenderType.translucent());}});
        event.enqueueWork(()->MenuScreens.register(Content.GOLEM_WORKBENCH_MENU.get(),GolemWorkbenchScreen::new));
        event.enqueueWork(()->MenuScreens.register(Content.BOILER_MENU.get(),BoilerScreen::new));
        event.enqueueWork(()->MenuScreens.register(Content.MAW_MENU.get(),MawScreen::new));
        event.enqueueWork(()->MenuScreens.register(Content.MIRROR_SETTINGS_MENU.get(),MirrorSettingsScreen::new));
        event.enqueueWork(()->MenuScreens.register(Content.AUTO_HAND_MIRROR_MENU.get(),AutoHandMirrorScreen::new));
        event.enqueueWork(() -> MenuScreens.register(Content.INVENTARIUM_MENU.get(), InventariumScreen::new));
        event.enqueueWork(() -> MenuScreens.register(Content.PORTABLE_CRAFTING_MENU.get(), PortableCraftingScreen::new));
        event.enqueueWork(() -> MenuScreens.register(Content.FILTER_MENU.get(), FilterScreen::new));
        event.enqueueWork(() -> MenuScreens.register(Content.GREEDY_MENU.get(), GreedyChestScreen::new));
        event.enqueueWork(() -> MenuScreens.register(Content.EAGER_MENU.get(), EagerChestScreen::new));
        event.enqueueWork(() -> MenuScreens.register(Content.TALLY_MENU.get(), TallyScreen::new));
        event.enqueueWork(() -> MenuScreens.register(Content.HOURGLASS_MENU.get(), HourglassScreen::new));
        event.enqueueWork(() -> MenuScreens.register(Content.VIS_READER_MENU.get(), VisReaderScreen::new));
        event.enqueueWork(() -> MenuScreens.register(Content.ENTITY_SENSOR_MENU.get(), EntitySensorScreen::new));
    }
}
