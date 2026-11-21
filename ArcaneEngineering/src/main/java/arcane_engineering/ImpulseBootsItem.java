package arcane_engineering;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.List;

public class ImpulseBootsItem extends ArmorItem {
    public static final int TANK=20000;
    public static final ArmorMaterial MATERIAL=new ArmorMaterial() {
        @Override public int getDurabilityForType(Type type) { return 13*(type==Type.BOOTS?13:15); }
        @Override public int getDefenseForType(Type type) { return type==Type.BOOTS?2:0; }
        @Override public int getEnchantmentValue() { return 5; }
        @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_IRON; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
        @Override public String getName() { return "arcane_engineering:mech"; }
        @Override public float getToughness() { return 0; }
        @Override public float getKnockbackResistance() { return 0; }
    };
    public ImpulseBootsItem() { super(MATERIAL,Type.BOOTS,new Properties()); }
    @Override public String getArmorTexture(ItemStack stack,Entity wearer,EquipmentSlot slot,String type) { return "arcane_engineering:textures/models/rocketboots.png"; }
    public static boolean biodiesel(FluidStack fluid) { return !fluid.isEmpty() && ResourceLocation.parse("immersiveengineering:biodiesel").equals(ForgeRegistries.FLUIDS.getKey(fluid.getFluid())); }
    @Override public ICapabilityProvider initCapabilities(ItemStack stack,CompoundTag nbt) {
        var fluid=new FluidHandlerItemStack(stack,TANK) {
            @Override public boolean canFillFluidType(FluidStack resource) { return biodiesel(resource); }
        };
        return fluid;
    }
    public static int fuel(ItemStack stack) { return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler->handler.getFluidInTank(0).getAmount()).orElse(0); }
    public static boolean consume(ItemStack stack) { return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler -> handler.drain(1,IFluidHandler.FluidAction.EXECUTE).getAmount()==1).orElse(false); }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags) {
        lines.add(Component.translatable("tooltip.arcane_engineering.fuel",fuel(stack),TANK));
        lines.add(Component.translatable("tooltip.arcane_engineering.boots_controls"));
    }
}
