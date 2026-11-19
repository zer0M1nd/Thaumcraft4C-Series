package thaumicomniverse.client;

import java.util.*;
import journeymap.client.api.event.forge.FullscreenDisplayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import journeymap.client.api.*;
import journeymap.client.api.display.*;
import journeymap.client.api.event.ClientEvent;
import journeymap.client.api.model.MapImage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import thaumicomniverse.compat.nodemap.*;

@ClientPlugin
public final class JourneyMapNodes implements IClientPlugin {
    private static IClientAPI api;
    private static final Map<String, MarkerOverlay> SHOWN = new HashMap<>();
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static boolean reported;
    private static final Map<String, NodeMapAppearance.Layer> STYLES = new HashMap<>();
    @Override public String getModId() { return "thaumicomniverse"; }
    @Override public void initialize(IClientAPI clientApi) {
        api = clientApi;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, FullscreenDisplayEvent.AddonButtonDisplayEvent.class, this::onButtons);
        NodeMapBridge.install(JourneyMapNodes::refresh, JourneyMapNodes::clear, JourneyMapNodes::animate);
        api.subscribe(getModId(), EnumSet.of(ClientEvent.Type.MAPPING_STARTED, ClientEvent.Type.MAPPING_STOPPED));
        LOGGER.info("Omniverse aura node map: JourneyMap legacy API initialized");
    }
    @Override public void onEvent(ClientEvent event) {
        Minecraft.getInstance().execute(() -> { if (event.type == ClientEvent.Type.MAPPING_STOPPED) clear(); else refresh(); });
    }
    public static void clear() { if (api != null) SHOWN.values().forEach(api::remove); SHOWN.clear(); STYLES.clear(); NodeMapTooltip.clear(); }
    public static void refresh() {
        if (api == null) return;
        if (!NodeMapConfig.SHOW.get() || Minecraft.getInstance().level == null) { clear(); return; }
        var stale = new HashSet<>(SHOWN.keySet());
        for (var entry : NodeMapClient.NODES.entrySet()) {
            var row = entry.getValue(); var dimension = ResourceLocation.tryParse(row.getString("dimension"));
            if (dimension == null) continue;
            boolean offline = !row.getBoolean("loaded");
            long now = row.getLong("serverNow") + Math.max(0, System.currentTimeMillis() - row.getLong("received"));
            var projection = NodeRecoveryProjection.project(row.getCompound(offline ? "state" : "live"), row.getInt("lock"), now, offline);
            String total = (offline ? "~" : "") + projection.total() + "/" + projection.capacity();
            var title = new StringBuilder(Component.translatable(offline ? "omniverse.node_map.offline" : "omniverse.node_map.live").getString());
            title.append("\n").append(total);
            projection.aspects().forEach((aspect, range) -> title.append("\n").append(thaumcraft.api.aspects.AspectViews.display(aspect).displayName()).append(": ")
                    .append(range.minimum() == range.maximum() ? Integer.toString(range.minimum()) : range.minimum() + "–" + range.maximum()).append('/').append(range.capacity()));
            if (offline) title.append("\n").append(Component.translatable("omniverse.node_map.forecast").getString());
            var layers = NodeMapAppearance.layers(row.getCompound(offline ? "state" : "live"));
            for (int i=0; i<layers.size(); i++) {
                String key = entry.getKey() + "/layer/" + i;
                var marker = SHOWN.get(key);
                if (marker == null) {
                    marker = createMarker(key, BlockPos.of(row.getLong("position")), NodeMapAppearance.TEXTURE);
                    marker.setDimension(ResourceKey.create(Registries.DIMENSION, dimension));
                    marker.setActiveUIs(EnumSet.of(Context.UI.Minimap, Context.UI.Fullscreen));
                    marker.setOverlayGroupName(Component.translatable("omniverse.node_map.group").getString());
                    marker.setDisplayOrder(100 + i);
                    SHOWN.put(key, marker);
                }
                stale.remove(key); STYLES.put(key, layers.get(i));
                marker.setTitle(null); // Custom tooltip avoids JourneyMap's doubled multiline scale.
                marker.setLabel(i == layers.size()-1 ? total : null);
                marker.getTextProperties().setOffsetY(-18);
                marker.setOverlayListener(tooltip(key, title.toString()));
                NodeMapTooltip.update(key, title.toString());
                marker.setIcon(sprite(layers.get(i), System.currentTimeMillis()));
                marker.flagForRerender();
                try { api.show(marker); } catch (Exception exception) {
                    if (!reported) { LOGGER.warn("Unable to show scanned aura nodes on JourneyMap", exception); reported = true; }
                }
            }
        }
        for (var key : stale) { api.remove(SHOWN.remove(key)); STYLES.remove(key); NodeMapTooltip.leave(key); }
    }
    public static void animate() {
        long now = System.currentTimeMillis();
        SHOWN.forEach((key, marker) -> marker.setIcon(sprite(STYLES.get(key), now)));
    }
    public static MapImage sprite(NodeMapAppearance.Layer layer, long now) {
        int tile = NodeMapAppearance.tileSize();
        return new MapImage(NodeMapAppearance.TEXTURE, NodeMapAppearance.frame(now)*tile, layer.row()*tile,
                tile, tile, layer.color(), layer.alpha(now))
                .setDisplayWidth(layer.size()).setDisplayHeight(layer.size()).centerAnchors();
    }
    private static IOverlayListener tooltip(String key, String text) {
        return new IOverlayListener() {
            public void onActivate(journeymap.client.api.util.UIState state) {}
            public void onDeactivate(journeymap.client.api.util.UIState state) { NodeMapTooltip.leave(key); }
            public void onMouseMove(journeymap.client.api.util.UIState state, java.awt.geom.Point2D.Double point, BlockPos pos) { NodeMapTooltip.hover(key,text); }
            public void onMouseOut(journeymap.client.api.util.UIState state, java.awt.geom.Point2D.Double point, BlockPos pos) { NodeMapTooltip.leave(key); }
            public boolean onMouseClick(journeymap.client.api.util.UIState state, java.awt.geom.Point2D.Double point, BlockPos pos, int button, boolean twice) { return false; }
            public void onOverlayMenuPopup(journeymap.client.api.util.UIState state, java.awt.geom.Point2D.Double point, BlockPos pos, journeymap.client.api.display.ModPopupMenu menu) {}
        };
    }
    public void onButtons(FullscreenDisplayEvent.AddonButtonDisplayEvent event) {
        addButtons(event.getThemeButtonDisplay());
    }
    void addButtons(ThemeButtonDisplay display) {
        // Register even with no discoveries, and while the layer is disabled.
        display.addThemeToggleButton("omniverse.node_map.toggle",
                "thaumicomniverse_nodes", NodeMapConfig.SHOW.get(), button -> {
                    NodeMapBridge.toggleVisibility();
                    button.setToggled(NodeMapConfig.SHOW.get());
                });
    }
    public static MarkerOverlay createMarker(String key, BlockPos position, ResourceLocation texture) {
        // MapImage does not initialize its nullable anchors in the constructor.
        var icon = new MapImage(texture, 16, 16).centerAnchors();
        return new MarkerOverlay("thaumicomniverse", "node/" + key, position, icon);
    }
}
