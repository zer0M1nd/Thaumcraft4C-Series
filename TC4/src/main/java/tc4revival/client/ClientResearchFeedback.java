package tc4revival.client;

import java.util.LinkedHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.Connection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.TC4Revival;
import tc4revival.content.TCSounds;
import tc4revival.network.ResearchCompletionPacket;
import tc4revival.research.ResearchCatalogs;

@Mod.EventBusSubscriber(modid = TC4Revival.MOD_ID, value = Dist.CLIENT)
public  class ClientResearchFeedback {
    private static final LinkedHashSet<String> HIGHLIGHTED = new LinkedHashSet<>();
    private static int lastColumn, lastRow;
    private ClientResearchFeedback() {}
    public static java.util.Set<String> highlighted() { return java.util.Set.copyOf(HIGHLIGHTED); }
    public static int lastColumn() { return lastColumn; }
    public static int lastRow() { return lastRow; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { HIGHLIGHTED.clear(); }
    public static void receive(ResearchCompletionPacket packet, Connection source) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || mc.getConnection().getConnection() != source || !source.isConnected()) return;
        if (!packet.virtual()) {
            mc.getToasts().addToast(new ResearchCompletionToast(packet, source));
            lastColumn = packet.column(); lastRow = packet.row();
            HIGHLIGHTED.add(packet.key());
            ResearchCatalogs.bundled().find(packet.key()).ifPresent(entry -> HIGHLIGHTED.add(entry.category()));
        }
        if (packet.permanentWarp() > 0) warp("tc.addwarp");
        if (packet.stickyWarp() > 0) warp("tc.addwarpsticky");
    }
    /** Return to the world before displaying a sealed-research hint. */
    public static void sealedResearch() {
        var mc=Minecraft.getInstance();
        if(mc.player==null)return;
        mc.player.closeContainer();
        mc.setScreen(null);
        mc.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("thaumcraft.research.eldritch_sealed")
                .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE,net.minecraft.ChatFormatting.ITALIC),true);
    }
    private static void warp(String key) {
        ClientScanFeedback.notice(I18n.get(key));
        Minecraft.getInstance().player.playSound(TCSounds.WHISPERS.get(), 0.5f, 1);
    }
    public static void receiveWarp(tc4revival.network.WarpGainPacket packet, Connection source) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || mc.getConnection().getConnection() != source || !source.isConnected()) return;
        if (packet.amount() < 0) {
            // Legacy PacketWarpMessage only displays removal for sticky warp, without whispers.
            if (packet.kind() == tc4revival.research.PlayerKnowledge.WarpKind.STICKY)
                ClientScanFeedback.notice(I18n.get("tc.removewarpsticky"));
            return;
        }
        switch (packet.kind()) {
            case PERMANENT -> warp("tc.addwarp");
            case STICKY -> warp("tc.addwarpsticky");
            case TEMPORARY -> ClientScanFeedback.notice(I18n.get("tc.addwarptemp"));
        }
    }
}
