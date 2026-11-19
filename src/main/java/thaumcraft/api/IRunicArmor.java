package thaumcraft.api;

public interface IRunicArmor extends tc4revival.content.RunicGear {
    int getRunicCharge(net.minecraft.world.item.ItemStack stack);
    @Override default int intrinsicRunicCharge(net.minecraft.world.item.ItemStack stack) { return getRunicCharge(stack); }
}
