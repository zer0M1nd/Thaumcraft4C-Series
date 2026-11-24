package tc4revival.compat.jei;

/** The active JEI filter owns its index. A world/runtime change releases the old callback. */
public final class JeiAspectRefresh {
    private static Runnable refresh;

    public static void watch(Runnable update) {
        refresh = update;
    }

    public static void tick() {
        if (refresh != null) refresh.run();
    }

    public static void clear() { refresh = null; }
    private JeiAspectRefresh() {}
}
