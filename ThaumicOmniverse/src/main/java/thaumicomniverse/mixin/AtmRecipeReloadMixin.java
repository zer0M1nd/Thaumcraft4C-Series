package thaumicomniverse.mixin;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ReloadableServerResources.class, remap = false)
public abstract class AtmRecipeReloadMixin {
    @Inject(method = {"updateRegistryTags(Lnet/minecraft/core/RegistryAccess;)V", "m_206868_(Lnet/minecraft/core/RegistryAccess;)V"}, at = @At("RETURN"), remap = false)
    private void omniverse$afterAllReloadListeners(RegistryAccess registries, CallbackInfo ci) {
        thaumicomniverse.FeatureRecipeLifecycle.apply(((ReloadableServerResources) (Object) this).getRecipeManager(), registries);
    }
}
