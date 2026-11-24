package tc4revival.mixin;

import appeng.client.gui.me.common.Repo;
import appeng.client.gui.me.common.PinnedKeys;
import java.util.Objects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tc4revival.client.search.AspectExpression;
import tc4revival.client.search.ClientAspectSearch;

@Pseudo
@Mixin(targets = "appeng.client.gui.me.common.Repo", remap = false)
public abstract class AeAspectRepoMixin {
    @Shadow public abstract String getSearchString();
    @Unique private String thaumcraft$lastQuery;
    @Unique private long thaumcraft$lastEpoch;
    @Unique private boolean thaumcraft$queryChanged;

    // AE2 freezes existing slots while Shift is down. Typing !, &, or | must still apply the new predicate.
    @Inject(method = "updateView", at = @At("HEAD"))
    private void thaumcraft$checkQuery(CallbackInfo ci) {
        String text = getSearchString();
        String query = AspectExpression.prefixed(text) ? text : null;
        long epoch = query == null ? 0 : ClientAspectSearch.epoch();
        thaumcraft$queryChanged = !Objects.equals(query, thaumcraft$lastQuery) || epoch != thaumcraft$lastEpoch;
        thaumcraft$lastQuery = query;
        thaumcraft$lastEpoch = epoch;
    }

    @Redirect(method = "updateView", at = @At(value = "INVOKE",
            target = "Lappeng/client/gui/me/common/Repo;isPaused()Z"))
    private boolean thaumcraft$keepSlotsWhilePaused(Repo repo) {
        return repo.isPaused() && !thaumcraft$queryChanged;
    }

    // Normally pinned keys bypass all predicates, including for synthetic entries that have no stock.
    @Redirect(method = "addEntriesToView", at = @At(value = "INVOKE",
            target = "Lappeng/client/gui/me/common/PinnedKeys;isEmpty()Z"), require = 0)
    private boolean thaumcraft$filterPinnedItems() {
        return PinnedKeys.isEmpty() || AspectExpression.prefixed(getSearchString());
    }
}
