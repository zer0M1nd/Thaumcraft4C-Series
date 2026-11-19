package thaumicomniverse.mixin;

import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tc4revival.content.TCFluids;

/** JEI otherwise rejects finite death water because its default state is not a source. */
@Mixin(targets="mezz.jei.library.plugins.vanilla.ingredients.fluid.FluidIngredientHelper",remap=false)
public abstract class JeiFiniteFluidMixin {
    // JEI 15.20 inherits the permissive interface default and has no override to
    // inject into. Later JEI versions reject non-source fluids here; only those
    // versions need this exception for Thaumcraft's finite death water.
    @Inject(method="isValidIngredient(Ljava/lang/Object;)Z",at=@At("HEAD"),cancellable=true,remap=false,require=0)
    private void omniverse$deathWaterIngredient(Object ingredient,CallbackInfoReturnable<Boolean> ci) {
        if(ingredient instanceof FluidStack stack&&!stack.isEmpty()&&stack.getFluid()==TCFluids.DEATH.get())
            ci.setReturnValue(true);
    }
}
