package thaumicomniverse.mixin;
import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=LinearActuatorBlockEntity.class,remap=false)
public abstract class CreateFocusLinearMixin {
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void omniverse$motion(CallbackInfo ci){
        if(thaumicomniverse.compat.create.NativeRelocation.linear((LinearActuatorBlockEntity)(Object)this))ci.cancel();
    }
}
