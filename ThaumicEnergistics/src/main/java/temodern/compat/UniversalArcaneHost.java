package temodern.compat;

import java.util.function.BiConsumer;
import appeng.api.inventories.InternalInventory;
import appeng.menu.ISubMenu;
import appeng.menu.locator.MenuLocator;
import appeng.menu.locator.MenuLocators;
import appeng.parts.reporting.CraftingTerminalPart;
import de.mari_023.ae2wtlib.Platform;
import de.mari_023.ae2wtlib.terminal.WTMenuHost;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import temodern.TE;
import temodern.crafting.PortableArcaneHost;
import temodern.crafting.Workstation;

public class UniversalArcaneHost extends WTMenuHost implements PortableArcaneHost {
    private final Workstation work;
    private boolean ready;

    public UniversalArcaneHost(Player player, Integer slot, ItemStack stack, BiConsumer<Player, ISubMenu> reopen) {
        super(player, slot, stack, reopen);
        readFromNbt();
        work = new Workstation(this);
        work.load(stack.getOrCreateTag());
        ready = true;
    }

    public InternalInventory singularity() { return super.getSubInventory(INV_SINGULARITY); }
    @Override public InternalInventory getSubInventory(ResourceLocation id) {
        return CraftingTerminalPart.INV_CRAFTING.equals(id) ? work.craftingMatrix : super.getSubInventory(id);
    }
    @Override public Workstation workstation() { return work; }
    @Override public void markWorkChanged() {
        if (ready && !isClientSide()) {
            work.save(getItemStack().getOrCreateTag());
            getPlayer().getInventory().setChanged();
        }
    }
    @Override public boolean removed() { return !stillValid() || !isClientSide() && !active(); }
    @Override public MenuLocator locator() {
        return getSlot() == null ? Platform.findTerminalFromAccessory(getPlayer(), WTLibTerminal.TERMINAL) : MenuLocators.forInventorySlot(getSlot());
    }
    @Override public boolean onBroadcastChanges(AbstractContainerMenu menu) {
        if (!super.onBroadcastChanges(menu) || !stillValid()) return false;
        if (!isClientSide() && workLevel().getGameTime() % 5 == 0) charge();
        return true;
    }
    @Override public ItemStack getMainMenuIcon() { return new ItemStack(TE.MATERIALS.get("wireless_arcane_terminal").get()); }
}
