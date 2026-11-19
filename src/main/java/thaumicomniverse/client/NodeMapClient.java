package thaumicomniverse.client;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import thaumicomniverse.compat.nodemap.*;

@Mod.EventBusSubscriber(modid = "thaumicomniverse", value = Dist.CLIENT)
public final class NodeMapClient {
    public static final Map<String, CompoundTag> NODES = new LinkedHashMap<>();
    public static KeyMapping TOGGLE;
    private static int ticks;
    public static void receive(NodeMapNetwork.Snapshot packet) {
        if (packet.reset()) NODES.clear();
        for (var element : packet.rows()) {
            var row = ((CompoundTag) element).copy(); row.putLong("received", System.currentTimeMillis());
            NODES.put(row.getString("key"), row);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ModList.get().isLoaded("journeymap")) return;
        while (TOGGLE.consumeClick()) {
            NodeMapBridge.toggleVisibility();
        }
        if (Minecraft.getInstance().level != null) {
            if (++ticks % 20 == 0) NodeMapBridge.refresh();
            else if (ticks % 2 == 0) NodeMapBridge.animate();
        }
    }
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        NODES.clear(); NodeMapBridge.clear();
    }
    @Mod.EventBusSubscriber(modid = "thaumicomniverse", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        @SubscribeEvent public static void reload(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) resources -> NodeMapAppearance.invalidate());
        }
        @SubscribeEvent public static void register(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
            if (ModList.get().isLoaded("journeymap")) event.register(TOGGLE = new KeyMapping("key.thaumicomniverse.node_map", org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN, "key.categories.thaumcraft"));
        }
    }
}
