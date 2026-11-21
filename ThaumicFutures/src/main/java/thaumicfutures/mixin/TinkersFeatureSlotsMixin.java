package thaumicfutures.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

@Mixin(value=ToolStack.class,remap=false)
public abstract class TinkersFeatureSlotsMixin {
    @Inject(method="getVolatileData",at=@At("RETURN"),cancellable=true,remap=false)
    private void omniverse$slots(CallbackInfoReturnable<IModDataView> ci) {
        ci.setReturnValue(thaumicfutures.compat.TinkersCompat.currentSlots((ToolStack)(Object)this,ci.getReturnValue()));
    }
}
