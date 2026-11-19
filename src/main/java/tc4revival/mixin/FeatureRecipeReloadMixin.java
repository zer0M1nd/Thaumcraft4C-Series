package tc4revival.mixin;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs after addon recipe reload handlers, before recipe synchronization. */
@Mixin(value=ReloadableServerResources.class, remap=false, priority=900)
public abstract class FeatureRecipeReloadMixin {
    @Inject(method={"updateRegistryTags(Lnet/minecraft/core/RegistryAccess;)V", "m_206868_(Lnet/minecraft/core/RegistryAccess;)V"}, at=@At("RETURN"), remap=false)
    private void thaumcraft$featureRecipes(RegistryAccess registries, CallbackInfo ci) {
        thaumcraft.api.config.FeatureRecipes.reloaded(((ReloadableServerResources)(Object)this).getRecipeManager());
    }
}
