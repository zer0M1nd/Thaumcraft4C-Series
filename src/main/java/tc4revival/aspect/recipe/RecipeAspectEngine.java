package tc4revival.aspect.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.recipe.RecipeAspectData.Ingredient;
import tc4revival.aspect.recipe.RecipeAspectData.Key;
import tc4revival.aspect.recipe.RecipeAspectData.Kind;
import tc4revival.aspect.recipe.RecipeAspectData.Recipe;

/** One server-owned calculation session. Stack-specific potion/wand additions are separate. */
public  class RecipeAspectEngine {
    private final InferenceLimits limits;
    private final RecipeAspectData data;
    private final AspectCatalog catalog;
    private final Map<Key, AspectAmounts> inherited;
    private final Map<Key, AspectAmounts> cache = new LinkedHashMap<>();
    private Map<Key, AspectAmounts> forkSnapshot;
    private final Map<Key, Map<Kind,List<Recipe>>> recipesByOutput;
    private final Set<Key> active = new HashSet<>();
    private final BiFunction<Key, AspectAmounts, AspectAmounts> objectModifier;

    public RecipeAspectEngine(RecipeAspectData data, AspectCatalog catalog) {
        this(data, catalog, Map.of());
    }

    public RecipeAspectEngine(RecipeAspectData data, AspectCatalog catalog, Map<Key, AspectAmounts> overrides) {
        this(data, catalog, overrides, (key, value) -> value);
    }

    public RecipeAspectEngine(RecipeAspectData data, AspectCatalog catalog, Map<Key, AspectAmounts> overrides,
                              BiFunction<Key, AspectAmounts, AspectAmounts> objectModifier) {
        this(data,catalog,overrides,objectModifier,InferenceLimits.server());
    }
    public RecipeAspectEngine(RecipeAspectData data, AspectCatalog catalog, Map<Key,AspectAmounts> overrides,
            BiFunction<Key,AspectAmounts,AspectAmounts> objectModifier,InferenceLimits limits) {
        this.limits=limits;
        this.data = data;
        this.catalog = catalog;
        this.objectModifier = objectModifier;
        this.recipesByOutput=new LinkedHashMap<>();
        var seeds = new LinkedHashMap<Key, AspectAmounts>();
        for (var registration : data.registrations()) seeds.put(registration.key(), amounts(registration.aspects()));
        for (var recipe : data.recipes()) {
            amounts(recipe.cost());
            int meta=recipe.output().metadata();
            if((recipe.kind()==Kind.CRAFTING||recipe.kind()==Kind.ARCANE)&&meta==RecipeAspectData.WILDCARD)meta=0;
            var key=new Key(recipe.output().item(),meta);
            recipesByOutput.computeIfAbsent(key,k->new java.util.EnumMap<>(Kind.class))
                    .computeIfAbsent(recipe.kind(),k->new ArrayList<>()).add(recipe);
        }
        overrides.forEach((key, value) -> seeds.put(key, value.copy()));
        inherited = Collections.unmodifiableMap(seeds);
        forkSnapshot = inherited;
    }
    private RecipeAspectEngine(RecipeAspectEngine source) {
        limits=source.limits;data=source.data;catalog=source.catalog;objectModifier=source.objectModifier;recipesByOutput=source.recipesByOutput;
        // Values are never mutated in place: lookup returns a capped copy, generation stores a copy.
        inherited = source.snapshot();
        forkSnapshot = inherited;
    }
    /** Reuse validated definitions/indexes while keeping recursive traversal and derived caches isolated. */
    public RecipeAspectEngine fork() { return new RecipeAspectEngine(this); }

    private Map<Key, AspectAmounts> snapshot() {
        if (forkSnapshot == null) {
            var merged = new LinkedHashMap<>(inherited);
            merged.putAll(cache);
            forkSnapshot = Collections.unmodifiableMap(merged);
        }
        return forkSnapshot;
    }

    /** Null means no list was derived in this call; an explicitly cached empty list is distinct. */
    public AspectAmounts object(Key key) {
        var registered = lookup(key);
        return objectModifier.apply(key, registered != null ? cap(registered) : generate(key, new ArrayList<>()));
    }

    public AspectAmounts generate(Key key) { return generate(key, new ArrayList<>()); }

    private AspectAmounts lookup(Key key) {
        var found = cached(key);
        if (found == null) found = cached(new Key(key.item(), RecipeAspectData.WILDCARD));
        if (found == null && key.metadata() == RecipeAspectData.WILDCARD) {
            for (int meta = 0; meta < 16 && found == null; meta++) found = cached(new Key(key.item(), meta));
        }
        return found;
    }

    private AspectAmounts cached(Key key) {
        var value = cache.get(key);
        return value != null ? value : inherited.get(key);
    }

    private AspectAmounts generate(Key original, List<Key> history) {
        var properties = data.items().get(original.item());
        Key key = properties != null && (properties.damageable() || !properties.hasSubtypes())
                ? new Key(original.item(), RecipeAspectData.WILDCARD) : original;
        var registered = lookup(key);
        if (registered != null) return objectModifier.apply(key, cap(registered));
        if (history.contains(key) || active.contains(key) || active.size() >= 99) return null;
        history.add(key);
        if (history.size() >= 100) return null;
        active.add(key);
        try {
            var target = new Key(original.item(), key.metadata() == RecipeAspectData.WILDCARD ? 0 : original.metadata());
            AspectAmounts result = null;
            for (var kind : List.of(Kind.CRUCIBLE, Kind.ARCANE, Kind.INFUSION, Kind.CRAFTING)) {
                result = candidates(target, kind, history);
                if (result != null) break;
            }
            var limited = result == null ? null : cap(GeneratedAspectLimit.apply(result,catalog,limits));
            // The old registration API turns a failed generated result into a known empty list.
            cache.put(key, limited == null ? new AspectAmounts() : limited.copy());
            forkSnapshot = null;
            return limited;
        } finally { active.remove(key); }
    }

    private AspectAmounts candidates(Key target, Kind kind, List<Key> history) {
        AspectAmounts selected = null;
        int minimum = Integer.MAX_VALUE;
        for (var recipe : recipesByOutput.getOrDefault(target,Map.of()).getOrDefault(kind,List.of())) {
            var result = calculate(recipe, history);
            if (kind == Kind.CRUCIBLE || kind == Kind.INFUSION) return result;
            {
                int score = (int) result.total();
                if (score > 0 && score < minimum) {
                    selected = result;
                    minimum = score;
                }
            }
        }
        return selected;
    }

    private boolean matches(Recipe recipe, Key target) {
        int metadata = recipe.output().metadata();
        if ((recipe.kind() == Kind.CRAFTING || recipe.kind() == Kind.ARCANE) && metadata == RecipeAspectData.WILDCARD) metadata = 0;
        return recipe.output().item().equals(target.item()) && metadata == target.metadata();
    }

    private AspectAmounts calculate(Recipe recipe, List<Key> history) {
        if(limits.scaling())return calculateScaled(recipe,history);
        var result = new AspectAmounts();
        if (recipe.kind() == Kind.CRUCIBLE) {
            if (!recipe.ingredients().isEmpty() && !recipe.ingredients().get(0).alternatives().isEmpty()) {
                // Crucible uses the first catalyst, with a fresh traversal and without output-count division.
                var catalyst = generate(recipe.ingredients().get(0).alternatives().get(0));
                if (catalyst != null) result.add(catalyst);
            }
        } else {
            var sum = new AspectAmounts();
            for (Ingredient ingredient : recipe.ingredients()) {
                Key key = select(ingredient, history);
                if (key == null) continue;
                var properties = data.items().get(key.item());
                var value = generate(key, history);
                if (value != null) value = value.copy();
                if (properties != null && properties.container() != null) {
                    if (properties.container().equals(key.item())) continue;
                    var container = generate(new Key(properties.container(), RecipeAspectData.WILDCARD), history);
                    // Only the consumed portion contributes. Subtract per ingredient so
                    // a large remainder cannot cancel aspects from another input.
                    if (container != null && value != null)
                        for (var entry : container.view().entrySet()) value.remove(entry.getKey(), entry.getValue());
                }
                if (value != null) sum.add(value);
            }
            sum.view().forEach((aspect, amount) -> result.add(aspect, (int) (amount * 0.75f / recipe.outputCount())));
        }
        recipe.cost().forEach((tag, amount) -> catalog.find(tag).ifPresent(aspect -> result.add(aspect, (int) (Math.sqrt(amount) / recipe.outputCount()))));
        for (var aspect : new ArrayList<>(result.view().keySet())) if (result.amount(aspect) <= 0) result.remove(aspect);
        return result;
    }

    private AspectAmounts calculateScaled(Recipe recipe,List<Key> history) {
        var sum=new java.util.LinkedHashMap<tc4revival.aspect.Aspect,Double>();
        if(recipe.kind()==Kind.CRUCIBLE) {
            if(!recipe.ingredients().isEmpty()&&!recipe.ingredients().get(0).alternatives().isEmpty()) {
                var catalyst=generate(recipe.ingredients().get(0).alternatives().get(0));
                if(catalyst!=null)catalyst.view().forEach((a,n)->sum.put(a,(double)limits.expand(n)));
            }
        }else {
            for(var ingredient:recipe.ingredients()) {
                var key=select(ingredient,history);if(key==null)continue;
                var properties=data.items().get(key.item());
                if(properties!=null&&key.item().equals(properties.container()))continue;
                var values=generate(key,history);
                var container=properties==null||properties.container()==null?null:
                        generate(new Key(properties.container(),RecipeAspectData.WILDCARD),history);
                if(values!=null)values.view().forEach((a,n)->{
                    double consumed=Math.max(0,limits.expand(n)-(container==null?0:limits.expand(container.amount(a))));
                    if(consumed>0)sum.merge(a,consumed,Double::sum);
                });
            }
            sum.replaceAll((a,n)->n*.75/recipe.outputCount());
        }
        // Recipe essentia costs are raw contributions, not another already-scaled item list.
        recipe.cost().forEach((tag,n)->catalog.find(tag).ifPresent(a->sum.merge(a,Math.sqrt(n)/recipe.outputCount(),Double::sum)));
        var result=new AspectAmounts();sum.forEach((a,n)->{int amount=limits.compress(n);if(amount>0)result.add(a,amount);});return result;
    }

    private Key select(Ingredient ingredient, List<Key> history) {
        for (Key key : ingredient.alternatives()) {
            if (!ingredient.chooseFirstWithAspects()) return key;
            var value = generate(key, history);
            if (value != null && !value.isEmpty()) return key;
        }
        return null;
    }

    private AspectAmounts amounts(Map<String, Integer> values) {
        var result = new AspectAmounts();
        values.forEach((tag, amount) -> catalog.find(tag).ifPresent(aspect -> result.add(aspect, amount)));
        return result;
    }
    private AspectAmounts cap(AspectAmounts values) {
        var result = new AspectAmounts();
        values.view().forEach((aspect, amount) -> result.merge(aspect, Math.min(amount, limits.maximum())));
        return result;
    }
}
