package thaumcraft.api.devices;

/** Server-side heat transfer. Returns consumed energy; simulation must not change the device. */
public interface ExternalHeatReceiver {
    int receiveHeat(int available, int energyPerTick, boolean powered, boolean simulate);

    /** Bounded furnace transfer arithmetic shared by implementations and their simulators. */
    static int heatTicks(int burn, int available, int energyPerTick, boolean ready) {
        if (!ready || burn >= 200 || available <= 0) return 0;
        return Math.min(Math.min(4, 200 - Math.max(0, burn)), available / Math.max(1, energyPerTick));
    }
}
