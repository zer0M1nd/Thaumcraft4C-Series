package thaumicomniverse.compat.backpack;

import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import tc4revival.content.AspectOrbEntity;
import tc4revival.content.CastingWandItem;

/** Vis orbs charge wands; they must not be converted into essentia or liquid XP. */
public final class BackpackOrbCollection {
    public static boolean collect(IStorageWrapper storage, AspectOrbEntity orb) {
        if(!thaumicomniverse.RevivalFeatures.Group.BACKPACKS.enabled())return false;
        if (!orb.isAlive() || !orb.aspect().isPrimal() || orb.value() <= 0) return false;
        long total = (long) orb.value() * 100;
        if (total > Integer.MAX_VALUE) return false;
        var inventory = storage.getInventoryHandler();
        int remaining = (int) total;
        // Do not partially consume an indivisible native orb.
        for (int slot = 0; slot < inventory.getSlots() && remaining > 0; slot++) {
            var stack = inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof CastingWandItem wand)
                remaining -= wand.receiveVis(stack, orb.aspect(), remaining, true);
        }
        if (remaining != 0) return false;
        remaining = (int) total;
        for (int slot = 0; slot < inventory.getSlots() && remaining > 0; slot++) {
            var stack = inventory.getStackInSlot(slot).copy();
            if (stack.getItem() instanceof CastingWandItem wand) {
                int accepted = wand.receiveVis(stack, orb.aspect(), remaining, false);
                if (accepted > 0) { inventory.setStackInSlot(slot, stack); remaining -= accepted; }
            }
        }
        orb.discard();
        return true;
    }
}
