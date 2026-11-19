package conciliummodern;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ConciliumClient {
    @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers event){for(String skin:event.getSkins()){var renderer=event.getSkin(skin);if(renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer player){player.addLayer(new CastingBraceletLayer(player));player.addLayer(new ConciliumCompanionLayer(player));}}}
    @SubscribeEvent public static void colors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event){event.register((stack,index)->DrainageSyringeRenderer.COLORS[DrainageSyringe.type(stack)],ConciliumItems.DRAINAGE_SYRINGE.get());}
    @SubscribeEvent public static void reload(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event){
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)resources->{VisCondenserRenderer.reload();PredictabilityRenderer.reload();AstralMonitorRenderer.reload();LithographerRenderer.reload();DrumRenderer.reload();ShardMillRenderer.reload();DrainageSyringeRenderer.reload();CastingBraceletRenderer.reload();DimensionalGemRenderer.reload();});
    }
    @SubscribeEvent public static void shaders(net.minecraftforge.client.event.RegisterShadersEvent event)throws java.io.IOException{ConciliumShaders.register(event);PredictabilityRenderer.shaders(event);AstralMonitorRenderer.shaders(event);LithographyOverlay.shaders(event);DistortionRenderer.shaders(event);}
    @SubscribeEvent public static void overlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event){LithographyOverlay.register(event);}
    @SubscribeEvent public static void models(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {
        event.register(DrainageSyringeRenderer.LIQUID);
        event.register(VoidBodyRenderer.BODY);for(var model:VoidBodyRenderer.LIMBS)event.register(model);
        event.register(DistortionRenderer.MODEL);
        for(String name:java.util.List.of("taint_animation_orn","ethereal_manipulator_orn","vis_conductor_orn","impulse_orn","impulse_orn_inversed","impulse_orn_diluted"))
            event.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ThaumicConcilium.ID,"item/"+name));
    }
    @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(ThaumaturgeMenu.TYPE.get(),ThaumaturgeScreen::new));
        event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(AstralMonitorMenu.TYPE.get(),AstralMonitorScreen::new));
        event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(LithographerMenu.TYPE.get(),LithographerScreen::new));
        event.enqueueWork(()->thaumcraft.api.client.ResearchPageRenderers.register(net.minecraft.resources.ResourceLocation.parse("thaumicconcilium:chained_rift"),ChainedRiftPage::new));
        event.enqueueWork(()->thaumcraft.api.client.ResearchPageRenderers.register(net.minecraft.resources.ResourceLocation.parse("thaumicconcilium:polishment"),PolishmentPage::new));
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ConciliumBlocks.RED_MIND_TILE.get(),RedPoweredMindRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.RED_MIND.get(),RedPoweredMindRenderer.EntityRenderer::new);
        event.registerBlockEntityRenderer(ConciliumBlocks.SOLID_VOID_TILE.get(),VoidBodyRenderer.Block::new);
        event.registerEntityRenderer(ConciliumEntities.PROTOLIMB.get(),VoidBodyRenderer.Limb::new);
        event.registerEntityRenderer(ConciliumEntities.PROTOBODY_DROP.get(),net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        event.registerBlockEntityRenderer(ConciliumBlocks.QUICKSILVER_CRUCIBLE_TILE.get(),ConciliumCrucibleRenderer::new);
        event.registerBlockEntityRenderer(ConciliumBlocks.FLESH_CRUCIBLE_TILE.get(),ConciliumCrucibleRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.QUICKSILVER_ELEMENTAL.get(),QuicksilverRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.SHARD_POWDER.get(),FirePowderRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.DISTORTION_FIELD.get(),DistortionRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.WARP_CHARGE.get(),WarpChargeRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.WRATH_GLYPH.get(),net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.SMART_TAINT_SPIDER.get(),tc4revival.client.TaintSpiderRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.PEELER.get(),MaterialPeelerRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.BRIGHTEST.get(),BrightestRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.FIRE_POWDER.get(),FirePowderRenderer::new);
        event.registerBlockEntityRenderer(ConciliumBlocks.CRYSTAL_TILE.get(),DestabilizedCrystalRenderer::new);
        event.registerBlockEntityRenderer(ConciliumBlocks.CONDENSER_TILE.get(),VisCondenserRenderer::new);
        event.registerBlockEntityRenderer(ConciliumBlocks.HEX_TILE.get(),PredictabilityRenderer::new);
        event.registerBlockEntityRenderer(ConciliumBlocks.LITHOGRAPHER_TILE.get(),LithographerRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.CLEAR_WATER.get(),net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.THICK_TAINT.get(),net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.CHARGED_WISP.get(),ChargedWispRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.COMPRESSED_BLAST.get(),CompressedBlastRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.POSITIVE_ORB.get(),PositiveBurstRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.MAD_THAUMATURGE.get(),MadThaumaturgeRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.THAUMATURGE.get(),ThaumaturgeRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.RIFT.get(),RiftRenderer::new);
        event.registerEntityRenderer(ConciliumEntities.NETHER_EXPLORER.get(),NetherExplorerRenderer::new);
    }
}
