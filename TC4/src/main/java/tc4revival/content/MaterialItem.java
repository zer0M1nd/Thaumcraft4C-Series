package tc4revival.content;

import net.minecraft.world.item.Item;

/** These specific old resource forms have no use/tick action; their stored aspects remain functional. */
public  class MaterialItem extends Item implements StoredAspectItem {
    private final MaterialType type;
    public MaterialItem(MaterialType type) { super(new Properties().stacksTo(64)); this.type = type; }
    public MaterialType type() { return type; }
}
