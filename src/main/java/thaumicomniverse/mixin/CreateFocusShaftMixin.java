package thaumicomniverse.mixin;
import com.simibubi.create.content.kinetics.base.*;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=DirectionalShaftHalvesBlockEntity.class,remap=false)
public abstract class CreateFocusShaftMixin {
    @Inject(method="getSourceFacing",at=@At("HEAD"),cancellable=true)
    private void omniverse$face(CallbackInfoReturnable<Direction> cir){
        var face=thaumicomniverse.compat.create.PowerFocusSessions.inputFace((KineticBlockEntity)(Object)this);
        if(face!=null)cir.setReturnValue(face);
    }
}
