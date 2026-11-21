package thaumicomniverse.mixin;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraftforge.client.model.ExtendedBlockModelDeserializer;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** ModelManager parses even unregistered optional machines; no GT geometry exists without GT. */
@Mixin(value=ExtendedBlockModelDeserializer.class,remap=false)
public abstract class OptionalMachineModelsMixin {
    @Inject(method="deserializeGeometry",at=@At("HEAD"),cancellable=true)
    private static void omniverse$optionalMachine(JsonDeserializationContext context,JsonObject model,
                                                  CallbackInfoReturnable<IUnbakedGeometry<?>> result) {
        if(!ModList.get().isLoaded("gtceu") && model.has("loader") && model.get("loader").isJsonPrimitive()
                && model.get("loader").getAsString().equals("gtceu:machine") && model.has("machine")
                && model.get("machine").getAsString().startsWith("thaumicomniverse:"))result.setReturnValue(null);
    }
}
