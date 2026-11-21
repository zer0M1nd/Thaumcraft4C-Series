package thaumicomniverse.compat.backpack;

import java.util.function.ToIntBiFunction;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;
import tc4revival.content.CastingWandItem;

public final class BackpackVisCharging {
    /** Amounts are centivis. The caller owns a shared budget across every target. */
    public static boolean charge(IStorageWrapper storage, ToIntBiFunction<Aspect, Integer> supply, int limitPerWand) {
        if(!thaumicomniverse.RevivalFeatures.Group.BACKPACKS.enabled())return false;
        boolean changed = false;
        for (var upgrade : storage.getUpgradeHandler().getTypeWrappers(ArcaneCraftingUpgradeItem.TYPE))
            if (upgrade.isEnabled()) changed |= charge(upgrade.inventory(), 9, supply, limitPerWand);
        var inventory = storage.getInventoryHandler();
        for (int slot = 0; slot < inventory.getSlots(); slot++) changed |= charge(inventory, slot, supply, limitPerWand);
        return changed;
    }
    private static boolean charge(IItemHandlerModifiable inventory, int slot, ToIntBiFunction<Aspect, Integer> supply, int limit) {
        var stack = inventory.getStackInSlot(slot).copy();
        boolean changed = chargeInPlace(stack, supply, limit);
        if (changed) inventory.setStackInSlot(slot, stack);
        return changed;
    }
    private static boolean chargeInPlace(net.minecraft.world.item.ItemStack stack, ToIntBiFunction<Aspect, Integer> supply, int limit) {
        if (stack.getCount() != 1 || !(stack.getItem() instanceof CastingWandItem wand)) return false;
        if(!thaumicomniverse.RevivalFeatures.Group.BACKPACKS.enabled())return false;
        boolean changed = false;
        for (var aspect : TCAspects.catalog().primals()) {
            int request = wand.receiveVis(stack, aspect, limit, true);
            if (request <= 0) continue;
            int received = supply.applyAsInt(aspect, request);
            if (received > 0) { wand.receiveVis(stack, aspect, received, false); changed = true; }
        }
        return changed;
    }
    /** Includes main inventory, hotbar, armour and offhand, with the same node budget. */
    public static void chargePlayer(net.minecraft.world.entity.player.Player player,ToIntBiFunction<Aspect,Integer> supply) {
        var items=new net.minecraftforge.items.wrapper.PlayerInvWrapper(player.getInventory());
        boolean changed=false;
        // Active item use and focus sessions retain the player's ItemStack identity.
        // Replacing it with a charged copy interrupts channeled spells every tick.
        for(int slot=0;slot<items.getSlots();slot++)changed|=chargeInPlace(items.getStackInSlot(slot),supply,Integer.MAX_VALUE);
        if(changed){player.getInventory().setChanged();player.containerMenu.broadcastChanges();}
    }
}
