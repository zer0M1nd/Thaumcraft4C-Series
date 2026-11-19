package thaumicfutures.mixin;

import com.google.gson.JsonObject;
import com.reborn.modularmachinery.recipe.MachineRequirement;
import com.reborn.modularmachinery.recipe.ModRecipeSerializers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicfutures.compat.machinery.AspectRequirement;

@Mixin(value = ModRecipeSerializers.class, remap = false)
public abstract class MachinerySerializerMixin {
    @Inject(method = "parseRequirement", at = @At("HEAD"), cancellable = true)
    private static void omniverse$json(JsonObject json, ResourceLocation recipe, CallbackInfoReturnable<MachineRequirement> callback) {
        if (json.has("type") && json.get("type").isJsonPrimitive() && AspectRequirement.customKind(json.get("type").getAsString()))
            callback.setReturnValue(AspectRequirement.parse(json));
    }
    @Inject(method = "writeRequirement", at = @At("HEAD"), cancellable = true)
    private static void omniverse$write(FriendlyByteBuf buffer, MachineRequirement requirement, CallbackInfo callback) {
        if (requirement instanceof AspectRequirement aspect) { aspect.write(buffer); callback.cancel(); }
    }
    @Inject(method = "readRequirement", at = @At("HEAD"), cancellable = true)
    private static void omniverse$read(FriendlyByteBuf buffer, CallbackInfoReturnable<MachineRequirement> callback) {
        int start = buffer.readerIndex();
        String kind = buffer.readUtf();
        if (AspectRequirement.customKind(kind)) callback.setReturnValue(AspectRequirement.read(kind, buffer));
        else buffer.readerIndex(start);
    }
}
