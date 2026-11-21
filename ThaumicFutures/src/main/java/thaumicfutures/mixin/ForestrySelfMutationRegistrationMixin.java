package thaumicfutures.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Four original infused bee recipes intentionally breed two bees of the same species. */
@Mixin(value = forestry.apiimpl.plugin.MutationsRegistration.class, remap = false)
public abstract class ForestrySelfMutationRegistrationMixin {
    @Shadow @Final private ResourceLocation speciesId;
    @ModifyArg(method = {"add", "get"}, at = @At(value = "INVOKE", target = "Lcom/google/common/base/Preconditions;checkArgument(ZLjava/lang/Object;)V"), index = 0)
    private boolean omniverse$legacySelfMutation(boolean different) {
        return different || speciesId.getNamespace().equals("thaumicfutures")
                && java.util.Set.of("bee_thaumic_air", "bee_thaumic_fire", "bee_thaumic_water", "bee_thaumic_earth").contains(speciesId.getPath());
    }
}
