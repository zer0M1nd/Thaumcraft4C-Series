package thaumicomniverse.mixin;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=BlazeBurnerBlockEntity.class,remap=false)
public class CreateBurnerAnimationMixin {
    @Inject(method="shouldTickAnimation",at=@At("HEAD"),cancellable=true)
    private void omniverse$animate(CallbackInfoReturnable<Boolean> result){
        // This custom type uses the vanilla renderer even when Flywheel is active.
        if((Object)this instanceof thaumicomniverse.compat.create.ArcaneBurner.Tile)result.setReturnValue(true);
    }
}
