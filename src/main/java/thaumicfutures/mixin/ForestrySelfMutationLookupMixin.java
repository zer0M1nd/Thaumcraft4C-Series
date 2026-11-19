package thaumicfutures.mixin;

import forestry.api.genetics.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = forestry.core.genetics.MutationManager.class, remap = false)
public abstract class ForestrySelfMutationLookupMixin<S extends ISpecies<?>> {
    @Shadow public abstract List<IMutation<S>> getAllMutations();
    @Inject(method = "getCombinations(Lforestry/api/genetics/ISpecies;Lforestry/api/genetics/ISpecies;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            at = @At("HEAD"), cancellable = true)
    private void omniverse$selfPartners(S first, S second, CallbackInfoReturnable<ObjectArrayList<IMutation<S>>> result) {
        if (first != second || !first.id().getNamespace().equals("thaumicfutures")) return;
        var matches = new ObjectArrayList<IMutation<S>>();
        for (var mutation : getAllMutations()) if (mutation.getFirstParent() == first && mutation.getSecondParent() == second
                && mutation.getResult().id().getNamespace().equals("thaumicfutures")) matches.add(mutation);
        result.setReturnValue(matches);
    }
    @Inject(method = "getMutationsFrom", at = @At("RETURN"), cancellable = true)
    private void omniverse$distinctParents(S species, CallbackInfoReturnable<List<IMutation<S>>> result) {
        if (species.id().getNamespace().equals("thaumicfutures")) result.setReturnValue(result.getReturnValue().stream().distinct().toList());
    }
}
