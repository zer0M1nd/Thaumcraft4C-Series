package tc4revival.content;

/** Base-mod metal subdivisions; vanilla iron nuggets are reused separately. */
public enum NuggetType {
    COPPER(1, "copper_nugget", "nuggetcopper", "copper_ingot", () -> net.minecraft.world.item.Items.COPPER_INGOT, "forge:nuggets/copper"),
    QUICKSILVER(5, "quicksilver_drop", "nuggetquicksilver", MaterialType.QUICKSILVER, "forge:nuggets/quicksilver"),
    THAUMIUM(6, "thaumium_nugget", "nuggetthaumium", MaterialType.THAUMIUM, "forge:nuggets/thaumium"),
    VOID(7, "void_nugget", "nuggetvoid", MaterialType.VOID, "forge:nuggets/void");

    private final int metadata;
    private final String path, texture, tag;
    private final String materialPath;
    private final java.util.function.Supplier<net.minecraft.world.item.Item> material;
    NuggetType(int metadata, String path, String texture, MaterialType material, String tag) {
        this(metadata, path, texture, material.path(), () -> TCItems.material(material), tag);
    }
    NuggetType(int metadata, String path, String texture, String materialPath,
               java.util.function.Supplier<net.minecraft.world.item.Item> material, String tag) {
        this.metadata = metadata; this.path = path; this.texture = texture; this.materialPath = materialPath; this.material = material; this.tag = tag;
    }
    public int metadata() { return metadata; }
    public String path() { return path; }
    public String texture() { return texture; }
    public net.minecraft.world.item.Item materialItem() { return material.get(); }
    public String materialPath() { return materialPath; }
    public java.util.Optional<String> tag() { return java.util.Optional.ofNullable(tag); }
}
