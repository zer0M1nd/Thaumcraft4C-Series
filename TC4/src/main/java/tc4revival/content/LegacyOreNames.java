package tc4revival.content;

/** Shared registry paths, usable by resource conversion without initializing Minecraft blocks. */
public  class LegacyOreNames {
    private LegacyOreNames() {}
    public static String path(int metadata) {
        return metadata == 0 ? "cinnabar_ore" : metadata == 7 ? "amber_ore"
                : ShardType.values()[metadata - 1].material() + "_infused_stone";
    }
}
