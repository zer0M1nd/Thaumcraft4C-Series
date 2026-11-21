package temodern.crafting;

import appeng.api.features.Locatables;
import appeng.api.networking.IGridNode;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.core.definitions.AEItems;
import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/** Optional WTLib card support through AE2's public quantum bridge registry. */
public final class WirelessQuantumLink {
    private static final ResourceLocation CARD = ResourceLocation.parse("ae2wtlib:quantum_bridge_card");
    private WirelessQuantumLink() {}
    public static Item card() {
        return ForgeRegistries.ITEMS.containsKey(CARD) ? ForgeRegistries.ITEMS.getValue(CARD) : null;
    }
    public static boolean installed(IUpgradeInventory upgrades) {
        var card = card();
        return card != null && upgrades.isInstalled(card);
    }
    public static long frequency(ItemStack terminal) {
        var inventory = new AppEngInternalInventory(1);
        if (terminal.hasTag()) inventory.readFromNBT(terminal.getTag(), "singularity");
        var singularity = inventory.getStackInSlot(0);
        return singularity.is(AEItems.QUANTUM_ENTANGLED_SINGULARITY.asItem()) && singularity.hasTag()
                ? singularity.getTag().getLong("freq") : 0;
    }
    public static IGridNode node(ItemStack terminal, Level level, IUpgradeInventory upgrades) {
        if (level.isClientSide || !installed(upgrades)) return null;
        long frequency = frequency(terminal);
        if (frequency == 0) return null;
        var bridge = Locatables.quantumNetworkBridges().get(level, frequency);
        if (bridge == null) bridge = Locatables.quantumNetworkBridges().get(level, -frequency);
        var node = bridge == null ? null : bridge.getActionableNode();
        return node != null && node.isActive() && node.getGrid().getEnergyService().isNetworkPowered() ? node : null;
    }
}
