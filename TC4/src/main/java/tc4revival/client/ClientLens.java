package tc4revival.client;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.TC4Revival;
import tc4revival.network.LensPacket;

@Mod.EventBusSubscriber(modid = TC4Revival.MOD_ID, value = Dist.CLIENT)
public  class ClientLens {
    private static LensPacket packet;
    private ClientLens() {}
    public static void receive(LensPacket incoming, Connection source) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || mc.getConnection().getConnection() != source || !source.isConnected()) return;
        packet = incoming;
    }
    public static Optional<LensPacket> display() {
        var level = Minecraft.getInstance().level;
        return thaumcraft.api.research.ScannerViews.hint(Minecraft.getInstance().player,level != null && packet != null && level.dimension().location().equals(packet.dimension()) ? Optional.of(packet) : Optional.empty());
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { packet = null; }
}
