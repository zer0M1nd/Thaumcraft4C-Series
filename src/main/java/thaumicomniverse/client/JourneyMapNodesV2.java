package thaumicomniverse.client;

import java.util.*;
import journeymap.api.v2.client.event.FullscreenDisplayEvent;
import journeymap.api.v2.common.event.FullscreenEventRegistry;
import journeymap.api.v2.client.*;
import journeymap.api.v2.common.Context;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.ClientEventRegistry;
import journeymap.api.v2.client.display.*;
import journeymap.api.v2.client.event.MappingEvent;
import journeymap.api.v2.client.model.MapImage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import thaumicomniverse.compat.nodemap.*;

@JourneyMapPlugin(apiVersion = "2.0.0")
public final class JourneyMapNodesV2 implements IClientPlugin {
    private static IClientAPI api;
    private static final Map<String, MarkerOverlay> SHOWN = new HashMap<>();
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static boolean reported;
    private static final Map<String, NodeMapAppearance.Layer> STYLES = new HashMap<>();
    @Override public String getModId() { return "thaumicomniverse"; }
    @Override public void initialize(IClientAPI clientApi) {
        api = clientApi;
        FullscreenEventRegistry.ADDON_BUTTON_DISPLAY_EVENT.subscribe(getModId(), this::onButtons);
        NodeMapBridge.install(JourneyMapNodesV2::refresh, JourneyMapNodesV2::clear, JourneyMapNodesV2::animate);
        ClientEventRegistry.MAPPING_EVENT.subscribe(getModId(), this::onEvent);
        LOGGER.info("Omniverse aura node map: JourneyMap v2 API initialized");
    }
    public void onEvent(MappingEvent event) {
        Minecraft.getInstance().execute(() -> { if (event.getStage() == MappingEvent.Stage.MAPPING_STOPPED) clear(); else refresh(); });
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
                    marker.setActiveUIs(Context.UI.Minimap, Context.UI.Fullscreen);
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
            public void onActivate(journeymap.api.v2.client.util.UIState state) {}
            public void onDeactivate(journeymap.api.v2.client.util.UIState state) { NodeMapTooltip.leave(key); }
            public void onMouseMove(journeymap.api.v2.client.util.UIState state, java.awt.geom.Point2D.Double point, BlockPos pos) { NodeMapTooltip.hover(key,text); }
            public void onMouseOut(journeymap.api.v2.client.util.UIState state, java.awt.geom.Point2D.Double point, BlockPos pos) { NodeMapTooltip.leave(key); }
            public boolean onMouseClick(journeymap.api.v2.client.util.UIState state, java.awt.geom.Point2D.Double point, BlockPos pos, int button, boolean twice) { return false; }
            public void onOverlayMenuPopup(journeymap.api.v2.client.util.UIState state, java.awt.geom.Point2D.Double point, BlockPos pos, journeymap.api.v2.client.fullscreen.ModPopupMenu menu) {}
        };
    }
    public void onButtons(FullscreenDisplayEvent.AddonButtonDisplayEvent event) {
        addButtons(event.getThemeButtonDisplay());
    }
    void addButtons(journeymap.api.v2.client.fullscreen.ThemeButtonDisplay display) {
        // Register even with no discoveries, and while the layer is disabled.
        display.addThemeToggleButton("omniverse.node_map.toggle",
                ResourceLocation.parse("journeymap:theme/flat/icon/thaumicomniverse_nodes.png"), NodeMapConfig.SHOW.get(), button -> {
                    NodeMapBridge.toggleVisibility();
                    button.setToggled(NodeMapConfig.SHOW.get());
                });
    }
    public static MarkerOverlay createMarker(String key, BlockPos position, ResourceLocation texture) {
        // MapImage does not initialize its nullable anchors in the constructor.
        var icon = new MapImage(texture, 16, 16).centerAnchors();
        return new MarkerOverlay("thaumicomniverse", position, icon);
    }
}
