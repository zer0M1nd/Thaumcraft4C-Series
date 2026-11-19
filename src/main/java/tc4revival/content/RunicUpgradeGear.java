package tc4revival.content;

/** Intrinsic effects of the original runic accessories; hardening does not add upgrade effects. */
public interface RunicUpgradeGear extends RunicGear {
    enum Upgrade { NONE, CHARGED, KINETIC, HEALING, EMERGENCY }
    Upgrade runicUpgrade(net.minecraft.world.item.ItemStack stack);
}
