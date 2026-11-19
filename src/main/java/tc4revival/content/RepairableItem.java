package tc4revival.content;

/** Opt-in for the old Repair enchantment; ordinary anvil repairability is a different rule. */
public interface RepairableItem {
    default boolean allowHotbarRepair() { return true; }
    default boolean beforeVisRepair(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.player.Player player, int level) { return true; }
}
