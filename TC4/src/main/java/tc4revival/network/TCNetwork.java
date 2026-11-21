package tc4revival.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import tc4revival.client.ClientKnowledge;
import tc4revival.client.ClientScanFeedback;
import tc4revival.client.ClientLens;

public  class TCNetwork {
    private static final String PROTOCOL = "47";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.parse("thaumcraft:main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private TCNetwork() { }

    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.messageBuilder(FocalBatchPacket.class,40,NetworkDirection.PLAY_TO_SERVER).encoder(FocalBatchPacket::encode).decoder(FocalBatchPacket::decode)
                .consumerMainThread((packet,context)->packet.handle(context.get().getSender())).add();
            CHANNEL.messageBuilder(FocusListRequest.class,38,NetworkDirection.PLAY_TO_SERVER).encoder(FocusListRequest::encode).decoder(FocusListRequest::decode)
                .consumerMainThread((packet,context)->packet.handle(context.get().getSender())).add();
            CHANNEL.messageBuilder(FocusListReply.class,39,NetworkDirection.PLAY_TO_CLIENT).encoder(FocusListReply::encode).decoder(FocusListReply::decode)
                .consumerMainThread((packet,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->tc4revival.client.FocusRadialScreen.receive(packet,context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(AspectEditorOpen.class,35,NetworkDirection.PLAY_TO_CLIENT).encoder(AspectEditorOpen::encode).decoder(AspectEditorOpen::decode)
                .consumerMainThread((p,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->tc4revival.client.AspectEditorScreen.open(p))).add();
            CHANNEL.messageBuilder(AspectEditorSave.class,36,NetworkDirection.PLAY_TO_SERVER).encoder(AspectEditorSave::encode).decoder(AspectEditorSave::decode)
                .consumerMainThread((p,c)->tc4revival.aspect.editor.AspectEditorServer.save(c.get().getSender(),p)).add();
            CHANNEL.messageBuilder(AspectEditorResult.class,37,NetworkDirection.PLAY_TO_CLIENT).encoder(AspectEditorResult::encode).decoder(AspectEditorResult::decode)
                .consumerMainThread((p,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->tc4revival.client.AspectEditorScreen.result(p))).add();
            CHANNEL.messageBuilder(AspectDataFrame.class,33,NetworkDirection.PLAY_TO_CLIENT).encoder(AspectDataFrame::encode).decoder(AspectDataFrame::decode)
                .consumerMainThread((packet,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->tc4revival.client.ClientItemAspects.receive(packet,context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(AspectDataRequest.class,34,NetworkDirection.PLAY_TO_SERVER).encoder(AspectDataRequest::encode).decoder(AspectDataRequest::decode)
                .consumerMainThread((packet,context)->packet.handle(context.get().getSender())).add();
            CHANNEL.messageBuilder(LegacyEffectPacket.class,32,NetworkDirection.PLAY_TO_CLIENT).encoder(LegacyEffectPacket::encode).decoder(LegacyEffectPacket::decode)
                .consumerMainThread((packet,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->tc4revival.client.LegacyAddonEffects.receive(packet,context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(InfusionPlacePacket.class,31,NetworkDirection.PLAY_TO_SERVER).encoder(InfusionPlacePacket::encode).decoder(InfusionPlacePacket::decode)
                .consumerMainThread((packet,context)->packet.handle(context.get().getSender())).add();
            CHANNEL.messageBuilder(JarLabelSelectPacket.class,30,NetworkDirection.PLAY_TO_SERVER).encoder(JarLabelSelectPacket::encode).decoder(JarLabelSelectPacket::decode)
                .consumerMainThread((packet,context)->packet.handle(context.get().getSender())).add();
            CHANNEL.messageBuilder(InventoryScanStatus.class,29,NetworkDirection.PLAY_TO_CLIENT).encoder(InventoryScanStatus::encode).decoder(InventoryScanStatus::decode)
                .consumerMainThread((packet,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->tc4revival.client.InventoryScanHover.status(packet,context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(BookRecipeRequest.class,28,NetworkDirection.PLAY_TO_SERVER).encoder(BookRecipeRequest::encode).decoder(BookRecipeRequest::decode)
                .consumerMainThread((packet,context)->packet.handle(context.get().getSender())).add();
            CHANNEL.messageBuilder(InfusionRiskRequest.class,26,NetworkDirection.PLAY_TO_SERVER).encoder(InfusionRiskRequest::encode).decoder(InfusionRiskRequest::decode)
                .consumerMainThread((packet,context)->packet.handle(context.get().getSender())).add();
            CHANNEL.messageBuilder(InfusionRiskReply.class,27,NetworkDirection.PLAY_TO_CLIENT).encoder(InfusionRiskReply::encode).decoder(InfusionRiskReply::decode)
                .consumerMainThread((packet,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->tc4revival.client.InfusionRiskOverlay.receive(packet,context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(BrowserRecipesRequest.class,24,NetworkDirection.PLAY_TO_SERVER).encoder(BrowserRecipesRequest::encode).decoder(BrowserRecipesRequest::decode)
                .consumerMainThread((packet,context)->packet.handle(context.get().getSender())).add();
            CHANNEL.messageBuilder(BrowserRecipesPacket.class,25,NetworkDirection.PLAY_TO_CLIENT).encoder(BrowserRecipesPacket::encode).decoder(BrowserRecipesPacket::decode)
                .consumerMainThread((packet,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->tc4revival.client.ClientBrowserRecipes.receive(packet,context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(InventoryScanPacket.class,23,NetworkDirection.PLAY_TO_SERVER).encoder(InventoryScanPacket::encode).decoder(InventoryScanPacket::decode)
                .consumerMainThread((packet,context)->packet.handle(context.get().getSender())).add();
            CHANNEL.messageBuilder(AddonDefinitionsPacket.class,22,NetworkDirection.PLAY_TO_CLIENT).encoder(AddonDefinitionsPacket::encode).decoder(AddonDefinitionsPacket::decode)
                .consumerMainThread((packet,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->tc4revival.client.ClientAddonDefinitions.receive(packet,context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(ElementalProspectingPacket.class,21,NetworkDirection.PLAY_TO_CLIENT).encoder(ElementalProspectingPacket::encode).decoder(ElementalProspectingPacket::decode)
                    .consumerMainThread((packet,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->tc4revival.client.ElementalPickScan.receive(packet,context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(InventoryAspectsRequest.class,19,NetworkDirection.PLAY_TO_SERVER).encoder(InventoryAspectsRequest::encode).decoder(InventoryAspectsRequest::decode)
                    .consumerMainThread((packet,context)->packet.handle(context.get().getSender())).add();
            CHANNEL.messageBuilder(InventoryAspectsReply.class,20,NetworkDirection.PLAY_TO_CLIENT).encoder(InventoryAspectsReply::encode).decoder(InventoryAspectsReply::decode)
                    .consumerMainThread((packet,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->tc4revival.client.InventoryAspectOverlay.receive(packet,context.get().getNetworkManager()))).add();

            CHANNEL.messageBuilder(GolemBellResetPacket.class,18,NetworkDirection.PLAY_TO_SERVER).encoder(GolemBellResetPacket::encode).decoder(GolemBellResetPacket::decode)
                    .consumerMainThread((packet,context)->packet.handle(context.get().getSender())).add();
            CHANNEL.messageBuilder(HoverTogglePacket.class,17,NetworkDirection.PLAY_TO_SERVER).encoder(HoverTogglePacket::encode).decoder(HoverTogglePacket::decode)
                    .consumerMainThread((packet,context)->tc4revival.content.HoverFlight.request(context.get().getSender())).add();
            CHANNEL.messageBuilder(EssentiaSourcePacket.class,16,NetworkDirection.PLAY_TO_CLIENT).encoder(EssentiaSourcePacket::encode).decoder(EssentiaSourcePacket::decode)
                    .consumerMainThread((packet,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->tc4revival.client.InfusionIntakeEffects.receive(packet,context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(RunicHitPacket.class,15,NetworkDirection.PLAY_TO_CLIENT).encoder(RunicHitPacket::encode).decoder(RunicHitPacket::decode)
                    .consumerMainThread((packet,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->tc4revival.client.RunicHitEffects.receive(packet,context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(RunicChargePacket.class,14,NetworkDirection.PLAY_TO_CLIENT).encoder(RunicChargePacket::encode).decoder(RunicChargePacket::decode)
                    .consumerMainThread((packet,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->tc4revival.client.ClientRunicShield.receive(packet,context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(WandAreaPacket.class, 13, NetworkDirection.PLAY_TO_SERVER)
                    .encoder(WandAreaPacket::encode).decoder(WandAreaPacket::decode)
                    .consumerMainThread((packet, context) -> packet.handle(context.get().getSender())).add();
            CHANNEL.messageBuilder(FocusEffectPacket.class, 12, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(FocusEffectPacket::encode).decoder(FocusEffectPacket::decode)
                    .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> tc4revival.client.ClientFocusEffects.receive(packet, context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(FocusChangePacket.class, 11, NetworkDirection.PLAY_TO_SERVER)
                    .encoder(FocusChangePacket::encode).decoder(FocusChangePacket::decode)
                    .consumerMainThread((packet, context) -> packet.handle(context.get().getSender())).add();
            CHANNEL.messageBuilder(BookActionPacket.class, 7, NetworkDirection.PLAY_TO_SERVER)
                    .encoder(BookActionPacket::encode).decoder(BookActionPacket::decode)
                    .consumerMainThread((packet, context) -> packet.handle(context.get().getSender())).add();
            CHANNEL.messageBuilder(BookOpenPacket.class, 8, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(BookOpenPacket::encode).decoder(BookOpenPacket::decode)
                    .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> tc4revival.client.ClientBookSession.open(packet, context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(BookResultPacket.class, 9, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(BookResultPacket::encode).decoder(BookResultPacket::decode)
                    .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> tc4revival.client.ClientBookSession.receive(packet, context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(ResearchTableActionPacket.class, 6, NetworkDirection.PLAY_TO_SERVER)
                    .encoder(ResearchTableActionPacket::encode).decoder(ResearchTableActionPacket::decode)
                    .consumerMainThread((packet, context) -> packet.handle(context.get().getSender())).add();
            CHANNEL.messageBuilder(KnowledgePacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(KnowledgePacket::encode).decoder(KnowledgePacket::decode)
                .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientKnowledge.receive(packet.frame(), context.get().getNetworkManager())))
                .add();
            CHANNEL.messageBuilder(ScanFeedbackPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(ScanFeedbackPacket::encode).decoder(ScanFeedbackPacket::decode)
                    .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> ClientScanFeedback.receive(packet, context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(LensPacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(LensPacket::encode).decoder(LensPacket::decode)
                    .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> ClientLens.receive(packet, context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(ScanEffectPacket.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(ScanEffectPacket::encode).decoder(ScanEffectPacket::decode)
                    .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> tc4revival.client.ClientScanEffects.receive(packet, context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(CluePacket.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(CluePacket::encode).decoder(CluePacket::decode)
                    .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> ClientScanFeedback.receiveClue(packet, context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(ResearchCompletionPacket.class, 5, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(ResearchCompletionPacket::encode).decoder(ResearchCompletionPacket::decode)
                    .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> tc4revival.client.ClientResearchFeedback.receive(packet, context.get().getNetworkManager()))).add();
            CHANNEL.messageBuilder(WarpGainPacket.class, 10, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(WarpGainPacket::encode).decoder(WarpGainPacket::decode)
                    .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> tc4revival.client.ClientResearchFeedback.receiveWarp(packet, context.get().getNetworkManager()))).add();
        });
    }
    public static void send(ServerPlayer player, KnowledgePacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    public static void send(ServerPlayer player, ScanFeedbackPacket packet) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet); }
    public static void send(ServerPlayer player, LensPacket packet) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet); }
    public static void send(ServerPlayer player, ScanEffectPacket packet) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet); }
    public static void send(ServerPlayer player, CluePacket packet) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet); }
    public static void send(ServerPlayer player, ResearchCompletionPacket packet) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet); }
    public static void send(ServerPlayer player, BookOpenPacket packet) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet); }
    public static void send(ServerPlayer player, BookResultPacket packet) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet); }
    public static void send(ServerPlayer player, WarpGainPacket packet) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet); }
    public static void send(ServerPlayer player, FocusEffectPacket packet) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet); }
}
