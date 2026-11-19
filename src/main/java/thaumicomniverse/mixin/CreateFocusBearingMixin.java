package thaumicomniverse.mixin;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=MechanicalBearingBlockEntity.class,remap=false)
public abstract class CreateFocusBearingMixin {
    @Inject(method="getAngularSpeed",at=@At("HEAD"),cancellable=true)
    private void omniverse$angle(CallbackInfoReturnable<Float> cir){
        var delta=thaumicomniverse.compat.create.NativeRelocation.bearing(((MechanicalBearingBlockEntity)(Object)this).getMovedContraption());
        if(delta!=null)cir.setReturnValue(delta);
    }
}
