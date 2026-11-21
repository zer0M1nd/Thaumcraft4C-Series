package thaumicomniverse.mixin;
import com.simibubi.create.content.trains.entity.Train;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=Train.class,remap=false)
public abstract class CreateFocusTrainMixin {
    @Inject(method="tick",at=@At(value="INVOKE",target="Lcom/simibubi/create/content/trains/entity/Train;tickPassiveSlowdown()V",shift=At.Shift.AFTER))
    private void omniverse$train(Level level,CallbackInfo ci){thaumicomniverse.compat.create.NativeRelocation.train((Train)(Object)this);}
}
