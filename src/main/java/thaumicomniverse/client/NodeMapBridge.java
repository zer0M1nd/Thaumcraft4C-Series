package thaumicomniverse.client;

/** Only the plugin discovered by the installed JourneyMap API supplies callbacks.
 * Client lifecycle handlers must never link either optional API's plugin class. */
public final class NodeMapBridge {
    private static Runnable refresh = () -> {};
    private static Runnable animate = () -> {};
    private static Runnable clear = () -> {};
    private NodeMapBridge() {}
    public static void install(Runnable refreshNodes, Runnable clearNodes) {
        install(refreshNodes, clearNodes, () -> {});
    }
    public static void install(Runnable refreshNodes, Runnable clearNodes, Runnable animateNodes) {
        animate = java.util.Objects.requireNonNull(animateNodes);
        refresh = java.util.Objects.requireNonNull(refreshNodes);
        clear = java.util.Objects.requireNonNull(clearNodes);
    }
    public static void toggleVisibility() {
        var config = thaumicomniverse.compat.nodemap.NodeMapConfig.SHOW;
        config.set(!config.get());
        thaumicomniverse.compat.nodemap.NodeMapConfig.SPEC.save();
        refresh();
    }
    public static void animate() { if(thaumicomniverse.RevivalFeatures.Group.NODE_MAP.enabled()) animate.run(); else clear.run(); }
    public static void refresh() { if(thaumicomniverse.RevivalFeatures.Group.NODE_MAP.enabled()) refresh.run(); else clear.run(); }
    public static void clear() { clear.run(); }
}
