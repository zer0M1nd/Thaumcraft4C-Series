package thaumicomniverse.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.library.tools.nbt.*;

@Mixin(value=ToolStack.class,remap=false)
public abstract class TinkersArcaneSlotsMixin {
    @Inject(method="isUnbreakable",at=@At("RETURN"),cancellable=true,remap=false)
    private void omniverse$unbreakable(CallbackInfoReturnable<Boolean> ci) {
        if(thaumicomniverse.compat.tinkers.ArcaneMaterials.enabled()
                && ((ToolStack)(Object)this).getModifierLevel(thaumicomniverse.compat.tinkers.ArcaneMaterials.DIVINE)>0)ci.setReturnValue(true);
    }
    @Inject(method="getVolatileData",at=@At("RETURN"),cancellable=true,remap=false)
    private void omniverse$slots(CallbackInfoReturnable<IModDataView> ci) {
        // Futures also returns from this method. Compose at its adapter when installed, independent of injector ordering.
        if(net.minecraftforge.fml.ModList.get().isLoaded("thaumicfutures"))return;
        ci.setReturnValue(thaumicomniverse.compat.tinkers.ArcaneMaterials.currentSlots((ToolStack)(Object)this,ci.getReturnValue()));
    }
}
