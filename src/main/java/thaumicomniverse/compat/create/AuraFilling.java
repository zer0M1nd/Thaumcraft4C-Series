package thaumicomniverse.compat.create;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.fluids.FluidStack;
import tc4revival.aspect.TCAspects;
import tc4revival.content.CastingWandItem;
import tc4revival.content.TCEnchantments;

public class AuraFilling {
    public static boolean liquid(FluidStack fluid){return fluid.getFluid().isSame(AuraContent.SOURCE.get());}
    public static boolean repairable(ItemStack stack){return stack.isDamageableItem() && stack.isDamaged() && EnchantmentHelper.getItemEnchantmentLevel(TCEnchantments.REPAIR.get(),stack)>0;}
    public static boolean chargeable(ItemStack stack){return stack.getItem() instanceof CastingWandItem wand && wand.rechargeable(stack) && missing(stack)>0;}
    public static long missing(ItemStack stack){
        if(!(stack.getItem() instanceof CastingWandItem wand))return 0;
        long sum=0;for(var aspect:TCAspects.catalog().primals())sum+=Math.max(0,wand.capacity(stack)-(long)wand.vis(stack,aspect));return sum;
    }
    public static boolean accepts(ItemStack stack){return thaumicomniverse.RevivalFeatures.Group.CREATE.enabled() && (chargeable(stack)||repairable(stack));}
    public static int amount(ItemStack stack,FluidStack fluid){
        if(!liquid(fluid)||!accepts(stack))return -1;
        long needed=chargeable(stack)?(missing(stack)+99)/100:(stack.getDamageValue()+1L)/2;
        return (int)Math.min(1000,Math.min(fluid.getAmount(),needed));
    }
    /** One mB restores one total vis or two durability. The last fractional vis may waste capacity. */
    public static ItemStack process(ItemStack stack,FluidStack fluid,int requested){
        int available=amount(stack,fluid);if(requested<=0||available<=0)return ItemStack.EMPTY;
        int amount=Math.min(requested,available);var result=stack.copyWithCount(1);
        if(result.getItem() instanceof CastingWandItem wand){
            int budget=amount*100,accepted=0;
            for(var aspect:TCAspects.catalog().primals()){
                int take=wand.receiveVis(result,aspect,budget,false);budget-=take;accepted+=take;if(budget==0)break;
            }
            if(accepted==0)return ItemStack.EMPTY;fluid.shrink((accepted+99)/100);
        }else{result.setDamageValue(Math.max(0,result.getDamageValue()-amount*2));fluid.shrink(amount);}
        stack.shrink(1);return result;
    }
}
