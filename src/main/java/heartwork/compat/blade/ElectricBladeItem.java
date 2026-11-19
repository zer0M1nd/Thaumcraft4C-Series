package heartwork.compat.blade;

import ic2.api.item.IElectricItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Preserves the final FIX's durability-derived charge, rather than inventing EU damage costs. */
public class ElectricBladeItem extends HeartworkBladeItem implements IElectricItem {
    public ElectricBladeItem(){super("eu");}
    public boolean canProvideEnergy(ItemStack stack){return false;}
    public double getMaxCharge(ItemStack stack){return 2_000_000;}
    public int getTier(ItemStack stack){return 3;}
    public double getTransferLimit(ItemStack stack){return 2048;}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected){
        super.inventoryTick(stack,level,holder,slot,selected);
        if(!level.isClientSide){
            double charge=getMaxCharge(stack)*(stack.getMaxDamage()-stack.getDamageValue())/Math.max(1,stack.getMaxDamage());
            stack.getOrCreateTag().putDouble("charge",charge);
            if(charge>=50_000)stack.getCapability(BLADESTATE).ifPresent(s -> s.setBroken(false));
        }
    }
}
