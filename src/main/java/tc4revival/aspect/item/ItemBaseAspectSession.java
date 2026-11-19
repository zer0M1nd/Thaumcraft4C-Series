package tc4revival.aspect.item;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.BiFunction;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.recipe.RecipeAspectData;
import tc4revival.aspect.recipe.RecipeAspectData.Key;
import tc4revival.aspect.recipe.RecipeAspectEngine;

/** Server operation snapshot; rebuild after resource/tag reloads, never persist generated aspect caches. */
public  class ItemBaseAspectSession {
    private final LegacyItemIdentity identity;
    private final ItemAspectResolver direct;
    private final BiPredicate<String, String> hasTag;
    private final RecipeAspectEngine engine;
    private final RecipeAspectEngine craftingEngine;
    private final java.util.Set<String> craftingItems;
    private final Map<String,Optional<AspectAmounts>> craftingResults=new LinkedHashMap<>();
    private final Map<Key,Optional<AspectAmounts>> objects=new LinkedHashMap<>(),canonical=new LinkedHashMap<>();

    public ItemBaseAspectSession(LegacyItemIdentity identity, RecipeAspectData data, AspectCatalog catalog,
                                ItemAspectResolver direct, BiPredicate<String, String> hasTag) {
        this(identity, data, catalog, direct, hasTag, (key, amounts) -> amounts);
    }
    public ItemBaseAspectSession(LegacyItemIdentity identity, RecipeAspectData data, AspectCatalog catalog,
                                ItemAspectResolver direct, BiPredicate<String, String> hasTag,
                                BiFunction<Key, AspectAmounts, AspectAmounts> objectModifier) {
        this(identity,data,catalog,direct,hasTag,objectModifier,null);
    }
    public ItemBaseAspectSession(LegacyItemIdentity identity, RecipeAspectData data, AspectCatalog catalog,
                                ItemAspectResolver direct, BiPredicate<String,String> hasTag,
                                BiFunction<Key,AspectAmounts,AspectAmounts> objectModifier,RecipeAspectData crafting) {
        this(identity,data,catalog,direct,hasTag,objectModifier,crafting,tc4revival.aspect.recipe.InferenceLimits.server());
    }
    public ItemBaseAspectSession(LegacyItemIdentity identity, RecipeAspectData data, AspectCatalog catalog,
            ItemAspectResolver direct, BiPredicate<String,String> hasTag,
            BiFunction<Key,AspectAmounts,AspectAmounts> objectModifier,RecipeAspectData crafting,
            tc4revival.aspect.recipe.InferenceLimits limits) {
        this.identity = identity;
        this.direct = direct;
        this.hasTag = hasTag;
        var baseline = new RecipeAspectEngine(data, catalog, Map.of(), objectModifier,limits);
        Map<Key, AspectAmounts> overrides = new LinkedHashMap<>();
        Map<Key, Boolean> editedKeys = new LinkedHashMap<>();
        identity.items().forEach((id, key) -> {
            if (!direct.contains(id)) return;
            var current = direct.resolve(id, tag -> hasTag.test(id, tag)).orElseGet(AspectAmounts::new);
            var original = baseline.object(identity.scanKey(key));
            // Preserve the separate legacy wildcard recipe value when the direct rule has not changed.
            if (original != null && original.view().equals(current.view())) return;
            var canonical = identity.canonicalKey(key);
            boolean edited = direct.editorOverride(id, tag -> hasTag.test(id, tag)).isPresent();
            boolean previouslyEdited = editedKeys.getOrDefault(canonical, false);
            editedKeys.put(canonical, edited || previouslyEdited);
            var previous = overrides.put(canonical, current);
            if (previous != null && !previous.view().equals(current.view())) {
                // Several modern items can share one old metadata key. Keep the first
                // recipe fallback; exact modern editor definitions still resolve per item.
                if(edited || previouslyEdited)overrides.put(canonical,previous);
                else overrides.put(canonical,previous.copy().merge(current));
            }
        });
        engine = new RecipeAspectEngine(data, catalog, overrides, objectModifier,limits);
        craftingItems=crafting==null?java.util.Set.of():crafting.items().keySet();
        if(crafting==null)craftingEngine=null;
        else{
            var seeds=new LinkedHashMap<Key,AspectAmounts>();
            for(var id:craftingItems){
                if(direct.contains(id)){
                    seeds.put(new Key(id,0),direct.resolve(id,tag->hasTag.test(id,tag)).orElseGet(AspectAmounts::new));
                }else{
                    var legacy=identity.recipeKey(id);
                    if(legacy!=null){
                        var amount=engine.object(identity.scanKey(legacy));
                        if(amount!=null&&!amount.isEmpty())seeds.put(new Key(id,0),amount);
                    }
                }
            }
            craftingEngine=new RecipeAspectEngine(crafting,catalog,seeds,(key,value)->value,limits);
        }
    }

    public LegacyItemIdentity identity() { return identity; }
    public boolean hasCraftingItem(String id){return craftingItems.contains(id);}
    public Optional<AspectAmounts> base(String modernId) {
        return base(modernId, identity.recipeKey(modernId));
    }
    public synchronized Optional<AspectAmounts> base(String modernId, Key key) {
        // An unmatched tag on a known direct rule is authoritative; do not resurrect the old cached value.
        if (direct.contains(modernId)) return direct.resolve(modernId, tag -> hasTag.test(modernId, tag));
        if(craftingEngine!=null&&craftingItems.contains(modernId)){
            var generated=craftingResults.computeIfAbsent(modernId,id->Optional.ofNullable(craftingEngine.fork().object(new Key(id,0))));
            if(generated.isPresent())return generated.map(AspectAmounts::copy);
        }
        if(key==null)return Optional.empty();
        return objects.computeIfAbsent(identity.scanKey(key),k->Optional.ofNullable(engine.fork().object(k))).map(AspectAmounts::copy);
    }
    public synchronized Optional<AspectAmounts> canonical(String modernId) {
        var key = identity.recipeKey(modernId);
        return key == null ? Optional.empty() : canonical.computeIfAbsent(key,k->Optional.ofNullable(engine.fork().generate(k))).map(AspectAmounts::copy);
    }
}
