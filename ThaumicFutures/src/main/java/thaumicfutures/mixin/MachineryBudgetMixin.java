package thaumicfutures.mixin;

import com.reborn.modularmachinery.machine.HatchCollection;
import com.reborn.modularmachinery.recipe.MachineRecipe;
import com.reborn.modularmachinery.recipe.RecipeModifiers;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicfutures.compat.machinery.AspectRecipeBudget;

@Mixin(value = MachineRecipe.class, remap = false)
public abstract class MachineryBudgetMixin {
    private void omniverse$check(HatchCollection ports, int phase, CallbackInfoReturnable<Boolean> callback) {
        var recipe = (MachineRecipe) (Object) this;
        if (!AspectRecipeBudget.fits(recipe, ports, phase, recipe.parallelism())) callback.setReturnValue(false);
    }
    @Inject(method = "canStart(Lcom/reborn/modularmachinery/machine/HatchCollection;Lcom/reborn/modularmachinery/recipe/RecipeModifiers;)Z", at = @At("HEAD"), cancellable = true)
    private void omniverse$canStart(HatchCollection ports, RecipeModifiers modifiers, CallbackInfoReturnable<Boolean> callback) { omniverse$check(ports, 0, callback); }
    @Inject(method = "canTick(Lcom/reborn/modularmachinery/machine/HatchCollection;Lcom/reborn/modularmachinery/recipe/RecipeModifiers;)Z", at = @At("HEAD"), cancellable = true)
    private void omniverse$canTick(HatchCollection ports, RecipeModifiers modifiers, CallbackInfoReturnable<Boolean> callback) { omniverse$check(ports, 1, callback); }
    @Inject(method = "canFinish(Lcom/reborn/modularmachinery/machine/HatchCollection;Lcom/reborn/modularmachinery/recipe/RecipeModifiers;)Z", at = @At("HEAD"), cancellable = true)
    private void omniverse$canFinish(HatchCollection ports, RecipeModifiers modifiers, CallbackInfoReturnable<Boolean> callback) { omniverse$check(ports, 2, callback); }
    @Inject(method = "start(Lcom/reborn/modularmachinery/machine/HatchCollection;Lnet/minecraft/util/RandomSource;Lcom/reborn/modularmachinery/recipe/RecipeModifiers;)Z", at = @At("HEAD"), cancellable = true)
    private void omniverse$start(HatchCollection ports, RandomSource random, RecipeModifiers modifiers, CallbackInfoReturnable<Boolean> callback) { omniverse$check(ports, 0, callback); }
    @Inject(method = "tick(Lcom/reborn/modularmachinery/machine/HatchCollection;Lnet/minecraft/util/RandomSource;Lcom/reborn/modularmachinery/recipe/RecipeModifiers;)Z", at = @At("HEAD"), cancellable = true)
    private void omniverse$tick(HatchCollection ports, RandomSource random, RecipeModifiers modifiers, CallbackInfoReturnable<Boolean> callback) { omniverse$check(ports, 1, callback); }
    @Inject(method = "finish(Lcom/reborn/modularmachinery/machine/HatchCollection;Lnet/minecraft/util/RandomSource;Lcom/reborn/modularmachinery/recipe/RecipeModifiers;)Z", at = @At("HEAD"), cancellable = true)
    private void omniverse$finish(HatchCollection ports, RandomSource random, RecipeModifiers modifiers, CallbackInfoReturnable<Boolean> callback) { omniverse$check(ports, 2, callback); }
    @Inject(method = "parallelism(Lcom/reborn/modularmachinery/machine/HatchCollection;Lcom/reborn/modularmachinery/recipe/RecipeModifiers;I)I", at = @At("RETURN"), cancellable = true)
    private void omniverse$parallel(HatchCollection ports, RecipeModifiers modifiers, int ceiling, CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(AspectRecipeBudget.limit((MachineRecipe) (Object) this, ports, callback.getReturnValue()));
    }
}
