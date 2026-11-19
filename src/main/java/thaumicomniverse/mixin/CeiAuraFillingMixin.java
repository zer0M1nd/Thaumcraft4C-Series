package thaumicomniverse.mixin;

import com.simibubi.create.content.fluids.spout.FillingBySpout;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.compat.create.AuraFilling;

@Mixin(value=FillingBySpout.class,remap=false)
public abstract class CeiAuraFillingMixin {
    @Inject(method="canItemBeFilled",at=@At("HEAD"),cancellable=true)
    private static void omniverse$can(Level level,ItemStack stack,CallbackInfoReturnable<Boolean> cir){if(AuraFilling.accepts(stack))cir.setReturnValue(true);}
    @Inject(method="getRequiredAmountForItem",at=@At("HEAD"),cancellable=true)
    private static void omniverse$amount(Level level,ItemStack stack,FluidStack fluid,CallbackInfoReturnable<Integer> cir){if(AuraFilling.liquid(fluid)&&AuraFilling.accepts(stack))cir.setReturnValue(AuraFilling.amount(stack,fluid));}
    @Inject(method="fillItem",at=@At("HEAD"),cancellable=true)
    private static void omniverse$fill(Level level,int amount,ItemStack stack,FluidStack fluid,CallbackInfoReturnable<ItemStack> cir){if(AuraFilling.liquid(fluid)&&AuraFilling.accepts(stack))cir.setReturnValue(AuraFilling.process(stack,fluid,amount));}
}
