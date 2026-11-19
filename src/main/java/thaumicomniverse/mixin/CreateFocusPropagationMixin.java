package thaumicomniverse.mixin;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=RotationPropagator.class,remap=false)
public abstract class CreateFocusPropagationMixin {
    @Inject(method="getAxisModifier",at=@At("HEAD"),cancellable=true)
    private static void omniverse$face(KineticBlockEntity tile,Direction face,CallbackInfoReturnable<Float> cir){
        var modifier=thaumicomniverse.compat.create.PowerFocusSessions.axisModifier(tile,face);
        if(modifier!=null)cir.setReturnValue(modifier);
    }
}
