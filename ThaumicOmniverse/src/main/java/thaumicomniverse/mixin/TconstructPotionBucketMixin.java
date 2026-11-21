package thaumicomniverse.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.fluids.item.PotionBucketItem;

/** FluidBucketWrapper retains its identity when draining replaces its contained item. */
@Pseudo
@Mixin(targets="slimeknights.tconstruct.fluids.item.PotionBucketItem$PotionBucketWrapper",remap=false)
public abstract class TconstructPotionBucketMixin extends FluidBucketWrapper {
    protected TconstructPotionBucketMixin(ItemStack container) { super(container); }

    @Inject(method="getFluid",at=@At("HEAD"),cancellable=true,remap=false)
    private void omniverse$readReplacedBucket(CallbackInfoReturnable<FluidStack> callback) {
        if (!(container.getItem() instanceof PotionBucketItem)) callback.setReturnValue(super.getFluid());
    }
}
