package thaumicomniverse.mixin;

import com.simibubi.create.foundation.render.SuperByteBuffer;
import net.minecraft.world.level.Level;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.compat.create.ContraptionHandoffLight;

@Mixin(value=SuperByteBuffer.class, remap=false)
public abstract class CreateHandoffLightMixin {
    @Inject(method="getLight(Lnet/minecraft/world/level/Level;Lorg/joml/Vector4f;)I", at=@At("RETURN"), cancellable=true)
    private static void omniverse$light(Level level, Vector4f position, CallbackInfoReturnable<Integer> ci) {
        ci.setReturnValue(ContraptionHandoffLight.sample(position, ci.getReturnValue()));
    }
}
