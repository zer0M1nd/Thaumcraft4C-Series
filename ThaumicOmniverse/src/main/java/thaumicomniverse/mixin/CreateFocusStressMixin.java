package thaumicomniverse.mixin;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=KineticNetwork.class,remap=false)
public abstract class CreateFocusStressMixin {
    @Inject(method="getActualCapacityOf",at=@At("RETURN"),cancellable=true)
    private void omniverse$capacity(KineticBlockEntity tile,CallbackInfoReturnable<Float> cir){
        cir.setReturnValue(thaumicomniverse.compat.create.PowerFocusSessions.capacity(tile,cir.getReturnValue()));
    }
}
