package tc4revival.content;

import net.minecraft.world.item.ItemStack;

/** Explicit counterpart of TC4's IRunicArmor; ordinary new-version armor is not implicitly eligible. */
public interface RunicGear {
    default boolean runicEnabled(ItemStack stack) { return true; }
    default int intrinsicRunicCharge(ItemStack stack) { return 0; }
    static int hardening(ItemStack stack) { return stack.getItem() instanceof RunicGear gear && gear.runicEnabled(stack) && stack.hasTag() ? stack.getTag().getByte("RS.HARDEN") : 0; }
    static int charge(ItemStack stack) { return stack.getItem() instanceof RunicGear gear && gear.runicEnabled(stack) ? gear.intrinsicRunicCharge(stack)+hardening(stack) : 0; }
}
