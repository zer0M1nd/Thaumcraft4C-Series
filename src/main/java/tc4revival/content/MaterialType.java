package tc4revival.content;

import java.util.List;

/** Passive ItemResource forms. Active resources remain separate implementations, not inert substitutes. */
public enum MaterialType {
    THAUMIUM(2, "thaumium_ingot", "thaumiumingot", "forge:ingots/thaumium"),
    QUICKSILVER(3, "quicksilver", "quicksilver", "forge:quicksilver"),
    TALLOW(4, "magic_tallow", "tallow"),
    DEFUNCT_BRAIN(5, "legacy_brain", "brain"),
    AMBER(6, "amber", "amber", "forge:gems/amber"),
    FABRIC(7, "enchanted_fabric", "cloth"),
    FILTER(8, "vis_filter", "filter"),
    MIRROR_GLASS(10, "mirrored_glass", "mirrorglass"),
    SALIS_MUNDUS(14, "salis_mundus", "dust"),
    VOID(16, "void_ingot", "voidingot", "forge:ingots/void"),
    VOID_SEED(17, "void_seed", "voidseed"),
    GOLD_COIN(18, "gold_coin", "coin", "forge:nuggets/gold");
    private final int metadata;
    private final String path, texture;
    private final List<String> tags;
    MaterialType(int metadata, String path, String texture, String... tags) {
        this.metadata = metadata; this.path = path; this.texture = texture; this.tags = List.of(tags);
    }
    public int metadata() { return metadata; }
    public String path() { return path; }
    public String texture() { return texture; }
    public List<String> tags() { return tags; }
    public boolean creativeVisible() { return this != DEFUNCT_BRAIN; }
}
