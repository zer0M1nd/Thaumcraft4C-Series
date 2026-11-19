package thaumicomniverse.mixin;

import com.simibubi.create.content.contraptions.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.compat.create.ContraptionRenderHandoff;

@Mixin(value=AbstractContraptionEntity.class,remap=false)
public abstract class CreateRenderHandoffMixin {
    @Inject(method="handleDisassemblyPacket",at=@At("HEAD"))
    private static void omniverse$capture(ContraptionDisassemblyPacket packet,CallbackInfo ci){ContraptionRenderHandoff.capture(packet);}
    @Inject(method={"tick","m_8119_"},at=@At("HEAD"),cancellable=true,remap=false)
    private void omniverse$freeze(CallbackInfo ci){if(ContraptionRenderHandoff.frozen((AbstractContraptionEntity)(Object)this))ci.cancel();}
    @Inject(method="collisionEnabled",at=@At("HEAD"),cancellable=true)
    private void omniverse$visualOnly(CallbackInfoReturnable<Boolean> ci){if(ContraptionRenderHandoff.frozen((AbstractContraptionEntity)(Object)this))ci.setReturnValue(false);}
}
