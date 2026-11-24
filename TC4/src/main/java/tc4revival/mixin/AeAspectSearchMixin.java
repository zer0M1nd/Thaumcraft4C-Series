package tc4revival.mixin;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import appeng.api.stacks.AEItemKey;
import appeng.menu.me.common.GridInventoryEntry;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tc4revival.client.search.AspectExpression;
import tc4revival.client.search.AspectSearchQuery;
import tc4revival.client.search.ClientAspectSearch;

@Pseudo
@Mixin(targets = "appeng.client.gui.me.search.RepoSearch", remap = false)
public abstract class AeAspectSearchMixin {
    @Shadow private String searchString;
    @Shadow private Predicate<GridInventoryEntry> search;
    @Shadow private Predicate<GridInventoryEntry> fromString(String text) { throw new AssertionError(); }
    @Shadow @Final private Long2BooleanMap cache;
    @Unique private final Map<AEItemKey, AspectExpression.Values> thaumcraft$profiles = new WeakHashMap<>();
    @Unique private long thaumcraft$epoch;

    @Inject(method = "setSearchString", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$search(String text, CallbackInfo ci) {
        if (!AspectSearchQuery.hasAspectTerm(text)) return;
        long epoch = ClientAspectSearch.epoch();
        if (epoch != thaumcraft$epoch || !text.equals(searchString)) {
            if (epoch != thaumcraft$epoch) thaumcraft$profiles.clear();
            thaumcraft$epoch = epoch;
            var query = ClientAspectSearch.parseSearch(text);
            searchString = text;
            Predicate<GridInventoryEntry> byAspect = entry -> {
                if (query == AspectSearchQuery.INVALID || !(entry.getWhat() instanceof AEItemKey item)) return false;
                var values = thaumcraft$profiles.computeIfAbsent(item, key -> ClientAspectSearch.values(key.toStack()));
                return values != null && query.aspects().test(values);
            };
            search = query.nativeText().isEmpty() ? byAspect : fromString(query.nativeText()).and(byAspect);
            cache.clear();
        }
        ci.cancel();
    }
}
