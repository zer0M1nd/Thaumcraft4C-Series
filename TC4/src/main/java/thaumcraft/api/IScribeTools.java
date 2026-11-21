package thaumcraft.api;

/** Ink units use item damage by default. Both methods can be overridden for NBT/fluid-backed tools. */
public interface IScribeTools {
    default int getInk(net.minecraft.world.item.ItemStack stack) { return Math.max(0,stack.getMaxDamage()-stack.getDamageValue()); }
    default void setInk(net.minecraft.world.item.ItemStack stack,int ink) { stack.setDamageValue(Math.max(0,stack.getMaxDamage()-ink)); }
}
