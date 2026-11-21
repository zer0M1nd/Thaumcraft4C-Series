package tc4revival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.network.AddonDefinitionsPacket;
import tc4revival.research.ResearchDefinitions;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class ClientAddonDefinitions {
    private static com.google.gson.JsonObject previousResearch;
    public static void receive(AddonDefinitionsPacket packet,Connection source) {
        var minecraft=Minecraft.getInstance();
        if(minecraft.getConnection()==null || minecraft.getConnection().getConnection()!=source)return;
        try {
            if(!packet.fingerprint().equals(AddonDefinitionsPacket.fingerprintNow()))
                throw new IllegalArgumentException("Client and server have different aspect/wand/upgrade definitions. Install matching addon versions.");
            ResearchDefinitions.accept(packet.research());
            if(!packet.research().equals(previousResearch)){
                previousResearch=packet.research().deepCopy();ClientBrowserRecipes.dataVersion++;
            }
            // A live book holds navigation indices into its old snapshot.
            if(minecraft.screen instanceof ResearchBrowserScreen || minecraft.screen instanceof ResearchReaderScreen) {
                ClientBookSession.close();minecraft.setScreen(null);
            }
        } catch(RuntimeException error) {
            tc4revival.TC4Revival.LOGGER.error("Unable to synchronize addon definitions",error);
            source.disconnect(Component.literal("Thaumcraft addon definition mismatch: "+error.getMessage()));
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { previousResearch=null;ResearchDefinitions.clearClient(); thaumcraft.api.wands.WandAttributes.clearClient(); }
}
