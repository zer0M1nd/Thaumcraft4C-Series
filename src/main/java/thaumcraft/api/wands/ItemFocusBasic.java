package thaumcraft.api.wands;

/** Extend this class, register the item with Forge, then supply casting and upgrade choices. */
public abstract class ItemFocusBasic extends tc4revival.content.WandFocusItem {
    public int getUpgradeLevel(net.minecraft.world.item.ItemStack focus,String key) { return upgradeLevel(focus,tc4revival.content.FocusUpgrade.require(key).id); }
}
