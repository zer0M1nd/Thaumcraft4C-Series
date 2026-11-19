package thaumicomniverse.compat.backpack;

import net.minecraft.world.inventory.Slot;

public final class SlotPosition {
    // Sophisticated Core makes these fields mutable. Resolve SRG names through Forge for both environments.
    private static final java.lang.reflect.Field X = net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(Slot.class, "f_40220_");
    private static final java.lang.reflect.Field Y = net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(Slot.class, "f_40221_");
    public static void move(Slot slot, int x, int y) {
        try { X.setInt(slot, x); Y.setInt(slot, y); }
        catch (IllegalAccessException exception) { throw new IllegalStateException("Cannot position backpack crafting slots", exception); }
    }
}
