package thaumicfutures.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.api.IScribeTools;

/** 2,000 mB of blood, 10 mB per ink unit; standard fluid-item capability for EvilCraft machines. */
public final class VeinedScribingToolsItem extends Item implements IScribeTools {
    public static final int CAPACITY = 2000;
    private static final ResourceLocation BLOOD = new ResourceLocation("evilcraft", "blood");
    public VeinedScribingToolsItem() { super(new Properties().stacksTo(1)); }
    private static FluidHandlerItemStack tank(ItemStack stack) {
        return new FluidHandlerItemStack(stack, CAPACITY) {
            @Override public boolean canFillFluidType(FluidStack fluid) {
                return thaumicfutures.FutureFeatures.Group.EVILCRAFT.enabled() && BLOOD.equals(ForgeRegistries.FLUIDS.getKey(fluid.getFluid()));
            }
        };
    }
    private static int amount(ItemStack stack) {
        FluidStack fluid = tank(stack).getFluid();
        return BLOOD.equals(ForgeRegistries.FLUIDS.getKey(fluid.getFluid())) ? Math.min(CAPACITY, fluid.getAmount()) : 0;
    }
    @Override public int getInk(ItemStack stack) { return thaumicfutures.FutureFeatures.Group.EVILCRAFT.enabled() ? amount(stack) / 10 : 0; }
    @Override public void setInk(ItemStack stack, int ink) {
        if (!thaumicfutures.FutureFeatures.Group.EVILCRAFT.enabled()) return;
        int clamped = Math.max(0, Math.min(200, ink));
        var tank = tank(stack);
        int delta = (clamped - getInk(stack)) * 10;
        if (delta < 0) tank.drain(-delta, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        else if (delta > 0) {
            var blood = ForgeRegistries.FLUIDS.getValue(BLOOD);
            if (blood != null) tank.fill(new FluidStack(blood, delta), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        }
    }
    @Override public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) { return tank(stack); }
    @Override public boolean isBarVisible(ItemStack stack) { return amount(stack) < CAPACITY; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13f * amount(stack) / CAPACITY); }
    @Override public int getBarColor(ItemStack stack) { return 0xA20D20; }
}
