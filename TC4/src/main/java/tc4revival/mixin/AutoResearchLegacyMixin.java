package tc4revival.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo @Mixin(targets="com.github.tc4auto.service.LegacyNote",remap=false)
public abstract class AutoResearchLegacyMixin {
    @Inject(method="isLegacyKey",at=@At("HEAD"),cancellable=true)
    private static void tc4$preserveDiscovery(String key,CallbackInfoReturnable<Boolean> cir) {
        // Uppercase TC4 research keys are native here; completed items must not be deleted.
        cir.setReturnValue(false);
    }
}
