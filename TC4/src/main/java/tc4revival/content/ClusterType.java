package tc4revival.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.function.Supplier;

/** Native base-game ore clusters from old ItemNugget; optional integration metals follow separately. */
public enum ClusterType {
    IRON(16, "iron", () -> Items.IRON_INGOT),
    COPPER(17, "copper", () -> Items.COPPER_INGOT),
    CINNABAR(21, "cinnabar", () -> TCItems.material(MaterialType.QUICKSILVER)),
    GOLD(31, "gold", () -> Items.GOLD_INGOT);
    private final int metadata;
    private final String material;
    private final Supplier<Item> output;
    ClusterType(int metadata, String material, Supplier<Item> output) {
        this.metadata = metadata; this.material = material; this.output = output;
    }
    public int metadata() { return metadata; }
    public String path() { return material + "_cluster"; }
    public String texture() { return "cluster" + material; }
    public String tag() { return "forge:clusters/" + material; }
    public Item output() { return output.get(); }
}
