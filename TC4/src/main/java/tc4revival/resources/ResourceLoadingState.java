package tc4revival.resources;

import java.util.*;

/** Failures survive initial vanilla resource loading, so they can be shown by a real Minecraft Screen. */
public final class ResourceLoadingState {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    private static final Set<String> failures=new LinkedHashSet<>(),warnings=new LinkedHashSet<>();
    public static synchronized void clear() { failures.clear();warnings.clear(); }
    public static synchronized void failure(String text) { failures.add(text); }
    public static synchronized void warning(String text) { if(warnings.add(text))LOGGER.warn("{}",text); }
    public static synchronized List<String> warnings() { return List.copyOf(warnings); }
    public static synchronized boolean missing() { return !failures.isEmpty(); }
    public static synchronized boolean needsScreen() { return missing(); }
    /** Kept for callers compiled against older versions; warnings no longer require acceptance. */
    @Deprecated public static void acceptWarnings() {}
    public static synchronized String details() { return String.join("\n\n",failures); }
    private ResourceLoadingState() {}
}
