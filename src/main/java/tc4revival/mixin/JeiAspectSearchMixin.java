package tc4revival.mixin;

import java.util.stream.Stream;
import mezz.jei.api.ingredients.ITypedIngredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tc4revival.client.search.AspectSearchQuery;
import tc4revival.client.search.ClientAspectSearch;
import tc4revival.compat.jei.JeiAspectIndex;

@Pseudo
@Mixin(targets = "mezz.jei.gui.ingredients.IngredientFilter", remap = false)
public abstract class JeiAspectSearchMixin {
    @Shadow protected abstract Stream<ITypedIngredient<?>> getIngredientListUncached(String text);
    @Shadow public abstract void invalidateCache();
    @Shadow protected abstract void notifyListenersOfChange();
    @Unique private JeiAspectIndex thaumcraft$index;
    @Unique private String thaumcraft$nativeText;
    @Unique private long thaumcraft$epoch;
    @Unique private boolean thaumcraft$apiRequest;
    @Unique private int thaumcraft$ticks;

    // API callers expect a complete list immediately. The interactive overlay can fill progressively.
    @Inject(method = "getFilteredIngredients", at = @At("HEAD"))
    private void thaumcraft$beginApiRequest(CallbackInfoReturnable<?> ci) {
        thaumcraft$apiRequest = true;
        if (thaumcraft$index != null && !thaumcraft$index.complete()) invalidateCache();
    }

    @Inject(method = "getFilteredIngredients", at = @At("RETURN"))
    private void thaumcraft$endApiRequest(CallbackInfoReturnable<?> ci) {
        thaumcraft$apiRequest = false;
    }

    @Inject(method = "getIngredientListUncached", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$search(String text, CallbackInfoReturnable<Stream<ITypedIngredient<?>>> ci) {
        if (!AspectSearchQuery.hasAspectTerm(text)) {
            tc4revival.compat.jei.JeiAspectRefresh.clear();
            return;
        }
        long epoch = ClientAspectSearch.epoch();
        if (epoch != thaumcraft$epoch) thaumcraft$index = null;
        thaumcraft$epoch = epoch;
        var query = ClientAspectSearch.parseSearch(text);
        if (query == AspectSearchQuery.INVALID) {
            ci.setReturnValue(Stream.empty());
        } else {
            if (thaumcraft$index == null || !query.nativeText().equals(thaumcraft$nativeText)) {
                thaumcraft$nativeText = query.nativeText();
                thaumcraft$index = new JeiAspectIndex(getIngredientListUncached(thaumcraft$nativeText), ClientAspectSearch::values);
                thaumcraft$ticks = 0;
                thaumcraft$index.advance(2_000_000);
            }
            if (thaumcraft$apiRequest) thaumcraft$index.advance(Long.MAX_VALUE);
            ci.setReturnValue(thaumcraft$index.filter(query.aspects()));
        }
        tc4revival.compat.jei.JeiAspectRefresh.watch(this::thaumcraft$tick);
    }

    @Unique private void thaumcraft$tick() {
        long epoch = ClientAspectSearch.epoch();
        if (epoch != thaumcraft$epoch) {
            thaumcraft$epoch = epoch;
            thaumcraft$index = null;
            invalidateCache();
            notifyListenersOfChange();
        } else if (thaumcraft$index != null && !thaumcraft$index.complete()) {
            thaumcraft$index.advance(2_000_000);
            if (thaumcraft$index.complete() || ++thaumcraft$ticks % 4 == 0) {
                invalidateCache();
                notifyListenersOfChange();
            }
        }
    }

    // JEI 15.x varies in how it reports bulk changes. Ordinary filter-text changes keep the index.
    @Inject(method = {"rebuildItemFilter", "addIngredient", "updateHidden", "onEditModeChanged",
            "onIngredientVisibilityChanged", "onIngredientsVisibilityChanged", "onIngredientsAdded",
            "onIngredientsRemoved", "onIngredientsUpdated", "markSortIndexesDirty", "refreshSearch"},
            at = @At("HEAD"), require = 0)
    private void thaumcraft$ingredientsChanged(CallbackInfo ci) {
        if (thaumcraft$index != null) {
            thaumcraft$index = null;
            invalidateCache();
        }
    }
}
