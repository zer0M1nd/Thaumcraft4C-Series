package thaumicfutures.devices;

/** TC5 preset order, indexed from the top left of the visible crafting grid. */
public final class CraftingPatterns {
    private static final int[] PRESETS={511,1,3,9,27,7,73,63,219,495};
    public static int count() { return PRESETS.length; }
    public static int preset(int index) { return PRESETS[Math.floorMod(index,PRESETS.length)]; }
    public static int set(int mask,int slot,boolean enabled) {
        if(slot<0 || slot>=9)throw new IllegalArgumentException("Crafting slot outside 0..8");
        return (enabled?mask|1<<slot:mask&~(1<<slot))&511;
    }
    public static boolean enabled(int mask,int slot) { return (mask&(1<<slot))!=0; }
    private CraftingPatterns() {}
}
