package thaumicfutures.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thedarkcolour.exdeorum.recipe.RecipeUtil;

/** JEI may start on the recipe packet before Ex Deorum handles its cache refresh packet. */
@Pseudo
@Mixin(targets = "thedarkcolour.exdeorum.compat.jei.ExDeorumJeiPlugin", remap = false)
public abstract class ExDeorumJeiCacheMixin {
    @Inject(method = "registerRecipes", at = @At("HEAD"))
    private void futures$refreshRecipeCache(CallbackInfo callback) {
        var level = Minecraft.getInstance().level;
        // Rebuild on every JEI registration, including reconnects and data-pack reloads,
        // so a non-null cache from an earlier recipe manager cannot hide updated heat sources.
        if (level != null) RecipeUtil.reload(level.getRecipeManager());
    }
}
