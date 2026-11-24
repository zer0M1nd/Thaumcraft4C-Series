package temodern.compat;

import appeng.api.upgrades.Upgrades;
import appeng.items.tools.powered.WirelessTerminalItem;
import de.mari_023.ae2wtlib.AE2wtlib;
import de.mari_023.ae2wtlib.terminal.IUniversalWirelessTerminalItem;
import de.mari_023.ae2wtlib.wut.WUTHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import temodern.TE;
import temodern.crafting.WorkMenu;
import temodern.item.WirelessArcaneTerminalItem;

/** Loaded only when AE2WTLib is installed. */
public class WTLibTerminal extends WirelessArcaneTerminalItem implements IUniversalWirelessTerminalItem {
    public static final String TERMINAL = "arcane";
    public static WirelessArcaneTerminalItem create() { return new WTLibTerminal(); }
    public static void register() {
        var item = (WTLibTerminal) TE.MATERIALS.get("wireless_arcane_terminal").get();
        WUTHandler.addTerminal(TERMINAL, item::tryOpen, UniversalArcaneHost::new, WorkMenu.TERMINAL.get(),
                item, item.getDescriptionId());
    }
    public static void registerUpgrade() {
        Upgrades.add(TE.MATERIALS.get("vis_charging_card").get(), AE2wtlib.UNIVERSAL_TERMINAL, 1);
    }
    @Override public MenuType<?> getMenuType(ItemStack stack) { return getMenuType(); }
    @Override public appeng.api.networking.IGrid getLinkedGrid(ItemStack stack, net.minecraft.world.level.Level level, Player player) {
        if (stack.getItem() != this && stack.getItem() instanceof WirelessTerminalItem carrier)
            return carrier.getLinkedGrid(stack, level, player);
        return super.getLinkedGrid(stack, level, player);
    }
}
