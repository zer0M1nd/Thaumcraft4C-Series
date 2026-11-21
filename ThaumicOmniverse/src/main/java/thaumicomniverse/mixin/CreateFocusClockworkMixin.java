package thaumicomniverse.mixin;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.ClockworkBearingBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=ClockworkBearingBlockEntity.class,remap=false)
public abstract class CreateFocusClockworkMixin {
    @Shadow protected ControlledContraptionEntity hourHand;
    @Shadow protected ControlledContraptionEntity minuteHand;
    @Inject(method="getHourArmSpeed",at=@At("HEAD"),cancellable=true)
    private void omniverse$hour(CallbackInfoReturnable<Float> cir){var d=thaumicomniverse.compat.create.NativeRelocation.bearing(hourHand);if(d!=null)cir.setReturnValue(d);}
    @Inject(method="getMinuteArmSpeed",at=@At("HEAD"),cancellable=true)
    private void omniverse$minute(CallbackInfoReturnable<Float> cir){var d=thaumicomniverse.compat.create.NativeRelocation.bearing(minuteHand);if(d!=null)cir.setReturnValue(d);}
}
