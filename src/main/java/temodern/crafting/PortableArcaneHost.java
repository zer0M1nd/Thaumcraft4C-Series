package temodern.crafting;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.items.tools.powered.WirelessTerminalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import temodern.RevivalFeatures;
import temodern.TE;
import temodern.machine.NetworkMachine;
import temodern.machine.VisInterfacePart;

public interface PortableArcaneHost extends WorkHost {
    ItemStack getItemStack();
    Player getPlayer();
    IUpgradeInventory getUpgrades();
    InternalInventory singularity();

    default Level workLevel() { return getPlayer().level(); }
    default BlockPos workPosition() { return getPlayer().blockPosition(); }
    default String kind() { return "arcane_terminal"; }
    default IManagedGridNode node() { return null; }
    default IActionSource action() { return IActionSource.ofPlayer(getPlayer()); }
    default boolean consumeNetworkVis(java.util.Map<tc4revival.aspect.Aspect,Integer> costs, boolean simulate) { return false; }
    default boolean active() {
        var node = getActionableNode();
        return RevivalFeatures.wireless() && node != null && node.isActive() && (getPlayer().isCreative()
                || getItemStack().getItem() instanceof WirelessTerminalItem item && item.hasPower(getPlayer(), .5, getItemStack()));
    }
    default boolean inventoryWands() { return getItemStack().getOrCreateTag().getBoolean("teInventoryWands"); }
    default void inventoryWands(boolean enabled) {
        getItemStack().getOrCreateTag().putBoolean("teInventoryWands", enabled);
        markWorkChanged();
    }
    default void charge() {
        if (!RevivalFeatures.charging() || !active() || !getUpgrades().isInstalled(TE.MATERIALS.get("vis_charging_card").get())) return;
        var grid = grid();
        if (grid == null) return;
        for (var source : grid.getActiveMachines(VisInterfacePart.class)) if (source.visInput()) workstation().chargeWand(source);
        for (var source : grid.getActiveMachines(NetworkMachine.class)) if (source.visInput()) workstation().chargeWand(source);
    }
}
